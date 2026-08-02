package com.meet.server.feature.codebase;

import com.meet.server.common.exception.CodebaseException;
import com.meet.server.feature.codebase.dto.CodebaseImportRequest;
import com.meet.server.feature.codebase.dto.CodebaseImportResponse;
import com.meet.server.feature.repositoryfile.RepositoryFileProcessor;
import com.meet.server.feature.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class CodebaseService {

    private final CodebaseRepository codebaseRepository;
    private final UserService userService;
    private final GitService gitService;
    private final RepositoryFileProcessor fileProcessor;
    private final CodebaseStatusService statusService;
    private final Executor codebaseTaskExecutor;

    public CodebaseService(
            CodebaseRepository codebaseRepository,
            UserService userService,
            GitService gitService,
            RepositoryFileProcessor fileProcessor,
            CodebaseStatusService statusService,
            @Qualifier("codebaseTaskExecutor") Executor codebaseTaskExecutor
    ) {
        this.codebaseRepository = codebaseRepository;
        this.userService = userService;
        this.gitService = gitService;
        this.fileProcessor = fileProcessor;
        this.statusService = statusService;
        this.codebaseTaskExecutor = codebaseTaskExecutor;
    }

    @Transactional
    public CodebaseImportResponse startClone(UUID userId, CodebaseImportRequest request) {
        validateCloneUrl(request.cloneUrl());
        var codebase = codebaseRepository.save(Codebase.builder()
                .user(userService.getById(userId))
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

        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (isInternalAddress(address)) {
                    throw invalidCloneUrl(null);
                }
            }
        } catch (IOException exception) {
            throw invalidCloneUrl(exception);
        }
    }

    private boolean isInternalAddress(InetAddress address) {
        String hostAddress = address.getHostAddress().toLowerCase(Locale.ROOT);
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || hostAddress.startsWith("fc")
                || hostAddress.startsWith("fd")) {
            return true;
        }
        if (address instanceof Inet4Address) {
            byte[] bytes = address.getAddress();
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            return first == 100 && second >= 64 && second <= 127
                    || first == 192 && second == 0
                    || first == 198 && (second == 18 || second == 19)
                    || first >= 240;
        }
        return false;
    }

    private CodebaseException invalidCloneUrl(Throwable cause) {
        return cause == null
                ? new CodebaseException("INVALID_CLONE_URL", "Clone URL must be a public HTTPS URL", HttpStatus.BAD_REQUEST)
                : new CodebaseException("INVALID_CLONE_URL", "Clone URL must be a public HTTPS URL", HttpStatus.BAD_REQUEST, cause);
    }

    public CompletableFuture<CodebaseImportResponse> processAsync(UUID codebaseId) {
        return CompletableFuture.supplyAsync(() -> process(codebaseId), codebaseTaskExecutor);
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
                gitService.cloneRepository(codebase.getCloneUrl(), codebase.getBranch(), repositoryPath);
                var commitSha = gitService.currentCommitSha(repositoryPath);
                codebaseRepository.updateLastCommitSha(codebaseId, commitSha);
                var files = gitService.listFiles(repositoryPath);
                var fileCount = fileProcessor.process(codebase, repositoryPath, files, commitSha);
                statusService.update(codebaseId, CodebaseStatus.INDEXED);
                return new CodebaseImportResponse(codebaseId, CodebaseStatus.INDEXED, fileCount);
            } finally {
                gitService.deleteRepository(repositoryPath);
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
}
