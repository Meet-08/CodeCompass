package com.meet.server.feature.chat.message;

import com.meet.server.feature.chat.dto.ChatMessageResponse;
import com.meet.server.feature.chat.dto.CodeCitation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatMessageMapper {

    public Message toSpringMessage(ChatMessage message) {
        return switch (message.getRole()) {
            case USER -> new UserMessage(message.getContent());
            case ASSISTANT -> new AssistantMessage(message.getContent());
            case SYSTEM -> new SystemMessage(message.getContent());
        };
    }

    public ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        List<CodeCitation> citations = message.getCitations() == null ? List.of() : message.getCitations();
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                citations,
                message.getCreatedAt(),
                message.getUpdatedAt());
    }
}
