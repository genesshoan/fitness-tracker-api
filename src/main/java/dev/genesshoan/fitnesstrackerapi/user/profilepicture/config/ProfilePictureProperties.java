package dev.genesshoan.fitnesstrackerapi.user.profilepicture.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for profile picture upload.
 *
 * <p>These properties control the size, format, and quality constraints
 * for uploaded profile pictures.
 */
@ConfigurationProperties(prefix = "app.file.upload.profile-picture")
@Validated
public record ProfilePictureProperties(
        DataSize maxSize,
        List<String> allowedTypes,
        int minDimension,
        long maxPixels,
        int outputSize,
        @DecimalMin("0.1") @DecimalMax("1.0") double outputQuality,
        String outputFormat) {
}