package dev.genesshoan.fitnesstrackerapi.user.application.service;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import dev.genesshoan.fitnesstrackerapi.user.application.exception.UserRegistrationException;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.command.RegisterUserCommand;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.PendingVerificationUser;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.RefreshUserData;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.UserCredentials;
import dev.genesshoan.fitnesstrackerapi.user.domain.Role;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.infrastructure.persistence.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.infrastructure.web.UserMapper;
import dev.genesshoan.fitnesstrackerapi.user.infrastructure.web.dto.ChangeUsernameRequest;
import dev.genesshoan.fitnesstrackerapi.user.infrastructure.web.dto.UserResponse;
import net.datafaker.Faker;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Faker FAKER = new Faker();
    private static final UUID USER_ID = UUID.fromString("01932f4a-1234-7000-8000-123456789abc");
    private static final String EMAIL = "shoan@test.com";
    private static final String USERNAME = "shoan";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProfilePictureService profilePictureService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = UserBuilder.aUser(FAKER)
                .withId(USER_ID)
                .withUsername(USERNAME)
                .withEmail(EMAIL)
                .build();
    }

    @Test
    @DisplayName("Should return the profile with the profile picture url")
    void getProfile_shouldReturnProfile() {
        // Given
        UserResponse mapped = new UserResponse(EMAIL, USERNAME, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(mapped);
        when(profilePictureService.getProfilePictureUrl(null)).thenReturn(null);

        // When
        UserResponse result = userService.getProfile(USER_ID);

        // Then
        assertThat(result).isEqualTo(mapped);
        verify(profilePictureService).getProfilePictureUrl(null);
    }

    @Test
    @DisplayName("Should resolve the profile picture url from the stored key")
    void getProfile_shouldResolveProfilePictureUrlFromStoredKey() {
        // Given
        String key = "profile-pictures/" + USER_ID + ".jpg";
        user.setProfilePictureKey(key);
        UserResponse mapped = new UserResponse(EMAIL, USERNAME, null);
        UserResponse expected = new UserResponse(EMAIL, USERNAME, "https://storage/picture.jpg");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(mapped);
        when(profilePictureService.getProfilePictureUrl(key)).thenReturn(expected.profilePictureUrl());

        // When
        UserResponse result = userService.getProfile(USER_ID);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(profilePictureService).getProfilePictureUrl(key);
    }

    @Test
    @DisplayName("Should throw when the user does not exist")
    void getProfile_shouldThrowWhenUserNotFound() {
        // Given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.getProfile(USER_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should change the username")
    void changeUsername_shouldChangeUsername() {
        // Given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // When
        userService.changeUsername(USER_ID, new ChangeUsernameRequest("newUsername"));

        // Then
        assertThat(user.getUsername()).isEqualTo("newUsername");
    }

    @Test
    @DisplayName("Should throw when the new username equals the current one")
    void changeUsername_shouldThrowWhenSameUsername() {
        // Given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // When / Then
        assertThatThrownBy(() -> userService.changeUsername(USER_ID, new ChangeUsernameRequest(USERNAME)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw when the user does not exist")
    void changeUsername_shouldThrowWhenUserNotFound() {
        // Given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.changeUsername(USER_ID, new ChangeUsernameRequest("newUsername")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should find the credentials by email")
    void findByEmail_shouldDelegateToRepository() {
        // Given
        UserCredentials credentials = new UserCredentials(USER_ID, EMAIL, USERNAME, "hash", true, Role.USER);
        when(userRepository.findCredentialsByEmail(EMAIL)).thenReturn(Optional.of(credentials));

        // When / Then
        assertThat(userService.findByEmail(EMAIL)).contains(credentials);
    }

    @Test
    @DisplayName("Should find the credentials by identifier")
    void findByIdentifier_shouldDelegateToRepository() {
        // Given
        UserCredentials credentials = new UserCredentials(USER_ID, EMAIL, USERNAME, "hash", true, Role.USER);
        when(userRepository.findCredentialsByIdentifier(USERNAME)).thenReturn(Optional.of(credentials));

        // When / Then
        assertThat(userService.findByIdentifier(USERNAME)).contains(credentials);
    }

    @Test
    @DisplayName("Should find the credentials by id")
    void findById_shouldDelegateToRepository() {
        // Given
        UserCredentials credentials = new UserCredentials(USER_ID, EMAIL, USERNAME, "hash", true, Role.USER);
        when(userRepository.findCredentialsById(USER_ID)).thenReturn(Optional.of(credentials));

        // When / Then
        assertThat(userService.findById(USER_ID)).contains(credentials);
    }

    @Test
    @DisplayName("Should find the refresh data by id")
    void findRefreshDataById_shouldDelegateToRepository() {
        // Given
        RefreshUserData refreshData = new RefreshUserData(Role.USER);
        when(userRepository.findRefreshUserDataById(USER_ID)).thenReturn(Optional.of(refreshData));

        // When / Then
        assertThat(userService.findRefreshDataById(USER_ID)).contains(refreshData);
    }

    @Test
    @DisplayName("Should register the user and return its id")
    void register_shouldReturnUserId() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(EMAIL, USERNAME, "UTC", "encoded");
        when(userMapper.toEntity(command)).thenReturn(user);
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        // When
        UUID userId = userService.register(command);

        // Then
        assertThat(userId).isEqualTo(USER_ID);
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    @DisplayName("Should throw a conflict when the email is already registered")
    void register_shouldThrowConflictWhenEmailExists() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(EMAIL, USERNAME, "UTC", "encoded");
        when(userMapper.toEntity(command)).thenReturn(user);
        when(userRepository.saveAndFlush(user)).thenThrow(dataIntegrityViolation("uk_user_email"));

        // When / Then
        assertThatThrownBy(() -> userService.register(command))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("Should throw a conflict when the username is already registered")
    void register_shouldThrowConflictWhenUsernameExists() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(EMAIL, USERNAME, "UTC", "encoded");
        when(userMapper.toEntity(command)).thenReturn(user);
        when(userRepository.saveAndFlush(user)).thenThrow(dataIntegrityViolation("uk_user_username"));

        // When / Then
        assertThatThrownBy(() -> userService.register(command))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("username");
    }

    @Test
    @DisplayName("Should throw when the registration fails for another reason")
    void register_shouldThrowWhenRegistrationFails() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(EMAIL, USERNAME, "UTC", "encoded");
        when(userMapper.toEntity(command)).thenReturn(user);
        when(userRepository.saveAndFlush(user)).thenThrow(dataIntegrityViolation("other_constraint"));

        // When / Then
        assertThatThrownBy(() -> userService.register(command)).isInstanceOf(UserRegistrationException.class);
    }

    @Test
    @DisplayName("Should update the password hash")
    void updatePasswordHash_shouldUpdateHash() {
        // Given
        when(userRepository.updatePasswordHash(USER_ID, "newHash")).thenReturn(1);

        // When
        userService.updatePasswordHash(USER_ID, "newHash");

        // Then
        verify(userRepository).updatePasswordHash(USER_ID, "newHash");
    }

    @Test
    @DisplayName("Should throw when the user does not exist while updating the password hash")
    void updatePasswordHash_shouldThrowWhenUserNotFound() {
        // Given
        when(userRepository.updatePasswordHash(USER_ID, "newHash")).thenReturn(0);

        // When / Then
        assertThatThrownBy(() -> userService.updatePasswordHash(USER_ID, "newHash"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should find the unverified user by email")
    void findUnverifiedByEmail_shouldDelegateToRepository() {
        // Given
        PendingVerificationUser pending = new PendingVerificationUser(USER_ID, USERNAME);
        when(userRepository.findUnverifiedByEmail(EMAIL)).thenReturn(Optional.of(pending));

        // When / Then
        assertThat(userService.findUnverifiedByEmail(EMAIL)).contains(pending);
    }

    @Test
    @DisplayName("Should mark the email as verified")
    void markEmailVerified_shouldReturnTrueWhenUpdated() {
        // Given
        when(userRepository.markEmailVerified(USER_ID)).thenReturn(1);

        // When / Then
        assertThat(userService.markEmailVerified(USER_ID)).isTrue();
    }

    @Test
    @DisplayName("Should return false when the email could not be marked as verified")
    void markEmailVerified_shouldReturnFalseWhenNotUpdated() {
        // Given
        when(userRepository.markEmailVerified(USER_ID)).thenReturn(0);

        // When / Then
        assertThat(userService.markEmailVerified(USER_ID)).isFalse();
    }

    @Test
    @DisplayName("Should return the timezone of the user")
    void getUserTimezoneById_shouldReturnZoneId() {
        // Given
        when(userRepository.findTimezoneById(USER_ID)).thenReturn(Optional.of("America/Sao_Paulo"));

        // When / Then
        assertThat(userService.getUserTimezoneById(USER_ID)).contains(ZoneId.of("America/Sao_Paulo"));
    }

    @Test
    @DisplayName("Should return empty when the user has no timezone")
    void getUserTimezoneById_shouldReturnEmptyWhenMissing() {
        // Given
        when(userRepository.findTimezoneById(USER_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThat(userService.getUserTimezoneById(USER_ID)).isEmpty();
        verifyNoInteractions(userMapper);
    }

    private DataIntegrityViolationException dataIntegrityViolation(String constraintName) {
        return new DataIntegrityViolationException(
                "constraint violation", new ConstraintViolationException("constraint violation", null, constraintName));
    }
}
