package dev.genesshoan.fitnesstrackerapi.user.profilepicture.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidProfilePictureException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ProfilePictureProcessingException;
import dev.genesshoan.fitnesstrackerapi.user.profilepicture.config.ProfilePictureProperties;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;

/**
 * Validates uploaded profile pictures against size, format, and dimension constraints.
 *
 * <p>This validator checks that profile pictures meet the requirements specified
 * in {@link ProfilePictureProperties}, ensuring files are safe and properly formatted
 * before further processing.
 */
@Component
@RequiredArgsConstructor
public class ProfilePictureValidator {

    private final ProfilePictureProperties pictureProperties;
    private final Tika tika;

    /**
     * Validates a profile picture's size and dimensions.
     *
     * <p>This method performs comprehensive validation:
     * <ul>
     * <li>Checks if the image is empty</li>
     * <li>Validates file size against maximum allowed size</li>
     * <li>Verifies the file type using MIME detection</li>
     * <li>Ensures image dimensions are within acceptable bounds</li>
     * </ul>
     *
     * @param contentSupplier supplies the image content stream
     * @param size the size of the uploaded file in bytes
     * @throws InvalidProfilePictureException if the image fails validation
     * @throws ProfilePictureProcessingException if an error occurs during validation
     */
    public void validate(Supplier<InputStream> contentSupplier, long size) {
        validateSize(size);
        try {
            try (InputStream in = contentSupplier.get()) {
                validateType(in);
            }

            try (InputStream in = contentSupplier.get()) {
                validateDimensions(in);
            }

        } catch (IOException e) {
            throw new ProfilePictureProcessingException("Error while validating image", e);
        }
    }

    /**
     * Validates the image size against configured limits.
     *
     * @param size the file size in bytes
     * @throws InvalidProfilePictureException if the file is empty or exceeds maximum size
     */
    private void validateSize(long size) {
        if (size == 0) {
            throw new InvalidProfilePictureException("Image is empty");
        }

        if (size > pictureProperties.maxSize().toBytes()) {
            throw new InvalidProfilePictureException(
                    "File exceeds maximum size of " + pictureProperties.maxSize() + " bytes");
        }
    }

    /**
     * Validates the file type using MIME detection.
     *
     * @param content the input stream containing the file data
     * @throws InvalidProfilePictureException if the file type is not in the allowed types list
     */
    private void validateType(InputStream content) {
        String detectedType;
        try {
            detectedType = tika.detect(content);
        } catch (IOException e) {
            throw new InvalidProfilePictureException("Unable to read file content", e);
        }

        if (!pictureProperties.allowedTypes().contains(detectedType)) {
            throw new InvalidProfilePictureException("Unsupported file type: " + detectedType);
        }
    }

    /**
     * Validates the image dimensions against configured constraints.
     *
     * @param content the input stream containing the image data
     * @throws InvalidProfilePictureException if the image format is unsupported or dimensions are invalid
     */
    private void validateDimensions(InputStream content) {
        try (ImageInputStream in = ImageIO.createImageInputStream(content)) {
            if (in == null) {
                throw new InvalidProfilePictureException("Unsupported file format");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);

            if (!readers.hasNext()) {
                throw new InvalidProfilePictureException("Unsupported or corrupt image");
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(in);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * height;

                if (width < pictureProperties.minDimension() || height < pictureProperties.minDimension()) {
                    throw new InvalidProfilePictureException("Image is too small");
                }

                if (pixels > pictureProperties.maxPixels()) {
                    throw new InvalidProfilePictureException("Image dimensions are too large");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new InvalidProfilePictureException("Unable to read image", e);
        }
    }
}
