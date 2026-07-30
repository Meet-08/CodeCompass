package com.meet.server.common.security.oauth2;

import com.meet.server.common.config.AppConfig;
import com.meet.server.common.util.CookieUtil;
import com.meet.server.feature.auth.AuthResult;
import com.meet.server.feature.auth.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.oauth2.success-redirect-uri}")
    private String successRedirectUri;

    @Override
    @NullMarked
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauth.getPrincipal();
        String provider = oauth.getAuthorizedClientRegistrationId();
        String email = principal.getAttribute("email");
        String fullName = firstNonBlank(principal.getAttribute("name"), principal.getAttribute("login"));
        String avatar = firstNonBlank(principal.getAttribute("picture"), principal.getAttribute("avatar_url"));

        AuthResult auth = authService.loginWithOAuth2(provider, email, fullName, avatar);
        CookieUtil.addRefreshTokenCookie(response, auth.refreshToken(), AppConfig.REFRESH_TOKEN_EXPIRY_SECONDS);

        String target = UriComponentsBuilder.fromUriString(successRedirectUri)
                .queryParam("access_token", auth.accessToken())
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(target);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
