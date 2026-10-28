package dev.genesshoan.fitnesstrackerapi.user.application.service;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.user.application.exception.UserRegistrationException;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetRefreshUserDataUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetUserCredentialsUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetUserTimeZoneUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.RegisterUserUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.UpdatePasswordHashUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.VerifyUserEmailUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.command.RegisterUserCommand;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.PendingVerificationUser;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.RefreshUserData;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.UserCredentials;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.infrastructure.persistence.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.infrastructure.web.UserMapper;
import dev.genesshoan.fitnesstrackerapi.user.infrastructure.web.dto.ChangeUsernameRequest;
import dev.genesshoan.fitnesstrackerapi.user.infrastructure.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService
        implements GetRefreshUserDataUseCase,
                GetUserCredentialsUseCase,
                RegisterUserUseCase,
                UpdatePasswordHashUseCase,
                VerifyUserEmailUseCase,
                GetUserTimeZoneUseCase {

    private static final String UK_USER_EMAIL = "uk_user_email";
    private static final String UK_USER_USERNAME = "uk_user_username";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ProfilePictureService profilePictureService;

    /**
     * Gets the user profile.
     *
     * @param id the user id
     * @return a {@link UserResponse} containing user's profile information
     * @throws ResourceNotFoundException if the user does not exist
     */
    public UserResponse getProfile(UUID id) {
        var user = findUserById(id);
        var profile = userMapper.toResponseDTO(user);
        return new UserResponse(
                profile.email(),
                profile.username(),
                profile.createdAt(),
                profilePictureService.getProfilePictureUrl(user.getProfilePictureKey()));
    }

    /**
     * Changes the user's username.
     *
     * @param id  the user id
     * @param dto the username change request
     * @throws ResourceNotFoundException if the user does not exist
     * @throws BadRequestException       if the new username matches the current
     *                                   username
     */
    @Transactional
    public void changeUsername(UUID id, ChangeUsernameRequest dto) {
        var user = findUserById(id);

        if (dto.newUsername().equals(user.getUsername())) {
            throw new BadRequestException("New username must be different from current one");
        }

        user.setUsername(dto.newUsername());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserCredentials> findByEmail(String email) {
        return userRepository.findCredentialsByEmail(email);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserCredentials> findByIdentifier(String identifier) {
        return userRepository.findCredentialsByIdentifier(identifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserCredentials> findById(UUID userId) {
        return userRepository.findCredentialsById(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RefreshUserData> findRefreshDataById(UUID id) {
        return userRepository.findRefreshUserDataById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UUID register(RegisterUserCommand command) {
        var user = userMapper.toEntity(command);
        UUID userId;

        try {
            userId = userRepository.saveAndFlush(user).getId();
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException cve) {
                String constraint = cve.getConstraintName();
                if (UK_USER_EMAIL.equalsIgnoreCase(constraint)) {
                    throw new ResourceAlreadyExistsException("An account with this email already exists", e);
                }
                if (UK_USER_USERNAME.equalsIgnoreCase(constraint)) {
                    throw new ResourceAlreadyExistsException("An account with this username already exists", e);
                }
            }
            throw new UserRegistrationException("Unable to register user", e);
        }

        return userId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updatePasswordHash(UUID userId, String newHash) {
        int updated = userRepository.updatePasswordHash(userId, newHash);

        if (updated == 0) {
            throw new ResourceNotFoundException("User not found");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PendingVerificationUser> findUnverifiedByEmail(String email) {
        return userRepository.findUnverifiedByEmail(email);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public boolean markEmailVerified(UUID id) {
        if (userRepository.markEmailVerified(id) != 1) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ZoneId> getUserTimezoneById(UUID userId) {
        return userRepository.findTimezoneById(userId).map(ZoneId::of);
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> {
            log.warn("User not found for id {}", id);
            return new ResourceNotFoundException("User not found");
        });
    }
}
