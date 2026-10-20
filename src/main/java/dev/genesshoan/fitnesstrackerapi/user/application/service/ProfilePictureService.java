package dev.genesshoan.fitnesstrackerapi.user.application.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.storage.ObjectStoragePort;
import dev.genesshoan.fitnesstrackerapi.user.application.exception.ProfilePictureProcessingException;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Application service that validates, processes, and stores profile pictures.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProfilePictureService {

    private final UserRepository userRepository;
    private final ProfilePictureValidator validator;
    private final ProfilePictureProcessor profilePictureProcessor;
    private final ObjectStoragePort objectStorage;

    /**
     * Uploads or replaces the authenticated user's profile picture.
     *
     * @param userId authenticated user identifier
     * @param file uploaded image
     * @return temporary URL for the stored image
     */
    public String uploadProfilePicture(UUID userId, MultipartFile file) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new ProfilePictureProcessingException("Failed to read uploaded profile picture", e);
        }

        Supplier<InputStream> content = () -> new ByteArrayInputStream(original);
        validator.validate(content, file.getSize()); // lanza InvalidProfilePictureException

        ProcessedImage processed = profilePictureProcessor.process(content.get());
        String key = profilePictureKey(user);
        objectStorage.upload(key, processed.inputStream(), processed.size(), processed.contentType());
        user.setProfilePictureKey(key);

        return objectStorage.getPresignedUrl(key);
    }

    /**
     * Resolves a temporary URL for a stored profile picture.
     *
     * @param profilePictureKey object-storage key, or {@code null}
     * @return temporary URL, or {@code null} when no picture exists
     */
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
