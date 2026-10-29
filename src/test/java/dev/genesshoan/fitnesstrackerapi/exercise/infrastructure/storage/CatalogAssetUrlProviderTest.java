package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.storage;

import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.config.CatalogProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogAssetUrlProviderTest {

    private static final String BASE_URL = "https://cdn.test.example.com/catalog";

    private CatalogAssetUrlProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CatalogAssetUrlProvider(new CatalogProperties(BASE_URL));
    }

    @Test
    @DisplayName("Should resolve the exercise GIF URL without duplicating the path segment")
    void exerciseGifUrl_shouldNotDuplicateThePathSegment() {
        assertThat(provider.exerciseGifUrl("exercises/2gPfomN.gif"))
                .isEqualTo("https://cdn.test.example.com/catalog/exercises/2gPfomN.gif")
                .doesNotContain("exercises/gifs/exercises");
    }

    @Test
    @DisplayName("Should resolve the exercise thumbnail URL without duplicating the path segment")
    void exerciseThumbnailUrl_shouldNotDuplicateThePathSegment() {
        assertThat(provider.exerciseThumbnailUrl("exercise-thumbnails/34-sit-up.512ebfb367761472.webp"))
                .isEqualTo("https://cdn.test.example.com/catalog/exercise-thumbnails/34-sit-up.512ebfb367761472.webp")
                .doesNotContain("exercise-thumbnails/exercise-thumbnails");
    }

    @Test
    @DisplayName("Should resolve the muscle image URL without duplicating the path segment")
    void muscleUrl_shouldNotDuplicateThePathSegment() {
        assertThat(provider.muscleUrl("muscles/male/front/chest.png"))
                .isEqualTo("https://cdn.test.example.com/catalog/muscles/male/front/chest.png")
                .doesNotContain("muscles/images/muscles");
    }

    @Test
    @DisplayName("Should return null when the object key is null")
    void build_shouldReturnNullWhenObjectKeyIsNull() {
        assertThat(provider.exerciseGifUrl(null)).isNull();
        assertThat(provider.exerciseThumbnailUrl(null)).isNull();
        assertThat(provider.muscleUrl(null)).isNull();
    }
}
