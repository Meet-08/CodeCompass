package com.meet.server.feature.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionResponse(
        UUID sessionId,
        UUID codebaseId,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
}
