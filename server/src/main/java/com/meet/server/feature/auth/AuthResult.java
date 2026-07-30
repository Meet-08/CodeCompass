package com.meet.server.feature.auth;

import com.meet.server.feature.auth.dto.UserResponse;

public record AuthResult(String accessToken, String refreshToken, UserResponse user) {
}
