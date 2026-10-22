package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.genesshoan.fitnesstrackerapi.auth.application.event.PasswordChanged;
import dev.genesshoan.fitnesstrackerapi.auth.application.event.PasswordResetRequested;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidCodeException;
import dev.genesshoan.fitnesstrackerapi.auth.application.result.IssuedToken;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationPurpose;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.RefreshTokenRepository;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetUserCredentialsUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.UpdatePasswordHashUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.UserCredentials;
import dev.genesshoan.fitnesstrackerapi.user.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the password reset flow of {@link CredentialsService}.
 */
@ExtendWith(MockitoExtension.class)
class CredentialsServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "shoan@test.com";
    private static final String USERNAME = "shoan";
    private static final String PASSWORD_HASH = "encoded-hash";
    private static final String TOKEN = "raw-reset-token";
    private static final String NEW_PASSWORD = "new-password";
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final Instant EXPIRES_AT = NOW.plusSeconds(3600);

    @Mock
    private GetUserCredentialsUseCase getUserCredentialsUseCase;

    @Mock
    private UpdatePasswordHashUseCase updatePasswordHashUseCase;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Clock clock;

    @InjectMocks
    private CredentialsService credentialsService;

    private UserCredentials credentials;

    @BeforeEach
    void setUp() {
        credentials = new UserCredentials(USER_ID, EMAIL, USERNAME, PASSWORD_HASH, true, Role.USER);
    }

    @Test
    @DisplayName("Should issue a reset token and publish the password reset event")
    void requestPasswordReset_shouldPublishPasswordResetEvent() {
        // Given
        when(getUserCredentialsUseCase.findByEmail(EMAIL)).thenReturn(Optional.of(credentials));
        when(verificationTokenService.issue(USER_ID, VerificationPurpose.PASSWORD_RESET))
                .thenReturn(new IssuedToken(TOKEN, EXPIRES_AT));

        // When
        credentialsService.requestPasswordReset(EMAIL);

        // Then
        verify(verificationTokenService).issue(USER_ID, VerificationPurpose.PASSWORD_RESET);
        verify(eventPublisher).publishEvent(new PasswordResetRequested(USERNAME, EMAIL, TOKEN, EXPIRES_AT));
    }

    @Test
    @DisplayName("Should ignore a password reset request for an unknown account")
    void requestPasswordReset_shouldIgnoreUnknownAccount() {
        // Given
        when(getUserCredentialsUseCase.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // When
        credentialsService.requestPasswordReset(EMAIL);

        // Then
        verifyNoInteractions(verificationTokenService, eventPublisher);
    }

    @Test
    @DisplayName("Should reset the password, revoke the sessions and publish the password changed event")
    void resetPassword_shouldResetPasswordAndPublishEvent() {
        // Given
        when(verificationTokenService.consume(VerificationPurpose.PASSWORD_RESET, TOKEN))
                .thenReturn(Optional.of(USER_ID));
        when(getUserCredentialsUseCase.findById(USER_ID)).thenReturn(Optional.of(credentials));
        when(passwordEncoder.matches(NEW_PASSWORD, PASSWORD_HASH)).thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("new-encoded-hash");
        when(clock.instant()).thenReturn(NOW);

        // When
        credentialsService.resetPassword(TOKEN, NEW_PASSWORD);

        // Then
        verify(updatePasswordHashUseCase).updatePasswordHash(USER_ID, "new-encoded-hash");
        verify(refreshTokenRepository).revokeByUserId(USER_ID);
        verify(eventPublisher).publishEvent(new PasswordChanged(USERNAME, EMAIL, NOW));
    }

    @Test
    @DisplayName("Should throw when the password reset token is invalid, expired or already used")
    void resetPassword_shouldThrowWhenTokenIsInvalid() {
        // Given
        when(verificationTokenService.consume(VerificationPurpose.PASSWORD_RESET, TOKEN))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> credentialsService.resetPassword(TOKEN, NEW_PASSWORD))
                .isInstanceOf(InvalidCodeException.class);

        verifyNoInteractions(updatePasswordHashUseCase, eventPublisher);
    }

    @Test
    @DisplayName("Should throw when the new password matches the current one")
    void resetPassword_shouldThrowWhenNewPasswordMatchesCurrentOne() {
        // Given
        when(verificationTokenService.consume(VerificationPurpose.PASSWORD_RESET, TOKEN))
                .thenReturn(Optional.of(USER_ID));
        when(getUserCredentialsUseCase.findById(USER_ID)).thenReturn(Optional.of(credentials));
        when(passwordEncoder.matches(NEW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> credentialsService.resetPassword(TOKEN, NEW_PASSWORD))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(updatePasswordHashUseCase, eventPublisher);
    }
}
