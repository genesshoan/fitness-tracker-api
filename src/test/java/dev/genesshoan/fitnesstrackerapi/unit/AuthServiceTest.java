package dev.genesshoan.fitnesstrackerapi.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import dev.genesshoan.fitnesstrackerapi.auth.TokenRepository;
import dev.genesshoan.fitnesstrackerapi.auth.domain.Token;
import dev.genesshoan.fitnesstrackerapi.auth.dto.LoginRequestDTO;
import dev.genesshoan.fitnesstrackerapi.auth.dto.RegisterRequestDTO;
import dev.genesshoan.fitnesstrackerapi.auth.dto.TokenResponseDTO;
import dev.genesshoan.fitnesstrackerapi.auth.service.AuthService;
import dev.genesshoan.fitnesstrackerapi.auth.service.JwtService;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidJwtException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import dev.genesshoan.fitnesstrackerapi.user.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.domain.Role;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.mapper.UserMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequestDTO registerDto;
    private User user;
    private User savedUser;
    private Token token;
    private UUID userId;

    @BeforeEach
    void setUp() {
        registerDto = new RegisterRequestDTO(
            "shoan",
            "12345678",
            "shoan@test.com"
        );

        userId = UUID.randomUUID();

        user = UserBuilder.aUser(FAKER)
            .withUsername("shoan")
            .withEmail("shoan@test.com")
            .build();

        savedUser = UserBuilder.aUser(FAKER)
            .withId(userId)
            .withUsername("shoan")
            .withEmail("shoan@test.com")
            .build();

        token = Token.builder()
            .jti(UUID.randomUUID())
            .familyId(UUID.randomUUID())
            .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void register_shouldRegisterUserSuccessfully() {
        // Given
        when(
            userRepository.existsByUsername(registerDto.username())
        ).thenReturn(false);

        when(userRepository.existsByEmail(registerDto.email())).thenReturn(
            false
        );

        when(userMapper.toEntity(registerDto)).thenReturn(user);

        when(passwordEncoder.encode(registerDto.password())).thenReturn(
            "encoded-password"
        );

        when(userRepository.save(user)).thenReturn(savedUser);

        when(tokenRepository.save(any(Token.class))).thenReturn(token);

        when(jwtService.getRefreshTokenExpirationInstant()).thenReturn(
            Instant.now().plusSeconds(86400)
        );

        when(jwtService.generateToken(any(UserDetailsImpl.class))).thenReturn(
            "access-token"
        );

        when(
            jwtService.generateRefreshToken(
                any(UserDetailsImpl.class),
                eq(token.getJti()),
                any(UUID.class)
            )
        ).thenReturn("refresh-token");

        // When
        TokenResponseDTO result = authService.register(registerDto);

        // Then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");

        verify(userRepository).save(user);
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    @DisplayName("Should throw when username already exists")
    void register_shouldThrowWhenUsernameExists() {
        // Given
        when(
            userRepository.existsByUsername(registerDto.username())
        ).thenReturn(true);

        // When / Then
        assertThatThrownBy(() ->
            authService.register(registerDto)
        ).isInstanceOf(ResourceAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(tokenRepository);
    }

    @Test
    @DisplayName("Should throw when email already exists")
    void register_shouldThrowWhenEmailExists() {
        // Given
        when(userRepository.existsByEmail(registerDto.email())).thenReturn(
            true
        );

        // When / Then
        assertThatThrownBy(() ->
            authService.register(registerDto)
        ).isInstanceOf(ResourceAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(tokenRepository);
    }

    @Test
    @DisplayName("Should throw when user is not found during login")
    void login_shouldThrowWhenUserNotFound() {
        // Given
        when(authenticationManager.authenticate(any())).thenReturn(
            mock(Authentication.class)
        );

        when(userRepository.findByEmail(anyString())).thenReturn(
            Optional.empty()
        );

        var dto = new LoginRequestDTO("test@test.com", "password");

        // When / Then
        assertThatThrownBy(() -> authService.login(dto)).isInstanceOf(
            ResourceNotFoundException.class
        );
    }

    @Test
    @DisplayName("Should login successfully")
    void login_shouldLoginSuccessfully() {
        // Given
        when(authenticationManager.authenticate(any())).thenReturn(
            mock(Authentication.class)
        );

        when(userRepository.findByEmail(anyString())).thenReturn(
            Optional.of(savedUser)
        );

        when(tokenRepository.save(any(Token.class))).thenReturn(token);

        when(jwtService.getRefreshTokenExpirationInstant()).thenReturn(
            Instant.now().plusSeconds(86400)
        );

        when(jwtService.generateToken(any(UserDetailsImpl.class))).thenReturn(
            "access-token"
        );

        when(
            jwtService.generateRefreshToken(
                any(UserDetailsImpl.class),
                eq(token.getJti()),
                any(UUID.class)
            )
        ).thenReturn("refresh-token");

        // When
        TokenResponseDTO result = authService.login(
            new LoginRequestDTO("shoan@test.com", "password")
        );

        // Then
        assertThat(result.accessToken()).isEqualTo("access-token");

        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    @DisplayName("Should throw when refresh header is invalid")
    void refresh_shouldThrowWhenHeaderInvalid() {
        // When / Then
        assertThatThrownBy(() ->
            authService.refreshToken("invalid")
        ).isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> authService.refreshToken(null)).isInstanceOf(
            BadRequestException.class
        );

        verifyNoInteractions(tokenRepository);
    }

    @Test
    @DisplayName("Should throw when refresh token was reused")
    void refresh_shouldThrowWhenTokenReused() {
        // Given
        String rawToken = "token";

        UUID jti = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();

        when(jwtService.extractJti(rawToken)).thenReturn(jti);

        when(jwtService.extractFamilyId(rawToken)).thenReturn(familyId);

        when(jwtService.extractUsername(rawToken)).thenReturn("test@test.com");

        when(userRepository.findByEmail(anyString())).thenReturn(
            Optional.of(user)
        );

        Token dbToken = Token.builder().jti(jti).familyId(familyId).build();

        dbToken.setRevoked(true);

        when(tokenRepository.findById(jti)).thenReturn(Optional.of(dbToken));

        // When / Then
        assertThatThrownBy(() -> authService.refreshToken("Bearer " + rawToken))
            .isInstanceOf(InvalidJwtException.class)
            .hasMessage("Refresh token reuse detected. Family revoked");

        verify(tokenRepository).revokeByFamily(familyId);
    }

    @Test
    @DisplayName("Should refresh tokens successfully")
    void refresh_shouldRefreshSuccessfully() {
        // Given
        String rawToken = "token";

        UUID jti = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();

        when(jwtService.extractJti(rawToken)).thenReturn(jti);

        when(jwtService.extractFamilyId(rawToken)).thenReturn(familyId);

        when(jwtService.extractUsername(rawToken)).thenReturn("test@test.com");

        when(userRepository.findByEmail(anyString())).thenReturn(
            Optional.of(user)
        );

        when(tokenRepository.findById(jti)).thenReturn(Optional.of(token));

        when(tokenRepository.save(any(Token.class))).thenReturn(token);

        when(jwtService.getRefreshTokenExpirationInstant()).thenReturn(
            Instant.now().plusSeconds(86400)
        );

        when(jwtService.generateToken(any(UserDetailsImpl.class))).thenReturn(
            "access-token"
        );

        when(
            jwtService.generateRefreshToken(
                any(UserDetailsImpl.class),
                eq(token.getJti()),
                any(UUID.class)
            )
        ).thenReturn("refresh-token");

        // When
        TokenResponseDTO result = authService.refreshToken(
            "Bearer " + rawToken
        );

        // Then
        assertThat(result.accessToken()).isEqualTo("access-token");

        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    @DisplayName("Should revoke token family on logout")
    void logout_shouldRevokeFamily() {
        // Given
        String rawToken = "token";
        UUID familyId = UUID.randomUUID();

        when(jwtService.extractUsername(rawToken)).thenReturn("test@test.com");

        when(userRepository.findByEmail("test@test.com")).thenReturn(
            Optional.of(user)
        );

        when(jwtService.extractFamilyId(rawToken)).thenReturn(familyId);

        // When
        authService.logout("Bearer " + rawToken);

        // Then
        verify(tokenRepository).revokeByFamily(familyId);
    }
}
