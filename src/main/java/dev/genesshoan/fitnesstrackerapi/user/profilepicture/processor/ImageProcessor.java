package dev.genesshoan.fitnesstrackerapi.user.profilepicture.processor;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidProfilePictureException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ProfilePictureProcessingException;
import dev.genesshoan.fitnesstrackerapi.user.profilepicture.config.ProfilePictureProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import net.coobird.thumbnailator.tasks.UnsupportedFormatException;
import org.springframework.stereotype.Component;

/**
 * Processes uploaded profile pictures by generating thumbnails.
 *
 * <p>This component takes an uploaded image and creates a resized thumbnail
 * using the Coobird Thumbnailator library, respecting the configured
 * output size and quality parameters.
 */
@Component
@RequiredArgsConstructor
public class ImageProcessor {

    private final ProfilePictureProperties pictureProperties;

    /**
     * Processes an uploaded image and generates a thumbnail.
     *
     * <p>Resizes the image to the configured output size while preserving
     * aspect ratio and quality settings.
     *
     * @param content the input stream containing the original image data
     * @return a processed image ready for storage
     * @throws ProfilePictureProcessingException if image processing fails
     */
    public ProcessedImage process(InputStream content) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(content)
                    .size(pictureProperties.outputSize(), pictureProperties.outputSize())
                    .crop(Positions.CENTER)
                    .outputFormat(pictureProperties.outputFormat())
                    .outputQuality(pictureProperties.outputQuality())
                    .useExifOrientation(true)
                    .toOutputStream(out);

            return new ProcessedImage(out.toByteArray(), "image/" + pictureProperties.outputFormat());
        } catch (UnsupportedFormatException e) {
            throw new InvalidProfilePictureException("Unsupported image format", e);
        } catch (IOException e) {
            throw new ProfilePictureProcessingException("Could not process image", e);
        }
    }
}