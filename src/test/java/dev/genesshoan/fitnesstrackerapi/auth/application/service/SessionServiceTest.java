package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import dev.genesshoan.fitnesstrackerapi.auth.application.exception.EmailNotVerifiedException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidCredentialsException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidRefreshTokenException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.RefreshTokenReuseException;
import dev.genesshoan.fitnesstrackerapi.auth.domain.RefreshToken;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config.RefreshTokenProperties;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.RefreshTokenRepository;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security.JwtService;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security.OpaqueTokens;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetRefreshUserDataUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.RefreshUserData;
import dev.genesshoan.fitnesstrackerapi.user.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SessionService}.
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final UUID USER_ID = UUID.fromString("01932f4a-1234-7000-8000-123456789abc");
    private static final UUID FAMILY_ID = UUID.fromString("01932f4a-9999-7000-8000-123456789abc");
    private static final String EMAIL = "shoan@test.com";
    private static final String PASSWORD = "password";
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Mock
    private RefreshTokenRepository tokenRepository;

    @Mock
    private GetRefreshUserDataUseCase getRefreshUserDataUseCase;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(
                tokenRepository,
                getRefreshUserDataUseCase,
                jwtService,
                authenticationManager,
                new RefreshTokenProperties(Duration.ofDays(7), true),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Should issue an access token and a refresh token on login")
    void login_shouldIssueTokens() {
        // Given
        when(authenticationManager.authenticate(any())).thenReturn(authentication(true));
        when(jwtService.buildAccessToken(USER_ID, List.of("ROLE_USER"))).thenReturn("access-token");

        // When
        var tokens = sessionService.login(EMAIL, PASSWORD);

        // Then
        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isNotBlank();

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokenRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(saved.getValue().isRevoked()).isFalse();
    }

    @Test
    @DisplayName("Should throw when the credentials are invalid")
    void login_shouldThrowWhenCredentialsAreInvalid() {
        // Given
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        // When / Then
        assertThatThrownBy(() -> sessionService.login(EMAIL, PASSWORD)).isInstanceOf(InvalidCredentialsException.class);

        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when the email is not verified")
    void login_shouldThrowWhenEmailIsNotVerified() {
        // Given
        when(authenticationManager.authenticate(any())).thenReturn(authentication(false));

        // When / Then
        assertThatThrownBy(() -> sessionService.login(EMAIL, PASSWORD)).isInstanceOf(EmailNotVerifiedException.class);

        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should rotate the refresh token")
    void refreshToken_shouldRotateToken() {
        // Given
        RefreshToken stored = refreshToken(false, NOW.plus(Duration.ofDays(1)));
        when(tokenRepository.findByHash(OpaqueTokens.sha256("refresh-token"))).thenReturn(Optional.of(stored));
        when(getRefreshUserDataUseCase.findRefreshDataById(USER_ID))
                .thenReturn(Optional.of(new RefreshUserData(Role.USER)));
        when(jwtService.buildAccessToken(USER_ID, List.of("ROLE_USER"))).thenReturn("new-access-token");

        // When
        var tokens = sessionService.refreshToken("refresh-token");

        // Then
        assertThat(tokens.accessToken()).isEqualTo("new-access-token");
        assertThat(tokens.refreshToken()).isNotBlank();

        verify(tokenRepository).revokeById(stored.getId());
        verify(tokenRepository).save(any(RefreshToken.class));
        verify(jwtService).buildAccessToken(USER_ID, List.of("ROLE_USER"));
    }

    @Test
    @DisplayName("Should throw when the refresh token is unknown")
    void refreshToken_shouldThrowWhenTokenNotFound() {
        // Given
        when(tokenRepository.findByHash(OpaqueTokens.sha256("unknown"))).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> sessionService.refreshToken("unknown"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("Should revoke the whole family when a revoked token is reused")
    void refreshToken_shouldRevokeFamilyWhenTokenIsReused() {
        // Given
        RefreshToken stored = refreshToken(true, NOW.plus(Duration.ofDays(1)));
        when(tokenRepository.findByHash(OpaqueTokens.sha256("refresh-token"))).thenReturn(Optional.of(stored));

        // When / Then
        assertThatThrownBy(() -> sessionService.refreshToken("refresh-token"))
                .isInstanceOf(RefreshTokenReuseException.class);

        verify(tokenRepository).revokeByFamily(stored.getFamilyId());
    }

    @Test
    @DisplayName("Should throw when the refresh token is expired")
    void refreshToken_shouldThrowWhenTokenIsExpired() {
        // Given
        RefreshToken stored = refreshToken(false, NOW.minus(Duration.ofMinutes(1)));
        when(tokenRepository.findByHash(OpaqueTokens.sha256("refresh-token"))).thenReturn(Optional.of(stored));

        // When / Then
        assertThatThrownBy(() -> sessionService.refreshToken("refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(tokenRepository, never()).revokeById(any());
    }

    @Test
    @DisplayName("Should revoke the token family on logout")
    void logout_shouldRevokeByHash() {
        // When
        sessionService.logout("refresh-token");

        // Then
        verify(tokenRepository).revokeByHash(OpaqueTokens.sha256("refresh-token"));
    }

    private UsernamePasswordAuthenticationToken authentication(boolean emailVerified) {
        UserDetailsImpl principal = new UserDetailsImpl(
                USER_ID,
                EMAIL,
                EMAIL,
                "shoan",
                "hash",
                emailVerified,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private RefreshToken refreshToken(boolean revoked, Instant expiresAt) {
        RefreshToken token = RefreshToken.builder()
                .familyId(FAMILY_ID)
                .userId(USER_ID)
                .hash(OpaqueTokens.sha256("refresh-token"))
                .revoked(revoked)
                .expiresAt(expiresAt)
                .build();
        token.setId(UUID.randomUUID());
        return token;
    }
}
