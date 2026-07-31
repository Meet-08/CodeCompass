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
import java.nio.file.Files;
import java.nio.file.Path;
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
                var files = gitService.listFiles(repositoryPath);
                var fileCount = fileProcessor.process(codebase, repositoryPath, files);
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
