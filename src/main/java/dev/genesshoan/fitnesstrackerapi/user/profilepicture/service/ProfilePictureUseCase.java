package dev.genesshoan.fitnesstrackerapi.user.profilepicture.service;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ProfilePictureUseCase {

    String uploadProfilePicture(UUID userId, MultipartFile file);

    String getProfilePictureUrl(String profilePictureKey);
}
