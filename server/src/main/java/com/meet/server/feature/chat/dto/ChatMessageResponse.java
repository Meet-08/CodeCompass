package com.meet.server.feature.chat.dto;

import com.meet.server.feature.chat.message.MessageRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatMessageResponse(
        UUID messageId,
        MessageRole role,
        String content,
        List<CodeCitation> citations,
        Instant createdAt,
        Instant updatedAt
) {
}
