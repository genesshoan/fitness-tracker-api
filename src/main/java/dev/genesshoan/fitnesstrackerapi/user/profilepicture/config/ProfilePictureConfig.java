package dev.genesshoan.fitnesstrackerapi.user.profilepicture.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.tika.Tika;

/**
 * Configuration class for profile picture processing.
 *
 * <p>Provides the {@link Tika} bean for MIME type detection.
 */
@Configuration
public class ProfilePictureConfig {

    /**
     * Creates a {@link Tika} instance for MIME type detection.
     *
     * @return a new Tika instance
     */
    @Bean
    public Tika tika() {
        return new Tika();
    }
}
