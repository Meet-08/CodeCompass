package com.meet.server.feature.advisor;

import com.meet.server.feature.retriver.CodeRetriever;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;


@Component
@RequiredArgsConstructor
public class CodeAdvisor implements CallAdvisor, StreamAdvisor {
    public static final String CODEBASE_ID_CONTEXT = "codebaseId";
    public static final String CITATIONS_CONTEXT = "citations";
    private final CodeRetriever codeRetriever;

    @Override
    @NullMarked
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextCall(enrich(request));
    }

    @Override
    @NullMarked
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(enrich(request));
    }

    private ChatClientRequest enrich(ChatClientRequest request) {
        Object rawId = request.context().get(CODEBASE_ID_CONTEXT);
        if (rawId == null) return request;
        var lastMessage = request.prompt().getLastUserOrToolResponseMessage();
        if (!(lastMessage instanceof UserMessage)) {
            return request;
        }
        var codebaseId = rawId instanceof UUID id ? id : UUID.fromString(rawId.toString());
        String query = lastMessage.getText();
        var retrieval = codeRetriever.retrieve(codebaseId, query);
        return request.mutate().prompt(request.prompt().augmentUserMessage(retrieval.promptContext()))
                .context(CITATIONS_CONTEXT, retrieval.citations())
                .build();
    }

    @Override
    @NullMarked
    public String getName() {
        return "code-retrieval";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
