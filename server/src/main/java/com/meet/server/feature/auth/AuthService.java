package com.meet.server.feature.auth;

import com.meet.server.common.exception.AuthException;
import com.meet.server.common.security.jwt.JwtService;
import com.meet.server.feature.auth.dto.LoginRequest;
import com.meet.server.feature.auth.dto.RegisterRequest;
import com.meet.server.feature.auth.mapper.AuthMapper;
import com.meet.server.feature.user.User;
import com.meet.server.feature.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final AuthMapper authMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userService.existsByEmail(request.email())) {
            throw new AuthException("EMAIL_ALREADY_EXISTS", "Email is already registered", HttpStatus.CONFLICT);
        }
        if (userService.existsByUsername(request.username())) {
            throw new AuthException("USERNAME_ALREADY_EXISTS", "Username is already registered", HttpStatus.CONFLICT);
        }
        User user = userService.create(authMapper.toUser(request.fullName(), request.username(), request.email(),
                passwordEncoder.encode(request.password())));
        return issueTokens(user);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        User user = userService.getByEmail(request.email());
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthException("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED);
        }
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(UUID userId) {
        return userService.getById(userId);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        User user = refreshTokenService.getUserFromToken(rawRefreshToken);
        String newRefreshToken = refreshTokenService.rotateRefreshToken(rawRefreshToken);
        return issueAccessToken(user, newRefreshToken);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenService.revokeAllForUser(userService.getById(userId));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeAllForUser(refreshTokenService.getUserFromToken(rawRefreshToken));
    }

    @Transactional
    public AuthResult loginWithOAuth2(String providerName, String email, String fullName, String avatarUrl) {
        if (!StringUtils.hasText(email)) {
            throw new AuthException("OAUTH_EMAIL_MISSING", "OAuth provider did not return an email address",
                    HttpStatus.UNAUTHORIZED);
        }

        User user = userService.findByEmail(email)
                .map(existing -> linkOAuthProvider(existing, providerName, fullName, avatarUrl))
                .orElseGet(() -> createOAuthUser(providerName, email, fullName, avatarUrl));
        return issueTokens(user);
    }

    private User linkOAuthProvider(User user, String providerName, String fullName, String avatarUrl) {
        user.setProvider(providerName.equalsIgnoreCase("github") ? Provider.GITHUB : Provider.GOOGLE);
        if (StringUtils.hasText(fullName)) user.setFullName(fullName);
        if (StringUtils.hasText(avatarUrl)) user.setAvatarUrl(avatarUrl);
        return userService.create(user);
    }

    private User createOAuthUser(String providerName, String email, String fullName, String avatarUrl) {
        String baseUsername = email.substring(0, email.indexOf('@'))
                .replaceAll("[^A-Za-z0-9_]", "_")
                .toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(baseUsername)) baseUsername = "user";

        String username = baseUsername;
        int suffix = 1;
        while (userService.existsByUsername(username)) {
            username = baseUsername + suffix++;
        }

        return userService.create(User.builder()
                .fullName(StringUtils.hasText(fullName) ? fullName : username)
                .username(username)
                .email(email)
                .avatarUrl(avatarUrl)
                .provider(providerName.equalsIgnoreCase("github") ? Provider.GITHUB : Provider.GOOGLE)
                .build());
    }

    private AuthResult issueTokens(User user) {
        String refreshToken = refreshTokenService.createRefreshToken(user);
        return issueAccessToken(user, refreshToken);
    }

    private AuthResult issueAccessToken(User user) {
        return issueAccessToken(user, null);
    }

    private AuthResult issueAccessToken(User user, String refreshToken) {
        return new AuthResult(jwtService.generateAccessToken(user), refreshToken, authMapper.toUserResponse(user));
    }
}
