package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.f4b6a3.uuid.UuidCreator;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.EmailNotVerifiedException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidCredentialsException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidRefreshTokenException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.RefreshTokenReuseException;
import dev.genesshoan.fitnesstrackerapi.auth.application.result.AuthTokens;
import dev.genesshoan.fitnesstrackerapi.auth.domain.RefreshToken;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config.RefreshTokenProperties;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.RefreshTokenRepository;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security.JwtService;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security.OpaqueTokens;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.TokenResponse;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetRefreshUserDataUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for authentication operations including registration,
 * login, token refresh and logout.
 *
 * <p>Manages JWT access and refresh token lifecycle, including generation,
 * persistence and revocation.
 *
 * <p>This service follows hexagonal architecture principles, delegating
 * credential verification to Spring Security's {@link AuthenticationManager}
 * and persisting refresh tokens via {@link RefreshTokenRepository}.
 *
 * <p><b>Token Rotation:</b> On each refresh, the old token is revoked and
 * a new pair is issued. This prevents replay attacks and ensures tokens
 * are single-use.
 *
 * <p><b>Reuse Detection:</b> If a revoked token is reused, the entire token
 * family is revoked to prevent session hijacking.
 *
 * @see RefreshTokenRepository
 * @see JwtService
 * @see AuthenticationManager
 */
@Slf4j
@Service
@AllArgsConstructor
public class SessionService {

    private final RefreshTokenRepository tokenRepository;
    private final GetRefreshUserDataUseCase getRefreshUserDataUseCase;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenProperties refreshTokenProperties;
    private final Clock clock;

    /**
     * Authenticates a user by email and password and returns a token pair.
     *
     * <p>Delegates credential verification to the {@link AuthenticationManager}.
     * If authentication succeeds, issues a new access token and refresh token.
     *
     * <p>Before issuing tokens, validates that the user's email is verified.
     * Unverified users are prevented from logging in until email verification
     * is complete.
     *
     * @param email    the user's email address for authentication
     * @param password the user's password for authentication
     * @return a {@link AuthTokens} with the access token and refresh token
     * @throws InvalidCredentialsException if the email or password is invalid
     * @throws EmailNotVerifiedException   if the user's email is not verified
     * @throws ResourceNotFoundException   if the user is not found after authentication
     *
     * @see AuthenticationManager#authenticate(Authentication)
     * @see #login(String, String)
     */
    @Transactional
    public AuthTokens login(String email, String password) {
        log.info("Login attempt. email={}", email);

        Authentication authenticated;

        try {
            authenticated =
                    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        var authenticatedUser = (UserDetailsImpl) authenticated.getPrincipal();

        if (!authenticatedUser.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email address has not been verified");
        }

        var authorities = authenticatedUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        log.info("Login successful. userId={}, email={}", authenticatedUser.getId(), email);

        return issueTokens(authenticatedUser.getId(), authorities, UuidCreator.getTimeOrderedEpoch());
    }

    /**
     * Issues a new token pair in exchange for a valid refresh token.
     *
     * <p>Validates the incoming refresh token, deletes it from the database,
     * and issues a new access token and refresh token (rotation).
     *
     * <p>Note:
     * JWT signature and expiration are validated during parsing of the token
     * via JwtService. If the token is expired, malformed or has an invalid
     * signature, a JwtException will be thrown before any business logic is executed.
     *
     * <p><b>Security:</b> On successful refresh, the consumed token is marked
     * as revoked. If the token was already revoked, the entire token family
     * is revoked to prevent session hijacking (reuse detection).
     *
     * @param incomingRefreshToken the raw refresh token string from the cookie
     * @return a {@link TokenResponse} with the new access token and refresh token
     * @throws InvalidRefreshTokenException if the token is invalid, expired, or not found
     * @throws RefreshTokenReuseException   if a revoked token is reused (entire family revoked)
     *
     * @see #revokeById(UUID)
     * @see #revokeByFamily(UUID)
     */
    @Transactional(noRollbackFor = {RefreshTokenReuseException.class, InvalidRefreshTokenException.class})
    public AuthTokens refreshToken(String incomingRefreshToken) {
        var token = tokenRepository
                .findByHash(OpaqueTokens.sha256(incomingRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        log.info("Refresh token attempt. userId={}, tokenId={}", token.getUserId(), token.getId());

        if (token.isRevoked()) {
            tokenRepository.revokeByFamily(token.getFamilyId());

            throw new RefreshTokenReuseException();
        }

        if (!token.getExpiresAt().isAfter(clock.instant())) {
            throw new InvalidRefreshTokenException();
        }

        var user = getRefreshUserDataUseCase
                .findRefreshDataById(token.getUserId())
                .orElseThrow(() -> {
                    tokenRepository.revokeByFamily(token.getFamilyId());
                    return new InvalidRefreshTokenException();
                });

        tokenRepository.revokeById(token.getId());

        var response = issueTokens(token.getUserId(), List.of("ROLE_" + user.role()), token.getFamilyId());

        log.info("Token refresh successful. userId={}, oldTokenId={}", token.getUserId(), token.getId());

        return response;
    }

    /**
     * Revokes the refresh token associated with the given Authorization header.
     *
     * <p>Extracts the token, validates the subject, and deletes the token
     * from the database. The client is responsible for discarding the access token.
     *
     * <p>Note:
     * JWT signature and expiration are validated during parsing of the token
     * via JwtService. If the token is expired, malformed or has an invalid
     * signature, a JwtException will be thrown before any business logic is executed.
     *
     * <p>On logout, the entire token family is revoked (not just the single token),
     * ensuring the user cannot generate new access tokens even if they have
     * existing refresh tokens in circulation.
     *
     * @param incomingRefreshToken the raw refresh token string from the cookie
     * @throws InvalidRefreshTokenException if the token is invalid, expired, or not found
     *
     * @see #revokeByHash(byte[])
     */
    @Transactional
    public void logout(String incomingRefreshToken) {
        log.info("Logout attempt");
        tokenRepository.revokeByHash(OpaqueTokens.sha256(incomingRefreshToken));
        log.info("Logout completed. Refresh token family revoked by hash.");
    }

    /**
     * Generates a new access token and refresh token for the given user,
     * persists the refresh token, and returns both as a {@link TokenResponse}.
     *
     * <p>Creates a cryptographically secure refresh token using {@link OpaqueTokens},
     * hashes it for storage, and persists the {@link RefreshToken} entity.
     * The access token is generated via {@link JwtService}.
     *
     * @param userId       the user's UUID identifier
     * @param authorities  the user's authorities/roles for the access token
     * @param familyId     the token family ID for grouping related tokens
     * @return a {@link AuthTokens} containing the access token and refresh token
     *
     * @see OpaqueTokens#generate()
     * @see OpaqueTokens#sha256(String)
     * @see JwtService#buildAccessToken(String, List)
     */
    private AuthTokens issueTokens(UUID userId, List<String> authorities, UUID familyId) {
        log.info("Issuing new tokens for user: {}, familyId: {}", userId, familyId);
        var refreshToken = OpaqueTokens.generate();

        tokenRepository.save(RefreshToken.builder()
                .familyId(familyId)
                .hash(OpaqueTokens.sha256(refreshToken))
                .userId(userId)
                .expiresAt(clock.instant().plus(refreshTokenProperties.expiration()))
                .build());

        var accessToken = jwtService.buildAccessToken(userId, authorities);
        return new AuthTokens(accessToken, refreshToken);
    }
}
