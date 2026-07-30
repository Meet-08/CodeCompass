package com.meet.server.feature.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "${app.auth.refresh-token-cleanup-cron:0 0 * * * *}")
    public void deleteRevokedOrExpiredTokens() {
        int deletedTokens = refreshTokenService.deleteRevokedOrExpiredTokens();
        log.debug("Deleted {} revoked or expired refresh tokens", deletedTokens);
    }
}
