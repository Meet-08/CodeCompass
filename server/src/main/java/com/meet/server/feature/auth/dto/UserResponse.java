package com.meet.server.feature.auth.dto;

import com.meet.server.feature.user.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String username,
        String email,
        String avatarUrl,
        UserRole role
) {
}
