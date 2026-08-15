package com.meet.server.feature.chat;

import com.meet.server.feature.advisor.CodeAdvisor;
import com.meet.server.feature.chat.dto.CodeChatRequest;
import com.meet.server.feature.chat.dto.CodeCitation;
import com.meet.server.feature.chat.message.ChatMessageService;
import com.meet.server.feature.chat.session.ChatSession;
import com.meet.server.feature.chat.session.ChatSessionService;
import com.meet.server.feature.chat.tool.CodeLookupTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class ChatService {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final ChatTitleService chatTitleService;
    private final ChatClient chatClient;
    private final CodeLookupTools codeLookupTools;
    private final JsonMapper jsonMapper;
    private final String systemPrompt;

    public ChatService(
            ChatSessionService chatSessionService,
            ChatMessageService chatMessageService,
            ChatTitleService chatTitleService,
            ChatClient.Builder chatClientBuilder,
            CodeAdvisor codeAdvisor,
            CodeLookupTools codeLookupTools,
            JsonMapper jsonMapper
    ) {
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.chatTitleService = chatTitleService;
        this.codeLookupTools = codeLookupTools;
        this.jsonMapper = jsonMapper;
        this.chatClient = chatClientBuilder.defaultAdvisors(codeAdvisor).build();
        this.systemPrompt = loadSystemPrompt();
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
        var toolCitations = new CopyOnWriteArrayList<CodeCitation>();
        var answer = new StringBuilder();
        var promptMessages = new ArrayList<>(history);
        promptMessages.add(new UserMessage(message));

        return chatClient.prompt().system(systemPrompt).messages(promptMessages)
                .tools(codeLookupTools)
                .toolContext(Map.of(
                        CodeAdvisor.CODEBASE_ID_CONTEXT, codebaseId,
                        CodeLookupTools.CITATIONS_SINK, toolCitations
                ))
                .advisors(advisors -> advisors.param(CodeAdvisor.CODEBASE_ID_CONTEXT, codebaseId))
                .stream().chatClientResponse()
                .flatMap(response -> {
                    var responseCitations = response.context().get(CodeAdvisor.CITATIONS_CONTEXT);
                    if (responseCitations instanceof List<?> values) {
                        citations.set(mergeCitations(values.stream()
                                .filter(CodeCitation.class::isInstance)
                                .map(CodeCitation.class::cast)
                                .toList(), toolCitations));
                    } else if (!toolCitations.isEmpty()) {
                        citations.updateAndGet(current -> mergeCitations(current, toolCitations));
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
                    citations.updateAndGet(current -> mergeCitations(current, toolCitations));
                    if (!answer.isEmpty()) {
                        chatMessageService.saveAssistantMessage(session, answer.toString(), citations.get());
                    }
                })
                .concatWith(Flux.defer(() -> {
                    var events = new ArrayList<ServerSentEvent<Object>>();
                    events.add(sse("citations", mergeCitations(citations.get(), toolCitations)));
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

    private static String loadSystemPrompt() {
        var resource = new ClassPathResource("prompt.md");
        if (!resource.exists()) {
            throw new IllegalStateException("Missing classpath resource prompt.md");
        }
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load classpath resource prompt.md", exception);
        }
    }

    private static List<CodeCitation> mergeCitations(List<CodeCitation> primary, List<CodeCitation> extra) {
        var merged = new ArrayList<CodeCitation>();
        var seen = new HashSet<UUID>();
        for (var source : List.of(
                primary == null ? List.<CodeCitation>of() : primary,
                extra == null ? List.<CodeCitation>of() : extra
        )) {
            for (var citation : source) {
                if (citation != null && citation.chunkId() != null && seen.add(citation.chunkId())) {
                    merged.add(citation);
                }
            }
        }
        return List.copyOf(merged);
    }

    private ServerSentEvent<Object> sse(String event, Object data) {
        return ServerSentEvent.builder((Object) jsonMapper.writeValueAsString(data))
                .event(event)
                .build();
    }
}
