package com.meet.server.feature.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatSessionUpdateRequest(
        @NotBlank(message = "title is required") String title
) {
}
