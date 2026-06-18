
package dev.genesshoan.fitnesstrackerapi;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidCredentialsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.user.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.UserService;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.dto.ChangePasswordRequestDTO;
import dev.genesshoan.fitnesstrackerapi.user.dto.ChangeUsernameRequestDTO;
import dev.genesshoan.fitnesstrackerapi.user.dto.UserResponseDTO;
import dev.genesshoan.fitnesstrackerapi.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  @Test
  void getProfile_ShouldReturnProfile() {

    User user = User.builder()
        .id(1L)
        .build();

    UserResponseDTO dto = new UserResponseDTO("test@test.com", "tester");

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));

    when(userMapper.toResponseDTO(user))
        .thenReturn(dto);

    UserResponseDTO result = userService.getProfile(1L);

    assertThat(result).isEqualTo(dto);

    verify(userRepository).findById(1L);
    verify(userMapper).toResponseDTO(user);
  }

  @Test
  void getProfile_ShouldThrowException_WhenUserNotFound() {

    when(userRepository.findById(1L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getProfile(1L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void changePassword_ShouldThrowException_WhenUserNotFound() {

    ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("oldPassword", "newPassword");

    when(userRepository.findById(1L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.changePassword(1L, dto))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void changePassword_ShouldChangePasswordSuccessfully() {

    User user = User.builder()
        .id(1L)
        .email("test@test.com")
        .passwordHash("oldHash")
        .build();

    ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("oldPassword", "newPassword");

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));

    when(passwordEncoder.matches("oldPassword", "oldHash"))
        .thenReturn(true);

    when(passwordEncoder.matches("newPassword", "oldHash"))
        .thenReturn(false);

    when(passwordEncoder.encode("newPassword"))
        .thenReturn("newHash");

    userService.changePassword(1L, dto);

    assertThat(user.getPasswordHash())
        .isEqualTo("newHash");

    verify(passwordEncoder).encode("newPassword");
  }

  @Test
  void changePassword_ShouldThrowException_WhenWrongPassword() {

    User user = User.builder()
        .id(1L)
        .email("test@test.com")
        .passwordHash("oldHash")
        .build();

    ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("oldPassword", "newPassword");

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));

    when(passwordEncoder.matches("oldPassword", "oldHash"))
        .thenReturn(false);

    assertThatThrownBy(() -> userService.changePassword(1L, dto))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void changePassword_ShouldThrowException_WhenSamePassword() {

    User user = User.builder()
        .id(1L)
        .email("test@test.com")
        .passwordHash("oldHash")
        .build();

    ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("oldPassword", "newPassword");

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));

    when(passwordEncoder.matches("oldPassword", "oldHash"))
        .thenReturn(true);

    when(passwordEncoder.matches("newPassword", "oldHash"))
        .thenReturn(true);

    assertThatThrownBy(() -> userService.changePassword(1L, dto))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void changeUsername_ShouldChangeUsernameSuccessfully() {

    User user = User.builder()
        .id(1L)
        .username("oldUsername")
        .build();

    ChangeUsernameRequestDTO dto = new ChangeUsernameRequestDTO("newUsername");

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));

    userService.changeUsername(1L, dto);

    assertThat(user.getUsername())
        .isEqualTo("newUsername");
  }

  @Test
  void changeUsername_ShouldThrowException_WhenSameUsername() {

    User user = User.builder()
        .id(1L)
        .username("oldUsername")
        .build();

    ChangeUsernameRequestDTO dto = new ChangeUsernameRequestDTO("oldUsername");

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));

    assertThatThrownBy(() -> userService.changeUsername(1L, dto))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void changeUsername_ShouldThrowException_WhenUserNotFound() {

    ChangeUsernameRequestDTO dto = new ChangeUsernameRequestDTO("newUsername");

    when(userRepository.findById(1L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.changeUsername(1L, dto))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
