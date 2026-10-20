package dev.genesshoan.fitnesstrackerapi.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetRefreshUserDataUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetUserCredentialsUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.RegisterUserUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.UpdatePasswordHashUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.VerifyUserEmailUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.command.RegisterUserCommand;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.PendingVerificationUser;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.RefreshUserData;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.UserCredentials;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.dto.ChangeUsernameRequestDTO;
import dev.genesshoan.fitnesstrackerapi.user.dto.UserResponseDTO;
import dev.genesshoan.fitnesstrackerapi.user.mapper.UserMapper;
import dev.genesshoan.fitnesstrackerapi.user.profilepicture.usecase.ProfilePictureUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService
        implements GetRefreshUserDataUseCase,
                GetUserCredentialsUseCase,
                RegisterUserUseCase,
                UpdatePasswordHashUseCase,
                VerifyUserEmailUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ProfilePictureUseCase profilePictureUseCase;

    /**
     * Gets the user profile.
     *
     * @param id the user id
     * @return a {@link UserResponseDTO} containing user's profile information
     * @throws ResourceNotFoundException if the user does not exist
     */
    public UserResponseDTO getProfile(UUID id) {
        var user = findUserById(id);
        var profile = userMapper.toResponseDTO(user);
        return new UserResponseDTO(
                profile.email(),
                profile.username(),
                profilePictureUseCase.getProfilePictureUrl(user.getProfilePictureKey()));
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
    public void changeUsername(UUID id, ChangeUsernameRequestDTO dto) {
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
            Throwable cause = e.getMostSpecificCause();

            if (cause instanceof PSQLException ex) {
                var constraint = ex.getServerErrorMessage() != null
                        ? ex.getServerErrorMessage().getConstraint()
                        : null;

                if (constraint != null) {
                    if (constraint.equals("uk_user_email")) {
                        throw new ResourceAlreadyExistsException("An account with this email already exists", e);
                    }

                    if (constraint.equals("uk_user_username")) {
                        throw new ResourceAlreadyExistsException("An account with this username already exists", e);
                    }
                }
            }

            throw new ResourceAlreadyExistsException("Unable to register user", e);
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

    private User findUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> {
            log.warn("User not found for id {}", id);
            return new ResourceNotFoundException("User not found");
        });
    }
}
