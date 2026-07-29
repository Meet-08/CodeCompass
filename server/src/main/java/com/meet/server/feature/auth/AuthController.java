package com.meet.server.feature.auth;

import com.meet.server.common.api.ApiResponse;
import com.meet.server.common.config.AppConfig;
import com.meet.server.common.util.CookieUtil;
import com.meet.server.feature.auth.dto.AuthResponse;
import com.meet.server.feature.auth.dto.LoginRequest;
import com.meet.server.feature.auth.dto.RegisterRequest;
import com.meet.server.feature.auth.dto.UserResponse;
import com.meet.server.feature.auth.mapper.AuthMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthMapper authMapper;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthResult auth = authService.register(request);
        writeRefreshCookie(response, auth.refreshToken());
        return ResponseEntity.ok(success("Registration successful", publicResponse(auth)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResult auth = authService.login(request);
        writeRefreshCookie(response, auth.refreshToken());
        return ResponseEntity.ok(success("Login successful", publicResponse(auth)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "refresh_token", required = true) String cookieRefreshToken,
            HttpServletResponse response
    ) {
        AuthResult auth = authService.refresh(cookieRefreshToken);
        writeRefreshCookie(response, auth.refreshToken());
        return ResponseEntity.ok(success("Token refreshed", publicResponse(auth)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication,
            @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
            HttpServletResponse response
    ) {
        if (cookieRefreshToken != null) {
            authService.logout(cookieRefreshToken);
        } else if (authentication != null) {
            authService.logout(UUID.fromString(authentication.getName()));
        }
        CookieUtil.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(new ApiResponse<>(true, "Logout successful", Optional.empty()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> currentUser(Authentication authentication) {
        UserResponse user = authMapper.toUserResponse(
                authService.getCurrentUser(UUID.fromString(authentication.getName())));
        return ResponseEntity.ok(success("Current user retrieved", user));
    }

    private void writeRefreshCookie(HttpServletResponse response, String refreshToken) {
        CookieUtil.addRefreshTokenCookie(response, refreshToken, AppConfig.REFRESH_TOKEN_EXPIRY_SECONDS);
    }

    private AuthResponse publicResponse(AuthResult auth) {
        return new AuthResponse(auth.accessToken(), auth.user());
    }

    private <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, Optional.of(data));
    }
}
