package dev.genesshoan.fitnesstrackerapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.genesshoan.fitnesstrackerapi.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.auth.service.JwtService;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidJwtException;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.user.domain.Role;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Value("${application.security.jwt.secret}")
    private String secret;

    private User user;
    private UserDetailsImpl userDetailsImpl;

    @BeforeEach
    void setUp() {
        user = User.builder()
            .username("test")
            .email("test@test.com")
            .role(Role.USER)
            .id(1L)
            .build();

        userDetailsImpl = new UserDetailsImpl(user);
    }

    @Test
    void shouldGenerateAndExtractUsername() {
        String token = jwtService.generateToken(userDetailsImpl);

        assertThat(jwtService.extractUsername(token)).isEqualTo(
            userDetailsImpl.getUsername()
        );
    }

    @Test
    void shouldExtractJtiAndFamilyId() {
        UUID jti = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();

        String token = jwtService.generateRefreshToken(
            userDetailsImpl,
            jti,
            familyId
        );

        assertThat(jwtService.extractJti(token)).isEqualTo(jti);

        assertThat(jwtService.extractFamilyId(token)).isEqualTo(familyId);
    }

    @Test
    void shouldExtractDifferentUsername() {
        String token = Jwts.builder()
            .subject("other@test.com")
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
            .compact();

        assertThat(jwtService.extractUsername(token)).isEqualTo(
            "other@test.com"
        );
    }

    @Test
    void shouldThrowIfJtiDoesNotExist() {
        String token = Jwts.builder()
            .subject("test@test.com")
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
            .compact();

        assertThatThrownBy(() -> jwtService.extractJti(token)).isInstanceOf(
            InvalidJwtException.class
        );
    }

    @Test
    void shouldThrowIfFamilyIdDoesNotExist() {
        String token = Jwts.builder()
            .subject("test@test.com")
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
            .compact();

        assertThatThrownBy(() ->
            jwtService.extractFamilyId(token)
        ).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(
                "v/mx8wsm2PoKQm05MYR+T30B8xKMIoW56EoybDugR0c="
            )
        );

        String token = Jwts.builder()
            .subject("test@test.com")
            .signWith(otherKey)
            .compact();

        assertThatThrownBy(() ->
            jwtService.extractUsername(token)
        ).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void shouldThrowWhenTokenIsExpired() {
        Instant expiredTime = Instant.now().minus(Duration.ofDays(40));

        String token = Jwts.builder()
            .subject("test@test.com")
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
            .expiration(Date.from(expiredTime))
            .compact();

        assertThatThrownBy(() ->
            jwtService.extractUsername(token)
        ).isInstanceOf(InvalidJwtException.class);
    }
}
