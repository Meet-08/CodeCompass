package com.meet.server.feature.chat.session;

import com.meet.server.feature.chat.dto.ChatSessionResponse;
import org.springframework.stereotype.Component;

@Component
public class ChatSessionMapper {

    public ChatSessionResponse toChatSessionResponse(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(),
                session.getCodebase().getId(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
