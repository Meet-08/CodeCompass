package com.meet.server.feature.chat;

import com.meet.server.common.exception.CodebaseException;
import com.meet.server.feature.advisor.CodeAdvisor;
import com.meet.server.feature.chat.dto.CodeChatRequest;
import com.meet.server.feature.chat.dto.CodeCitation;
import com.meet.server.feature.codebase.CodebaseRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final CodebaseRepository codebaseRepository;
    private final ChatClient chatClient;
    private final JsonMapper jsonMapper;

    public ChatService(
            CodebaseRepository codebaseRepository,
            ChatClient.Builder chatClientBuilder,
            CodeAdvisor codeAdvisor,
            JsonMapper jsonMapper
    ) {
        this.codebaseRepository = codebaseRepository;
        this.jsonMapper = jsonMapper;
        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        var memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        this.chatClient = chatClientBuilder.defaultAdvisors(memoryAdvisor, codeAdvisor).build();
    }

    @Transactional(readOnly = true)
    public Flux<ServerSentEvent<Object>> stream(UUID userId, UUID codebaseId, CodeChatRequest request) {
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
                .flatMap(response -> {
                    var responseCitations = response.context().get(CodeAdvisor.CITATIONS_CONTEXT);
                    if (responseCitations instanceof List<?> values) {
                        citations.set(values.stream()
                                .filter(CodeCitation.class::isInstance)
                                .map(CodeCitation.class::cast)
                                .toList());
                    }

                    var chatResponse = response.chatResponse();
                    if (chatResponse == null || chatResponse.getResult() == null
                            || chatResponse.getResult().getOutput() == null) {
                        return Flux.empty();
                    }

                    var text = chatResponse.getResult().getOutput().getText();
                    if (text == null || text.isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.just(sse("message", text));
                })
                .concatWith(Flux.defer(() -> Flux.just(
                        sse("citations", citations.get()),
                        sse("done", chatId))))
                .onErrorResume(error -> {
                    log.error("Chat streaming failed for userId={}, codebaseId={}, chatId={}",
                            userId, codebaseId, chatId, error);
                    return Flux.just(sse("error", Map.of("message", "Unable to complete chat")));
                });
    }

    private ServerSentEvent<Object> sse(String event, Object data) {
        return ServerSentEvent.builder((Object) jsonMapper.writeValueAsString(data))
                .event(event)
                .build();
    }
}
