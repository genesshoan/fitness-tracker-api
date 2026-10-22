package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.genesshoan.fitnesstrackerapi.auth.application.command.RegisterCommand;
import dev.genesshoan.fitnesstrackerapi.auth.application.event.VerificationEmailRequested;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidCodeException;
import dev.genesshoan.fitnesstrackerapi.auth.application.result.IssuedToken;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationPurpose;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.RegisterUserUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.VerifyUserEmailUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.PendingVerificationUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RegistrationService}.
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    private static final UUID USER_ID = UUID.fromString("01932f4a-1234-7000-8000-123456789abc");
    private static final String USERNAME = "shoan";
    private static final String EMAIL = "shoan@test.com";
    private static final String PASSWORD = "password";
    private static final String TOKEN = "raw-verification-token";
    private static final Instant EXPIRES_AT = Instant.parse("2026-01-02T12:00:00Z");

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @Mock
    private VerifyUserEmailUseCase verifyUserEmailUseCase;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                verificationTokenService, eventPublisher, registerUserUseCase, verifyUserEmailUseCase, passwordEncoder);
    }

    @Test
    @DisplayName("Should register the user and publish the verification email event")
    void register_shouldPublishVerificationEmailEvent() {
        // Given
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded");
        when(registerUserUseCase.register(any())).thenReturn(USER_ID);
        when(verificationTokenService.issue(USER_ID, VerificationPurpose.EMAIL_VERIFICATION))
                .thenReturn(new IssuedToken(TOKEN, EXPIRES_AT));

        // When
        registrationService.register(new RegisterCommand(USERNAME, PASSWORD, EMAIL, "UTC"));

        // Then
        verify(registerUserUseCase)
                .register(new dev.genesshoan.fitnesstrackerapi.user.application.port.in.command.RegisterUserCommand(
                        EMAIL, USERNAME, "UTC", "encoded"));
        verify(eventPublisher).publishEvent(new VerificationEmailRequested(USERNAME, EMAIL, TOKEN, EXPIRES_AT));
    }

    @Test
    @DisplayName("Should not publish any event when the registration fails")
    void register_shouldNotPublishEventWhenRegistrationFails() {
        // Given
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded");
        when(registerUserUseCase.register(any())).thenThrow(new ResourceAlreadyExistsException("already exists"));

        // When / Then
        assertThatThrownBy(() -> registrationService.register(new RegisterCommand(USERNAME, PASSWORD, EMAIL, "UTC")))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verifyNoInteractions(verificationTokenService, eventPublisher);
    }

    @Test
    @DisplayName("Should verify the email and mark the account as verified")
    void verifyEmail_shouldMarkEmailAsVerified() {
        // Given
        when(verificationTokenService.consume(VerificationPurpose.EMAIL_VERIFICATION, TOKEN))
                .thenReturn(Optional.of(USER_ID));
        when(verifyUserEmailUseCase.markEmailVerified(USER_ID)).thenReturn(true);

        // When
        registrationService.verifyEmail(TOKEN);

        // Then
        verify(verifyUserEmailUseCase).markEmailVerified(USER_ID);
    }

    @Test
    @DisplayName("Should throw when the verification token is invalid")
    void verifyEmail_shouldThrowWhenTokenIsInvalid() {
        // Given
        when(verificationTokenService.consume(VerificationPurpose.EMAIL_VERIFICATION, TOKEN))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> registrationService.verifyEmail(TOKEN)).isInstanceOf(InvalidCodeException.class);

        verify(verifyUserEmailUseCase, never()).markEmailVerified(any());
    }

    @Test
    @DisplayName("Should throw when the user of a valid token no longer exists")
    void verifyEmail_shouldThrowWhenUserNoLongerExists() {
        // Given
        when(verificationTokenService.consume(VerificationPurpose.EMAIL_VERIFICATION, TOKEN))
                .thenReturn(Optional.of(USER_ID));
        when(verifyUserEmailUseCase.markEmailVerified(USER_ID)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> registrationService.verifyEmail(TOKEN)).isInstanceOf(InvalidCodeException.class);
    }

    @Test
    @DisplayName("Should resend the verification email for an unverified account")
    void resendVerificationEmail_shouldPublishVerificationEmailEvent() {
        // Given
        when(verifyUserEmailUseCase.findUnverifiedByEmail(EMAIL))
                .thenReturn(Optional.of(new PendingVerificationUser(USER_ID, USERNAME)));
        when(verificationTokenService.issue(USER_ID, VerificationPurpose.EMAIL_VERIFICATION))
                .thenReturn(new IssuedToken(TOKEN, EXPIRES_AT));

        // When
        registrationService.resendVerificationEmail(EMAIL);

        // Then
        verify(eventPublisher).publishEvent(new VerificationEmailRequested(USERNAME, EMAIL, TOKEN, EXPIRES_AT));
    }

    @Test
    @DisplayName("Should ignore a resend request for an unknown or verified account")
    void resendVerificationEmail_shouldIgnoreUnknownAccount() {
        // Given
        when(verifyUserEmailUseCase.findUnverifiedByEmail(EMAIL)).thenReturn(Optional.empty());

        // When
        registrationService.resendVerificationEmail(EMAIL);

        // Then
        verifyNoInteractions(verificationTokenService, eventPublisher);
    }
}
