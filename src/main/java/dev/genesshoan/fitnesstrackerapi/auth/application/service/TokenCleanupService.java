package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import java.time.Clock;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.RefreshTokenRepository;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Removes authentication records that are no longer useful. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final Clock clock;

    /**
     * Keeps refresh-token families with an active member for replay detection.
     * Families without one are safe to remove in full.
     */
    @Scheduled(cron = "${AUTH_CLEANUP_CRON:0 0 3 * * *}", zone = "UTC")
    @Transactional
    public void cleanup() {
        var now = clock.instant();
        int deletedRefreshTokens = refreshTokenRepository.deleteExpiredAndInactiveFamilies(now);
        int deletedVerificationTokens = verificationTokenRepository.deleteExpiredBefore(now);

        log.info(
                "Authentication token cleanup completed: refreshTokens={}, verificationTokens={}",
                deletedRefreshTokens,
                deletedVerificationTokens);
    }
}
