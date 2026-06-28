package dev.genesshoan.fitnesstrackerapi.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.f4b6a3.uuid.UuidCreator;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidCredentialsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import dev.genesshoan.fitnesstrackerapi.user.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.UserService;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.dto.ChangePasswordRequestDTO;
import dev.genesshoan.fitnesstrackerapi.user.dto.ChangeUsernameRequestDTO;
import dev.genesshoan.fitnesstrackerapi.user.dto.UserResponseDTO;
import dev.genesshoan.fitnesstrackerapi.user.mapper.UserMapper;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Faker FAKER = new Faker();
    private static final UUID USER_ID = UUID.fromString(
        "01932f4a-1234-7000-8000-123456789abc"
    );

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = UserBuilder.aUser(FAKER).withId(USER_ID).build();
    }

    @Test
    void getProfile_ShouldReturnProfile() {
        UserResponseDTO dto = new UserResponseDTO(
            user.getEmail(),
            user.getUsername()
        );

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(dto);

        UserResponseDTO result = userService.getProfile(USER_ID);

        assertThat(result).isEqualTo(dto);
        verify(userRepository).findById(USER_ID);
        verify(userMapper).toResponseDTO(user);
    }

    @Test
    void getProfile_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(USER_ID)).isInstanceOf(
            ResourceNotFoundException.class
        );
    }

    @Test
    void changePassword_ShouldThrowException_WhenUserNotFound() {
        var dto = new ChangePasswordRequestDTO("oldPassword", "newPassword");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            userService.changePassword(USER_ID, dto)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePassword_ShouldChangePasswordSuccessfully() {
        var userWithHash = UserBuilder.aUser(FAKER)
            .withId(USER_ID)
            .withPasswordHash("oldHash")
            .build();

        var dto = new ChangePasswordRequestDTO("oldPassword", "newPassword");

        when(userRepository.findById(USER_ID)).thenReturn(
            Optional.of(userWithHash)
        );
        when(passwordEncoder.matches("oldPassword", "oldHash")).thenReturn(
            true
        );
        when(passwordEncoder.matches("newPassword", "oldHash")).thenReturn(
            false
        );
        when(passwordEncoder.encode("newPassword")).thenReturn("newHash");

        userService.changePassword(USER_ID, dto);

        assertThat(userWithHash.getPasswordHash()).isEqualTo("newHash");
        verify(passwordEncoder).encode("newPassword");
    }

    @Test
    void changePassword_ShouldThrowException_WhenWrongPassword() {
        var userWithHash = UserBuilder.aUser(FAKER)
            .withId(USER_ID)
            .withPasswordHash("oldHash")
            .build();

        var dto = new ChangePasswordRequestDTO("oldPassword", "newPassword");

        when(userRepository.findById(USER_ID)).thenReturn(
            Optional.of(userWithHash)
        );
        when(passwordEncoder.matches("oldPassword", "oldHash")).thenReturn(
            false
        );

        assertThatThrownBy(() ->
            userService.changePassword(USER_ID, dto)
        ).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void changePassword_ShouldThrowException_WhenSamePassword() {
        var userWithHash = UserBuilder.aUser(FAKER)
            .withId(USER_ID)
            .withPasswordHash("oldHash")
            .build();

        var dto = new ChangePasswordRequestDTO("oldPassword", "newPassword");

        when(userRepository.findById(USER_ID)).thenReturn(
            Optional.of(userWithHash)
        );
        when(passwordEncoder.matches("oldPassword", "oldHash")).thenReturn(
            true
        );
        when(passwordEncoder.matches("newPassword", "oldHash")).thenReturn(
            true
        );

        assertThatThrownBy(() ->
            userService.changePassword(USER_ID, dto)
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void changeUsername_ShouldChangeUsernameSuccessfully() {
        var userWithUsername = UserBuilder.aUser(FAKER)
            .withId(USER_ID)
            .withUsername("oldUsername")
            .build();

        var dto = new ChangeUsernameRequestDTO("newUsername");

        when(userRepository.findById(USER_ID)).thenReturn(
            Optional.of(userWithUsername)
        );

        userService.changeUsername(USER_ID, dto);

        assertThat(userWithUsername.getUsername()).isEqualTo("newUsername");
    }

    @Test
    void changeUsername_ShouldThrowException_WhenSameUsername() {
        var userWithUsername = UserBuilder.aUser(FAKER)
            .withId(USER_ID)
            .withUsername("oldUsername")
            .build();

        var dto = new ChangeUsernameRequestDTO("oldUsername");

        when(userRepository.findById(USER_ID)).thenReturn(
            Optional.of(userWithUsername)
        );

        assertThatThrownBy(() ->
            userService.changeUsername(USER_ID, dto)
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void changeUsername_ShouldThrowException_WhenUserNotFound() {
        var dto = new ChangeUsernameRequestDTO("newUsername");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            userService.changeUsername(USER_ID, dto)
        ).isInstanceOf(ResourceNotFoundException.class);
    }
}
