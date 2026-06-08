package dev.genesshoan.fitnesstrackerapi.auth.service;

import dev.genesshoan.fitnesstrackerapi.auth.TokenRepository;
import dev.genesshoan.fitnesstrackerapi.auth.domain.Token;
import dev.genesshoan.fitnesstrackerapi.auth.dto.LoginRequestDTO;
import dev.genesshoan.fitnesstrackerapi.auth.dto.RegisterRequestDTO;
import dev.genesshoan.fitnesstrackerapi.auth.dto.TokenResponseDTO;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidCredentialsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.user.domain.Role;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.domain.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.mapper.UserMapper;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service responsible for authentication operations including registration,
 * login, token refresh and logout.
 *
 * Manages JWT access and refresh token lifecycle, including generation,
 * persistence and revocation.
 */
@Slf4j
@Service
@AllArgsConstructor
public class AuthService {

  private final TokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final UserMapper userMapper;
  private final AuthenticationManager authenticationManager;

  /**
   * Registers a new user and returns a token pair.
   *
   * Validates that the username and email are not already taken,
   * encodes the password, persists the user, and issues an access
   * token and a refresh token.
   *
   * @param dto the registration request containing username, email and password
   * @return a {@link TokenResponseDTO} with the access token and refresh token
   * @throws ResourceAlreadyExistsException if the username or email is already
   *                                        registered
   */
  @Transactional
  public TokenResponseDTO register(RegisterRequestDTO dto) {

    log.info("Registration attempt. email={}, username={}",
        dto.email(),
        dto.username());

    if (userRepository.existsByUsername(dto.username())) {
      log.warn("Registration failed: username already exists. username={}",
          dto.username());

      throw new ResourceAlreadyExistsException("Username already exists");
    }

    if (userRepository.existsByEmail(dto.email())) {
      log.warn("Registration failed: email already exists. email={}",
          dto.email());

      throw new ResourceAlreadyExistsException("Email already exists");
    }

    var user = userMapper.toEntity(dto);
    user.setRole(Role.USER);
    user.setPasswordHash(passwordEncoder.encode(dto.password()));

    var savedUser = userRepository.save(user);

    log.info("User registered successfully. userId={}, email={}",
        savedUser.getId(),
        savedUser.getEmail());

    return generateAndPersistTokens(savedUser, UUID.randomUUID());
  }

  /**
   * Authenticates a user by email and password and returns a token pair.
   *
   * Delegates credential verification to the {@link AuthenticationManager}.
   * If authentication succeeds, issues a new access token and refresh token.
   *
   * @param dto the login request containing email and password
   * @return a {@link TokenResponseDTO} with the access token and refresh token
   * @throws ResourceNotFoundException if the user is not found after
   *                                   authentication
   */
  @Transactional
  public TokenResponseDTO login(LoginRequestDTO dto) {

    log.info("Login attempt. email={}", dto.email());

    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));
    } catch (AuthenticationException e) {
      throw new InvalidCredentialsException("Invalid email or password");
    }

    var user = userRepository.findByEmail(dto.email())
        .orElseThrow(() -> {
          log.warn("Login failed: user not found. email={}", dto.email());
          return new ResourceNotFoundException("User not found");
        });

    log.info("Login successful. userId={}, email={}",
        user.getId(),
        user.getEmail());

    return generateAndPersistTokens(user, UUID.randomUUID());
  }

  /**
   * Issues a new token pair in exchange for a valid refresh token.
   *
   * Validates the incoming refresh token, deletes it from the database,
   * and issues a new access token and refresh token (rotation).
   *
   * Note:
   * JWT signature and expiration are validated during parsing of the token
   * via JwtService. If the token is expired, malformed or has an invalid signature,
   * a JwtException will be thrown before any business logic is executed.
   *
   * @param authHeader the Authorization header containing the Bearer refresh
   *                   token
   * @return a {@link TokenResponseDTO} with the new access token and refresh
   *         token
   * @throws BadRequestException       if the Authorization header is missing or
   *                                   malformed
   * @throws MalformedJwtException     if the token does not contain a valid
   *                                   subject
   * @throws JwtException              if the token is invalid or expired
   * @throws ResourceNotFoundException if the user associated with the token is
   *                                   not found
   */
  @Transactional
  public TokenResponseDTO refreshToken(String authHeader) {

    String incomingRefreshToken = extractBearerToken(authHeader);

    UUID oldTokenId = jwtService.extractJti(incomingRefreshToken);
    UUID familyId = jwtService.extractFamilyId(incomingRefreshToken);


    var user = extractUserFromToken(incomingRefreshToken);

    log.info("Refresh token attempt. userId={}, tokenId={}",
        user.getId(),
        oldTokenId);

    var token = tokenRepository.findById(oldTokenId)
        .orElseThrow(() -> new JwtException("Unknown refresh token"));

    if (token.isRevoked()) {
      log.warn("Refresh token reuse detected. Revoking entire family. userId={}, tokenId={}, familyId={}",
          user.getId(),
          oldTokenId,
          familyId);

      tokenRepository.revokeByFamily(familyId);

      throw new JwtException("Refresh token reuse detected. Family revoked");
    }

    token.setRevoked(true);

    var response = generateAndPersistTokens(user, familyId);

    log.info("Token refresh successful. userId={}, oldTokenId={}, newTokenId={}",
        user.getId(),
        oldTokenId,
        jwtService.extractJti(response.refreshToken()));

    return response;
  }

  /**
   * Revokes the refresh token associated with the given Authorization header.
   *
   * Extracts the token, validates the subject, and deletes the token
   * from the database. The client is responsible for discarding the access token.
   *
   * Note:
   * JWT signature and expiration are validated during parsing of the token
   * via JwtService. If the token is expired, malformed or has an invalid signature,
   * a JwtException will be thrown before any business logic is executed.
   *
   * @param authHeader the Authorization header containing the Bearer refresh
   *                   token
   * @throws BadRequestException       if the Authorization header is missing or
   *                                   malformed
   * @throws MalformedJwtException     if the token does not contain a valid
   *                                   subject
   * @throws ResourceNotFoundException if the user associated with the token is
   *                                   not found
   */
  @Transactional
  public void logout(String authHeader) {

    String incomingRefreshToken = extractBearerToken(authHeader);

    var user = extractUserFromToken(incomingRefreshToken);
    UUID familyId = jwtService.extractFamilyId(incomingRefreshToken);

    log.info("Logout request. userId={}, familyId={}",
        user.getId(),
        familyId);

    tokenRepository.revokeByFamily(familyId);

    log.info("Logout successful. userId={}, familyId={}",
        user.getId(),
        familyId);
  }

  /**
   * Generates a new access token and refresh token for the given user,
   * persists the refresh token, and returns both as a {@link TokenResponseDTO}.
   *
   * @param user the authenticated user
   * @return a {@link TokenResponseDTO} with the access token and refresh token
   */
  private TokenResponseDTO generateAndPersistTokens(User user, UUID familyId) {

    var tokenEntity = tokenRepository.save(Token.builder()
        .familyId(familyId)
        .user(user)
        .expiresAt(jwtService.getRefreshTokenExpirationInstant())
        .build());

    var userDetails = new UserDetailsImpl(user);

    var accessToken = jwtService.generateToken(userDetails);
    var refreshToken = jwtService.generateRefreshToken(userDetails, tokenEntity.getJti(), familyId);

    return new TokenResponseDTO(accessToken, refreshToken);
  }

  /**
   * Extracts and validates the Bearer token from the Authorization header.
   *
   * @param authHeader the Authorization header value
   * @return the raw JWT string without the Bearer prefix
   * @throws BadRequestException if the header is null or does not start with
   *                             "Bearer "
   */
  private String extractBearerToken(String authHeader) {

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new BadRequestException("Missing Bearer Token");
    }
    return authHeader.substring(7);
  }

  /**
   * Extracts the email from the token and loads the associated user.
   *
   * @param token the raw JWT string
   * @return the {@link User} associated with the token's subject
   * @throws ResourceNotFoundException if no user is found for the extracted email
   */
  private User extractUserFromToken(String token) {

    String email = jwtService.extractUsername(token);

    return userRepository.findByEmail(email)
            .orElseThrow(() -> {
              log.warn("User not found for token subject={}", email);
              return new ResourceNotFoundException("User not found");
            });
  }
}
