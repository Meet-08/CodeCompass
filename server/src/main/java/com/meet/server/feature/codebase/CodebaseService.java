package com.meet.server.feature.codebase;

import com.meet.server.common.exception.CodebaseException;
import com.meet.server.feature.advisor.CodeAdvisor;
import com.meet.server.feature.codebase.dto.CodeChatRequest;
import com.meet.server.feature.codebase.dto.CodeCitation;
import com.meet.server.feature.codebase.dto.CodebaseImportRequest;
import com.meet.server.feature.codebase.dto.CodebaseImportResponse;
import com.meet.server.feature.repositoryfile.RepositoryFileProcessor;
import com.meet.server.feature.user.UserService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CodebaseService {

    private final CodebaseRepository codebaseRepository;
    private final UserService userService;
    private final GitService gitService;
    private final RepositoryFileProcessor fileProcessor;
    private final CodebaseStatusService statusService;
    private final Executor codebaseTaskExecutor;
    private final ChatClient chatClient;
    private final JsonMapper jsonMapper;

    public CodebaseService(
            CodebaseRepository codebaseRepository,
            UserService userService,
            GitService gitService,
            RepositoryFileProcessor fileProcessor,
            CodebaseStatusService statusService,
            @Qualifier("codebaseTaskExecutor") Executor codebaseTaskExecutor,
            ChatClient.Builder chatClientBuilder,
            CodeAdvisor codeAdvisor,
            JsonMapper jsonMapper
    ) {
        this.codebaseRepository = codebaseRepository;
        this.userService = userService;
        this.gitService = gitService;
        this.fileProcessor = fileProcessor;
        this.statusService = statusService;
        this.codebaseTaskExecutor = codebaseTaskExecutor;
        this.jsonMapper = jsonMapper;
        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        var memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        this.chatClient = chatClientBuilder.defaultAdvisors(memoryAdvisor, codeAdvisor).build();
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

    public Flux<ServerSentEvent<Object>> streamChat(UUID userId, UUID codebaseId, CodeChatRequest request) {
        var codebase = codebaseRepository.findById(codebaseId).orElseThrow(() ->
                new CodebaseException("CODEBASE_NOT_FOUND", "Codebase not found", HttpStatus.NOT_FOUND));
        if (codebase.getUser() == null || !userId.equals(codebase.getUser().getId())) {
            throw new CodebaseException("CODEBASE_FORBIDDEN", "You do not own this codebase", HttpStatus.FORBIDDEN);
        }
        String chatId = request.chatId() == null || request.chatId().isBlank() ? "default" : request.chatId().trim();
        String conversationId = userId + ":" + codebaseId + ":" + chatId;
        var citations = new AtomicReference<List<CodeCitation>>(List.of());

        return chatClient.prompt().user(request.message().trim())
                .advisors(advisors -> advisors
                        .param(ChatMemory.CONVERSATION_ID, conversationId)
                        .param(CodeAdvisor.CODEBASE_ID_CONTEXT, codebaseId))
                .stream().chatClientResponse()
                .map(response -> {
                    var responseCitations = response.context().get(CodeAdvisor.CITATIONS_CONTEXT);
                    if (responseCitations instanceof List<?> values) {
                        citations.set(values.stream()
                                .filter(CodeCitation.class::isInstance)
                                .map(CodeCitation.class::cast)
                                .toList());
                    }
                    var text = Objects.requireNonNull(Objects.requireNonNull(response.chatResponse()).getResult())
                            .getOutput().getText();
                    return sse("message", text);
                })
                .concatWith(Flux.defer(() -> Flux.just(
                        sse("citations", citations.get()),
                        sse("done", chatId))))
                .onErrorResume(error -> Flux.just(sse("error", Map.of("message", "Unable to complete chat"))));
    }

    private ServerSentEvent<Object> sse(String event, Object data) {
        return ServerSentEvent.builder((Object) jsonMapper.writeValueAsString(data))
                .event(event)
                .build();
    }
}
