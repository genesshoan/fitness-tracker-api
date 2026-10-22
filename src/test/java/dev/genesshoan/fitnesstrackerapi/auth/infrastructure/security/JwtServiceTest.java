package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidJwtException;
import dev.genesshoan.fitnesstrackerapi.auth.application.result.TokenData;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}.
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("fitness-tracker-test-secret-key-32bytes".getBytes(StandardCharsets.UTF_8));
    private static final Duration EXPIRATION = Duration.ofMinutes(15);
    private static final UUID USER_ID = UUID.fromString("01932f4a-1234-7000-8000-123456789abc");
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = serviceWithSecret(SECRET, Clock.systemUTC());
    }

    @Test
    @DisplayName("Should build an access token that can be parsed back")
    void buildAccessToken_shouldCreateParsableToken() {
        // When
        String token = jwtService.buildAccessToken(USER_ID, List.of("ROLE_USER"));

        // Then
        TokenData data = jwtService.parseToken(token);

        assertThat(data.subject()).isEqualTo(USER_ID);
        assertThat(data.authorities()).extracting("authority").containsExactly("ROLE_USER");
        assertThat(data.issuedAt()).isNotNull();
        assertThat(data.expiresAt()).isAfter(data.issuedAt());
    }

    @Test
    @DisplayName("Should throw when the token is not signed by the application")
    void parseToken_shouldThrowWhenSignatureIsInvalid() {
        // Given
        String foreignToken =
                serviceWithSecret(SECRET, Clock.systemUTC()).buildAccessToken(USER_ID, List.of("ROLE_USER"));

        JwtService otherService = serviceWithSecret(
                Base64.getEncoder()
                        .encodeToString("another-fitness-tracker-secret-key-32b".getBytes(StandardCharsets.UTF_8)),
                Clock.systemUTC());

        // When / Then
        assertThatThrownBy(() -> otherService.parseToken(foreignToken)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    @DisplayName("Should throw when the token is expired")
    void parseToken_shouldThrowWhenTokenIsExpired() {
        // Given
        JwtService expiredIssuer =
                serviceWithSecret(SECRET, Clock.fixed(NOW.minus(Duration.ofHours(2)), ZoneOffset.UTC));
        String expiredToken = expiredIssuer.buildAccessToken(USER_ID, List.of("ROLE_USER"));

        // When / Then
        assertThatThrownBy(() -> jwtService.parseToken(expiredToken)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    @DisplayName("Should throw when the token is malformed")
    void parseToken_shouldThrowWhenTokenIsMalformed() {
        // When / Then
        assertThatThrownBy(() -> jwtService.parseToken("not-a-jwt")).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    @DisplayName("Should throw when the token is missing")
    void parseToken_shouldThrowWhenTokenIsNull() {
        // When / Then
        assertThatThrownBy(() -> jwtService.parseToken(null)).isInstanceOf(InvalidJwtException.class);
    }

    private JwtService serviceWithSecret(String secret, Clock clock) {
        JwtService service = new JwtService(new JwtProperties(secret, EXPIRATION), clock);
        ReflectionTestUtils.invokeMethod(service, "init");
        return service;
    }
}
