package dev.genesshoan.fitnesstrackerapi.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.f4b6a3.uuid.UuidCreator;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidCredentialsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.dto.ChangePasswordRequestDTO;
import dev.genesshoan.fitnesstrackerapi.user.dto.ChangeUsernameRequestDTO;
import dev.genesshoan.fitnesstrackerapi.user.dto.UserResponseDTO;
import dev.genesshoan.fitnesstrackerapi.user.mapper.UserMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private static final UUID userId = UUID.fromString(
        "01932f4a-1234-7000-8000-123456789abc"
    );

    @Test
    void getProfile_ShouldReturnProfile() {
        User user = User.builder()
            .id(UuidCreator.getTimeOrderedEpoch())
            .build();

        UserResponseDTO dto = new UserResponseDTO("test@test.com", "tester");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(userMapper.toResponseDTO(user)).thenReturn(dto);

        UserResponseDTO result = userService.getProfile(userId);

        assertThat(result).isEqualTo(dto);

        verify(userRepository).findById(userId);
        verify(userMapper).toResponseDTO(user);
    }

    @Test
    void getProfile_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(userId)).isInstanceOf(
            ResourceNotFoundException.class
        );
    }

    @Test
    void changePassword_ShouldThrowException_WhenUserNotFound() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO(
            "oldPassword",
            "newPassword"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            userService.changePassword(userId, dto)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePassword_ShouldChangePasswordSuccessfully() {
        User user = User.builder()
            .id(userId)
            .email("test@test.com")
            .passwordHash("oldHash")
            .build();

        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO(
            "oldPassword",
            "newPassword"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(passwordEncoder.matches("oldPassword", "oldHash")).thenReturn(
            true
        );

        when(passwordEncoder.matches("newPassword", "oldHash")).thenReturn(
            false
        );

        when(passwordEncoder.encode("newPassword")).thenReturn("newHash");

        userService.changePassword(userId, dto);

        assertThat(user.getPasswordHash()).isEqualTo("newHash");

        verify(passwordEncoder).encode("newPassword");
    }

    @Test
    void changePassword_ShouldThrowException_WhenWrongPassword() {
        User user = User.builder()
            .id(userId)
            .email("test@test.com")
            .passwordHash("oldHash")
            .build();

        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO(
            "oldPassword",
            "newPassword"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(passwordEncoder.matches("oldPassword", "oldHash")).thenReturn(
            false
        );

        assertThatThrownBy(() ->
            userService.changePassword(userId, dto)
        ).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void changePassword_ShouldThrowException_WhenSamePassword() {
        User user = User.builder()
            .id(userId)
            .email("test@test.com")
            .passwordHash("oldHash")
            .build();

        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO(
            "oldPassword",
            "newPassword"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(passwordEncoder.matches("oldPassword", "oldHash")).thenReturn(
            true
        );

        when(passwordEncoder.matches("newPassword", "oldHash")).thenReturn(
            true
        );

        assertThatThrownBy(() ->
            userService.changePassword(userId, dto)
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void changeUsername_ShouldChangeUsernameSuccessfully() {
        User user = User.builder().id(userId).username("oldUsername").build();

        ChangeUsernameRequestDTO dto = new ChangeUsernameRequestDTO(
            "newUsername"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.changeUsername(userId, dto);

        assertThat(user.getUsername()).isEqualTo("newUsername");
    }

    @Test
    void changeUsername_ShouldThrowException_WhenSameUsername() {
        User user = User.builder().id(userId).username("oldUsername").build();

        ChangeUsernameRequestDTO dto = new ChangeUsernameRequestDTO(
            "oldUsername"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
            userService.changeUsername(userId, dto)
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void changeUsername_ShouldThrowException_WhenUserNotFound() {
        ChangeUsernameRequestDTO dto = new ChangeUsernameRequestDTO(
            "newUsername"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            userService.changeUsername(userId, dto)
        ).isInstanceOf(ResourceNotFoundException.class);
    }
}
