package com.meet.server.feature.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record CodeChatRequest(String chatId, @NotBlank(message = "message is required") String message) {}
