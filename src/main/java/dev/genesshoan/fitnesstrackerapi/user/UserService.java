package dev.genesshoan.fitnesstrackerapi.user;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidCredentialsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.dto.ChangePasswordRequestDTO;
import dev.genesshoan.fitnesstrackerapi.user.dto.ChangeUsernameRequestDTO;
import dev.genesshoan.fitnesstrackerapi.user.dto.UserResponseDTO;
import dev.genesshoan.fitnesstrackerapi.user.mapper.UserMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     *
     * Gets the user profile.
     *
     * @param id the user id
     * @return a {@link UserResponseDTO} containing user's profile information
     * @throws ResourceNotFoundException if the user does not exist
     */
    public UserResponseDTO getProfile(UUID id) {
        return userMapper.toResponseDTO(findUserById(id));
    }

    /**
     *
     * Changes the user's password.
     *
     * @param id  the user id
     * @param dto the password change request
     * @throws ResourceNotFoundException   if the user does not exist
     * @throws InvalidCredentialsException if the provided password is incorrect
     * @throws BadRequestException         if the new password matches the current
     *                                     password
     */
    @Transactional
    public void changePassword(UUID id, ChangePasswordRequestDTO dto) {
        var user = findUserById(id);

        if (
            !passwordEncoder.matches(dto.oldPassword(), user.getPasswordHash())
        ) {
            throw new InvalidCredentialsException(
                "Provided password does not match current one"
            );
        }

        if (
            passwordEncoder.matches(dto.newPassword(), user.getPasswordHash())
        ) {
            throw new BadRequestException(
                "New password must be different from current one"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
    }

    /**
     *
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
            throw new BadRequestException(
                "New username must be different from current one"
            );
        }

        user.setUsername(dto.newUsername());
    }

    /**
     *
     * Finds a user by id.
     *
     * @param id the user id
     * @return the user
     * @throws ResourceNotFoundException if the user does not exist
     */
    private User findUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> {
            log.warn("User not found for id {}", id);
            return new ResourceNotFoundException("User not found");
        });
    }
}
