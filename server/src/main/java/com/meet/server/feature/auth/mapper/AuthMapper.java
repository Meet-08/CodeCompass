package com.meet.server.feature.auth.mapper;

import com.meet.server.feature.auth.dto.AuthResponse;
import com.meet.server.feature.auth.dto.UserResponse;
import com.meet.server.feature.user.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public User toUser(String fullName, String username, String email, String encodedPassword) {
        return User.builder()
                .fullName(fullName)
                .username(username)
                .email(email)
                .password(encodedPassword)
                .build();
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getUsername(), user.getEmail(),
                user.getAvatarUrl(), user.getRole());
    }

    public AuthResponse toAuthResponse(String accessToken, User user) {
        return new AuthResponse(accessToken, toUserResponse(user));
    }
}
