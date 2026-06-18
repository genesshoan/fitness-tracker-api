package dev.genesshoan.fitnesstrackerapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.genesshoan.fitnesstrackerapi.auth.domain.Token;
import dev.genesshoan.fitnesstrackerapi.auth.dto.LoginRequestDTO;
import dev.genesshoan.fitnesstrackerapi.auth.dto.RegisterRequestDTO;
import dev.genesshoan.fitnesstrackerapi.auth.dto.TokenResponseDTO;
import dev.genesshoan.fitnesstrackerapi.auth.service.JwtService;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.user.domain.Role;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.mapper.UserMapper;
import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private TokenRepository tokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;
  @Mock private UserMapper userMapper;
  @Mock private AuthenticationManager authenticationManager;

  @InjectMocks private AuthService authService;

  private RegisterRequestDTO registerDto;
  private User user;
  private User savedUser;
  private Token token;

  @BeforeEach
  void setUp() {
    registerDto = new RegisterRequestDTO("shoan", "12345678", "shoan@test.com");

    user = User.builder()
            .username("shoan")
            .email("shoan@test.com")
            .build();

    savedUser = User.builder()
            .id(1L)
            .username("shoan")
            .email("shoan@test.com")
            .build();

    token = Token.builder()
            .jti(UUID.randomUUID())
            .familyId(UUID.randomUUID())
            .build();
  }

  @Test
  void register_shouldRegisterUserSuccessfully() {

    when(userRepository.existsByUsername(registerDto.username())).thenReturn(false);
    when(userRepository.existsByEmail(registerDto.email())).thenReturn(false);

    when(userMapper.toEntity(registerDto)).thenReturn(user);
    when(passwordEncoder.encode(registerDto.password())).thenReturn("encoded-password");
    when(userRepository.save(user)).thenReturn(savedUser);

    when(tokenRepository.save(any(Token.class))).thenReturn(token);
    when(jwtService.getRefreshTokenExpirationInstant()).thenReturn(Instant.now().plusSeconds(86400));

    when(jwtService.generateToken(any(UserDetailsImpl.class))).thenReturn("access-token");
    when(jwtService.generateRefreshToken(any(UserDetailsImpl.class), eq(token.getJti()), any(UUID.class)))
            .thenReturn("refresh-token");

    TokenResponseDTO result = authService.register(registerDto);

    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");

    assertThat(user.getRole()).isEqualTo(Role.USER);
    assertThat(user.getPasswordHash()).isEqualTo("encoded-password");

    verify(userRepository).save(user);
    verify(tokenRepository).save(any(Token.class));
  }

  @Test
  void register_shouldThrowWhenUsernameExists() {

    when(userRepository.existsByUsername(registerDto.username())).thenReturn(true);

    assertThatThrownBy(() -> authService.register(registerDto))
            .isInstanceOf(ResourceAlreadyExistsException.class);

    verify(userRepository, never()).save(any());
  }

  @Test
  void register_shouldThrowWhenEmailExists() {

    when(userRepository.existsByEmail(registerDto.email())).thenReturn(true);

    assertThatThrownBy(() -> authService.register(registerDto))
            .isInstanceOf(ResourceAlreadyExistsException.class);

    verify(userRepository, never()).save(any());
  }

  @Test
  void login_shouldThrowWhenUserNotFound() {

    when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    LoginRequestDTO dto = new LoginRequestDTO("test@test.com", "password");

    assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void login_shouldLoginSuccessfully() {

    when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(savedUser));

    when(tokenRepository.save(any(Token.class))).thenReturn(token);
    when(jwtService.getRefreshTokenExpirationInstant()).thenReturn(Instant.now().plusSeconds(86400));

    when(jwtService.generateToken(any(UserDetailsImpl.class))).thenReturn("access-token");
    when(jwtService.generateRefreshToken(any(UserDetailsImpl.class), eq(token.getJti()), any(UUID.class)))
            .thenReturn("refresh-token");

    TokenResponseDTO result =
            authService.login(new LoginRequestDTO("shoan@test.com", "password"));

    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");

    verify(tokenRepository).save(any(Token.class));
  }

  @Test
  void refresh_shouldThrowWhenHeaderInvalid() {

    assertThatThrownBy(() -> authService.refreshToken("invalid"))
            .isInstanceOf(BadRequestException.class);

    assertThatThrownBy(() -> authService.refreshToken(null))
            .isInstanceOf(BadRequestException.class);

    verifyNoInteractions(tokenRepository);
  }

  @Test
  void refresh_shouldThrowWhenTokenReused() {

    String rawToken = "token";

    UUID jti = UUID.randomUUID();
    UUID familyId = UUID.randomUUID();

    when(jwtService.extractJti(rawToken)).thenReturn(jti);
    when(jwtService.extractFamilyId(rawToken)).thenReturn(familyId);
    when(jwtService.extractUsername(rawToken)).thenReturn("test@test.com");

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

    Token dbToken = Token.builder()
            .jti(jti)
            .familyId(familyId)
            .build();
    dbToken.setRevoked(true);

    when(tokenRepository.findById(jti)).thenReturn(Optional.of(dbToken));

    assertThatThrownBy(() -> authService.refreshToken("Bearer " + rawToken))
            .isInstanceOf(JwtException.class)
            .hasMessage("Refresh token reuse detected. Family revoked");

    verify(tokenRepository).revokeByFamily(familyId);
  }

  @Test
  void refresh_shouldRefreshSuccessfully() {

    String rawToken = "token";

    UUID jti = UUID.randomUUID();
    UUID familyId = UUID.randomUUID();

    when(jwtService.extractJti(rawToken)).thenReturn(jti);
    when(jwtService.extractFamilyId(rawToken)).thenReturn(familyId);
    when(jwtService.extractUsername(rawToken)).thenReturn("test@test.com");

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

    when(tokenRepository.findById(jti)).thenReturn(Optional.of(token));
    when(tokenRepository.save(any(Token.class))).thenReturn(token);

    when(jwtService.getRefreshTokenExpirationInstant()).thenReturn(Instant.now().plusSeconds(86400));
    when(jwtService.generateToken(any(UserDetailsImpl.class))).thenReturn("access-token");
    when(jwtService.generateRefreshToken(any(UserDetailsImpl.class), eq(token.getJti()), any(UUID.class)))
            .thenReturn("refresh-token");

    TokenResponseDTO result = authService.refreshToken("Bearer " + rawToken);

    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");

    verify(tokenRepository).save(any(Token.class));
  }

  @Test
  void logout_shouldRevokeFamily() {

    String rawToken = "token";
    UUID familyId = UUID.randomUUID();

    when(jwtService.extractUsername(rawToken)).thenReturn("test@test.com");
    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    when(jwtService.extractFamilyId(rawToken)).thenReturn(familyId);

    authService.logout("Bearer " + rawToken);

    verify(tokenRepository).revokeByFamily(familyId);
  }
}