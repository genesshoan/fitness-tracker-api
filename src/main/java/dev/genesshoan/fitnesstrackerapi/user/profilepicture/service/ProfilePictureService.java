package dev.genesshoan.fitnesstrackerapi.user.profilepicture.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidProfilePictureException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ProfilePictureProcessingException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.user.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.common.storage.ObjectStoragePort;
import dev.genesshoan.fitnesstrackerapi.user.profilepicture.processor.ImageProcessor;
import dev.genesshoan.fitnesstrackerapi.user.profilepicture.processor.ProcessedImage;
import dev.genesshoan.fitnesstrackerapi.user.profilepicture.usecase.ProfilePictureUseCase;
import dev.genesshoan.fitnesstrackerapi.user.profilepicture.validator.ProfilePictureValidator;
import lombok.RequiredArgsConstructor;

/**
 * Application service that validates, processes, and stores profile pictures.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProfilePictureService implements ProfilePictureUseCase {

    private final UserRepository userRepository;
    private final ProfilePictureValidator validator;
    private final ImageProcessor imageProcessor;
    private final ObjectStoragePort objectStorage;

    /** {@inheritDoc} */
    @Override
    public String uploadProfilePicture(UUID userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            byte[] original = file.getBytes();
            Supplier<java.io.InputStream> content = () -> new ByteArrayInputStream(original);
            validator.validate(content, file.getSize());

            ProcessedImage processed = imageProcessor.process(content.get());
            String key = profilePictureKey(user);
            objectStorage.upload(key, processed.inputStream(), processed.size(), processed.contentType());
            user.setProfilePictureKey(key);

            return objectStorage.getPresignedUrl(key);
        } catch (InvalidProfilePictureException | IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            throw new ProfilePictureProcessingException("Failed to read uploaded profile picture", e);
        } catch (Exception e) {
            throw new ProfilePictureProcessingException("Failed to upload profile picture", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getProfilePictureUrl(String profilePictureKey) {
        return profilePictureKey == null ? null : objectStorage.getPresignedUrl(profilePictureKey);
    }

    private String profilePictureKey(User user) {
        if (user.getProfilePictureKey() == null) {
            return "profile-pictures/" + user.getId() + ".jpg";
        }
        return user.getProfilePictureKey();
    }
}