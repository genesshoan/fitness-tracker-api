package dev.genesshoan.fitnesstrackerapi.user.profilepicture.usecase;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * Entry port for profile picture operations exposed by the user module.
 */
public interface ProfilePictureUseCase {

    /**
     * Uploads or replaces the authenticated user's profile picture.
     *
     * @param userId authenticated user identifier
     * @param file uploaded image
     * @return temporary URL for the stored image
     */
    String uploadProfilePicture(UUID userId, MultipartFile file);

    /**
     * Resolves a temporary URL for a stored profile picture.
     *
     * @param profilePictureKey object-storage key, or {@code null}
     * @return temporary URL, or {@code null} when no picture exists
     */
    String getProfilePictureUrl(String profilePictureKey);
}
