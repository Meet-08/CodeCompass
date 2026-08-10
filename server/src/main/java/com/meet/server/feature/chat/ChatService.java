package com.meet.server.feature.chat;

import com.meet.server.feature.advisor.CodeAdvisor;
import com.meet.server.feature.chat.dto.CodeChatRequest;
import com.meet.server.feature.chat.dto.CodeCitation;
import com.meet.server.feature.chat.message.ChatMessageService;
import com.meet.server.feature.chat.session.ChatSession;
import com.meet.server.feature.chat.session.ChatSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class ChatService {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final ChatTitleService chatTitleService;
    private final ChatClient chatClient;
    private final JsonMapper jsonMapper;

    public ChatService(
            ChatSessionService chatSessionService,
            ChatMessageService chatMessageService,
            ChatTitleService chatTitleService,
            ChatClient.Builder chatClientBuilder,
            CodeAdvisor codeAdvisor,
            JsonMapper jsonMapper
    ) {
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.chatTitleService = chatTitleService;
        this.jsonMapper = jsonMapper;
        this.chatClient = chatClientBuilder.defaultAdvisors(codeAdvisor).build();
    }

    @Transactional
    public Flux<ServerSentEvent<Object>> stream(UUID userId, UUID codebaseId, CodeChatRequest request) {
        String message = request.message().trim();
        ChatSession session = chatSessionService.resolve(userId, codebaseId, request.chatId());
        List<Message> history = chatMessageService.loadPromptHistory(session.getId());
        boolean firstResponse = !chatMessageService.hasAssistantResponse(session.getId());
        chatMessageService.saveUserMessage(session, message);

        String chatId = session.getId().toString();
        var citations = new AtomicReference<List<CodeCitation>>(List.of());
        var answer = new StringBuilder();
        var promptMessages = new ArrayList<>(history);
        promptMessages.add(new UserMessage(message));

        return chatClient.prompt().messages(promptMessages)
                .advisors(advisors -> advisors.param(CodeAdvisor.CODEBASE_ID_CONTEXT, codebaseId))
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
                    if (chatResponse == null || chatResponse.getResult() == null) {
                        return Flux.empty();
                    } else {
                        chatResponse.getResult();
                    }

                    var text = chatResponse.getResult().getOutput().getText();
                    if (text == null || text.isEmpty()) {
                        return Flux.empty();
                    }
                    answer.append(text);
                    return Flux.just(sse("message", text));
                })
                .doOnComplete(() -> {
                    if (!answer.isEmpty()) {
                        chatMessageService.saveAssistantMessage(session, answer.toString());
                    }
                })
                .concatWith(Flux.defer(() -> {
                    var events = new ArrayList<ServerSentEvent<Object>>();
                    events.add(sse("citations", citations.get()));
                    if (firstResponse && !answer.isEmpty()) {
                        events.add(sse("title", chatTitleService.generateAndSave(chatClient, session, message)));
                    }
                    events.add(sse("done", chatId));
                    return Flux.fromIterable(events);
                }))
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
