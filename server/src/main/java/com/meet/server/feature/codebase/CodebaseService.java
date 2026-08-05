package com.meet.server.feature.codebase;

import com.meet.server.common.exception.CodebaseException;
import com.meet.server.feature.codebase.dto.CodebaseImportRequest;
import com.meet.server.feature.codebase.dto.CodebaseImportResponse;
import com.meet.server.feature.codebase.dto.CodebaseResponse;
import com.meet.server.feature.codebase.dto.CodebaseUpdateRequest;
import com.meet.server.feature.codebase.mapper.CodebaseMapper;
import com.meet.server.feature.codechunk.CodeChunkRepository;
import com.meet.server.feature.repositoryfile.RepositoryFileProcessor;
import com.meet.server.feature.repositoryfile.RepositoryFileRepository;
import com.meet.server.feature.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class CodebaseService {

    private static final Logger log = LoggerFactory.getLogger(CodebaseService.class);

    private final CodebaseRepository codebaseRepository;
    private final UserService userService;
    private final GitService gitService;
    private final RepositoryFileProcessor fileProcessor;
    private final CodebaseStatusService statusService;
    private final RepositoryFileRepository repositoryFileRepository;
    private final CodeChunkRepository codeChunkRepository;
    private final CodebaseMapper codebaseMapper;
    private final Executor codebaseTaskExecutor;

    public CodebaseService(
            CodebaseRepository codebaseRepository,
            UserService userService,
            GitService gitService,
            RepositoryFileProcessor fileProcessor,
            CodebaseStatusService statusService,
            RepositoryFileRepository repositoryFileRepository,
            CodeChunkRepository codeChunkRepository,
            @Qualifier("codebaseTaskExecutor") Executor codebaseTaskExecutor,
            CodebaseMapper codebaseMapper
    ) {
        this.codebaseRepository = codebaseRepository;
        this.userService = userService;
        this.gitService = gitService;
        this.fileProcessor = fileProcessor;
        this.statusService = statusService;
        this.repositoryFileRepository = repositoryFileRepository;
        this.codeChunkRepository = codeChunkRepository;
        this.codebaseTaskExecutor = codebaseTaskExecutor;
        this.codebaseMapper = codebaseMapper;
    }

    @Transactional
    public CodebaseImportResponse startClone(UUID userId, CodebaseImportRequest request) {
        validateCloneUrl(request.cloneUrl());
        var user = userService.getByIdForUpdate(userId);
        if (codebaseRepository.countByUserId(userId) >= 5) {
            throw new CodebaseException("CODEBASE_LIMIT_REACHED", "A user can have at most 5 codebases", HttpStatus.CONFLICT);
        }
        var codebase = codebaseRepository.save(Codebase.builder()
                .user(user)
                .name(request.name())
                .cloneUrl(request.cloneUrl())
                .branch(request.branch() == null || request.branch().isBlank() ? "main" : request.branch())
                .status(CodebaseStatus.QUEUED)
                .build());

        var codebaseId = codebase.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processAsync(codebaseId);
            }
        });

        return new CodebaseImportResponse(codebaseId, CodebaseStatus.QUEUED, 0);
    }

    @Transactional(readOnly = true)
    public List<CodebaseResponse> getUserCodebases(UUID userId) {
        userService.getById(userId);
        return codebaseRepository.findResponsesByUserId(userId);
    }

    @Transactional
    public CodebaseResponse updateCodebase(UUID userId, UUID codebaseId, CodebaseUpdateRequest request) {
        var codebase = ownedCodebase(userId, codebaseId);
        codebase.setName(request.name().trim());
        codebase.setBranch(request.branch().trim());
        codebaseRepository.save(codebase);
        return codebaseMapper.toCodebaseResponse(codebase, repositoryFileRepository.countByCodebaseId(codebaseId));
    }

    @Transactional
    public CodebaseImportResponse reindexCodebase(UUID userId, UUID codebaseId) {
        var codebase = ownedCodebase(userId, codebaseId);
        if (codebase.getStatus() == CodebaseStatus.QUEUED || codebase.getStatus() == CodebaseStatus.PROCESSING) {
            throw new CodebaseException("CODEBASE_BUSY", "Codebase is currently being indexed", HttpStatus.CONFLICT);
        }
        codeChunkRepository.deleteByCodebaseId(codebaseId);
        repositoryFileRepository.deleteByCodebaseId(codebaseId);
        codebase.setLastCommitSha(null);
        codebase.setIndexedAt(null);
        codebase.setStatus(CodebaseStatus.QUEUED);
        codebaseRepository.save(codebase);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processAsync(codebaseId);
            }
        });
        return new CodebaseImportResponse(codebaseId, CodebaseStatus.QUEUED, 0);
    }

    private void validateCloneUrl(String cloneUrl) {
        if (cloneUrl == null || cloneUrl.isBlank()) {
            throw invalidCloneUrl(null);
        }
        final URI uri;
        try {
            uri = new URI(cloneUrl);
        } catch (URISyntaxException exception) {
            throw invalidCloneUrl(exception);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getHost().isBlank()
                || uri.getUserInfo() != null) {
            throw invalidCloneUrl(null);
        }
    }

    private CodebaseException invalidCloneUrl(Throwable cause) {
        return cause == null
                ? new CodebaseException("INVALID_CLONE_URL", "Clone URL must be a public HTTPS URL", HttpStatus.BAD_REQUEST)
                : new CodebaseException("INVALID_CLONE_URL", "Clone URL must be a public HTTPS URL", HttpStatus.BAD_REQUEST, cause);
    }

    public CompletableFuture<CodebaseImportResponse> processAsync(UUID codebaseId) {
        return CompletableFuture.supplyAsync(() -> process(codebaseId), codebaseTaskExecutor)
                .whenComplete((response, exception) -> {
                    if (exception != null) {
                        log.error("Background codebase indexing failed for {}", codebaseId, exception);
                    }
                });
    }

    private CodebaseImportResponse process(UUID codebaseId) {
        var codebase = codebaseRepository.findById(codebaseId)
                .orElseThrow(() -> new CodebaseException(
                        "CODEBASE_NOT_FOUND",
                        "Codebase not found",
                        HttpStatus.NOT_FOUND));
        try {
            statusService.update(codebaseId, CodebaseStatus.PROCESSING);

            Path repositoryPath = Files.createTempDirectory("codebase-" + codebaseId);
            try {
                log.debug("Created temporary clone workspace at {}", repositoryPath);
                gitService.cloneRepository(codebase.getCloneUrl(), codebase.getBranch(), repositoryPath);
                var commitSha = gitService.currentCommitSha(repositoryPath);
                codebaseRepository.updateLastCommitSha(codebaseId, commitSha);
                var files = gitService.listFiles(repositoryPath);
                var fileCount = fileProcessor.process(codebase, repositoryPath, files, commitSha);
                statusService.update(codebaseId, CodebaseStatus.INDEXED);
                return new CodebaseImportResponse(codebaseId, CodebaseStatus.INDEXED, fileCount);
            } finally {
                try {
                    gitService.deleteRepository(repositoryPath);
                } catch (RuntimeException cleanupException) {
                    log.warn("Unable to clean up temporary clone workspace {}; indexing status is unchanged",
                            repositoryPath, cleanupException);
                }
            }
        } catch (IOException exception) {
            statusService.update(codebaseId, CodebaseStatus.FAILED);
            throw new CodebaseException(
                    "CODEBASE_WORKSPACE_FAILED",
                    "Unable to process codebase workspace",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception);
        } catch (RuntimeException exception) {
            statusService.update(codebaseId, CodebaseStatus.FAILED);
            throw exception;
        }
    }

    @Transactional
    public void deleteCodebase(UUID userId, UUID codebaseId) {
        var codebase = ownedCodebase(userId, codebaseId);
        if (codebase.getStatus() == CodebaseStatus.QUEUED || codebase.getStatus() == CodebaseStatus.PROCESSING) {
            throw new CodebaseException("CODEBASE_BUSY", "Codebase is currently being indexed", HttpStatus.CONFLICT);
        }
        codeChunkRepository.deleteByCodebaseId(codebaseId);
        repositoryFileRepository.deleteByCodebaseId(codebaseId);
        codebaseRepository.delete(codebase);
    }

    private Codebase ownedCodebase(UUID userId, UUID codebaseId) {
        var codebase = codebaseRepository.findById(codebaseId).orElseThrow(() ->
                new CodebaseException("CODEBASE_NOT_FOUND", "Codebase not found", HttpStatus.NOT_FOUND));
        if (codebase.getUser() == null || !userId.equals(codebase.getUser().getId())) {
            throw new CodebaseException("CODEBASE_FORBIDDEN", "You do not own this codebase", HttpStatus.FORBIDDEN);
        }
        return codebase;
    }

}
