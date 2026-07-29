package com.meet.server.feature.auth.dto;

public record AuthResponse(String accessToken, UserResponse user) {
}
