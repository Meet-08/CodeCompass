package com.meet.server.feature.chat.dto;

import com.meet.server.feature.chat.message.MessageRole;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID messageId,
        MessageRole role,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
}
