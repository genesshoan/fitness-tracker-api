package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.auth.application.result.IssuedToken;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationPurpose;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config.VerificationProperties;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.VerificationTokenRepository;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security.OpaqueTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VerificationTokenService}.
 */
@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceTest {

    private static final UUID USER_ID = UUID.fromString("01932f4a-1234-7000-8000-123456789abc");
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Mock
    private VerificationTokenRepository repository;

    private VerificationTokenService verificationTokenService;

    @BeforeEach
    void setUp() {
        verificationTokenService = new VerificationTokenService(
                new VerificationProperties(Duration.ofHours(24), Duration.ofHours(1)),
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Should issue a token that expires with the email verification expiration")
    void issue_shouldUseEmailVerificationExpiration() {
        // When
        IssuedToken issued = verificationTokenService.issue(USER_ID, VerificationPurpose.EMAIL_VERIFICATION);

        // Then
        assertThat(issued.token()).isNotBlank();
        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));

        ArgumentCaptor<byte[]> hash = ArgumentCaptor.forClass(byte[].class);
        verify(repository).save(any());
        verify(repository).invalidateActive(USER_ID, VerificationPurpose.EMAIL_VERIFICATION, NOW);
    }

    @Test
    @DisplayName("Should issue a token that expires with the password reset expiration")
    void issue_shouldUsePasswordResetExpiration() {
        // When
        IssuedToken issued = verificationTokenService.issue(USER_ID, VerificationPurpose.PASSWORD_RESET);

        // Then
        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        verify(repository).invalidateActive(USER_ID, VerificationPurpose.PASSWORD_RESET, NOW);
    }

    @Test
    @DisplayName("Should consume a valid token and return the subject id")
    void consume_shouldReturnSubjectId() {
        // Given
        var stored = new dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationToken();
        stored.setId(UUID.randomUUID());
        stored.setSubjectId(USER_ID);
        stored.setHash(OpaqueTokens.sha256("raw-token"));
        stored.setPurpose(VerificationPurpose.EMAIL_VERIFICATION);
        stored.setExpiresAt(NOW.plus(Duration.ofHours(1)));

        when(repository.findByHashAndPurpose(OpaqueTokens.sha256("raw-token"), VerificationPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(stored));
        when(repository.markUsed(eq(stored.getId()), any())).thenReturn(1);

        // When
        Optional<UUID> subject = verificationTokenService.consume(VerificationPurpose.EMAIL_VERIFICATION, "raw-token");

        // Then
        assertThat(subject).contains(USER_ID);
        verify(repository).markUsed(stored.getId(), NOW);
    }

    @Test
    @DisplayName("Should return empty when the token is unknown")
    void consume_shouldReturnEmptyWhenTokenIsUnknown() {
        // Given
        when(repository.findByHashAndPurpose(OpaqueTokens.sha256("unknown"), VerificationPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());

        // When / Then
        assertThat(verificationTokenService.consume(VerificationPurpose.EMAIL_VERIFICATION, "unknown"))
                .isEmpty();
    }

    @Test
    @DisplayName("Should return empty when the token cannot be marked as used")
    void consume_shouldReturnEmptyWhenTokenCannotBeMarkedUsed() {
        // Given
        var stored = new dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationToken();
        stored.setId(UUID.randomUUID());
        stored.setSubjectId(USER_ID);
        stored.setPurpose(VerificationPurpose.EMAIL_VERIFICATION);
        stored.setExpiresAt(NOW.plus(Duration.ofHours(1)));

        when(repository.findByHashAndPurpose(OpaqueTokens.sha256("raw-token"), VerificationPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(stored));
        when(repository.markUsed(eq(stored.getId()), any())).thenReturn(0);

        // When / Then
        assertThat(verificationTokenService.consume(VerificationPurpose.EMAIL_VERIFICATION, "raw-token"))
                .isEmpty();
    }
}
