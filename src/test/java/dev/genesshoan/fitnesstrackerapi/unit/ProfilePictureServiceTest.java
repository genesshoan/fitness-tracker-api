package dev.genesshoan.fitnesstrackerapi.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidProfilePictureException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.FileStorageException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.storage.ObjectStoragePort;
import dev.genesshoan.fitnesstrackerapi.user.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.profilepicture.processor.ImageProcessor;
import dev.genesshoan.fitnesstrackerapi.user.profilepicture.processor.ProcessedImage;
import dev.genesshoan.fitnesstrackerapi.user.profilepicture.service.ProfilePictureService;
import dev.genesshoan.fitnesstrackerapi.user.profilepicture.validator.ProfilePictureValidator;
import net.datafaker.Faker;

@ExtendWith(MockitoExtension.class)
class ProfilePictureServiceTest {

    private static final Faker FAKER = new Faker();
    private static final UUID USER_ID = UUID.fromString("01932f4a-1234-7000-8000-123456789abc");
    private static final String PICTURE_KEY = "profile-pictures/" + USER_ID + ".jpg";

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfilePictureValidator validator;

    @Mock
    private ImageProcessor imageProcessor;

    @Mock
    private ObjectStoragePort objectStorage;

    @InjectMocks
    private ProfilePictureService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .username(FAKER.name().username())
                .email(FAKER.internet().emailAddress())
                .passwordHash("hash")
                .timezone("UTC")
                .build();
    }

    @Test
    void uploadProfilePicture_ShouldStoreProcessedImageAndReturnUrl() {
        var file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1, 2, 3});
        var processed = new ProcessedImage(new byte[] {4, 5}, "image/jpeg");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(imageProcessor.process(org.mockito.ArgumentMatchers.any())).thenReturn(processed);
        when(objectStorage.getPresignedUrl(PICTURE_KEY)).thenReturn("https://storage/profile.jpg");

        String result = service.uploadProfilePicture(USER_ID, file);

        assertThat(result).isEqualTo("https://storage/profile.jpg");
        assertThat(user.getProfilePictureKey()).isEqualTo(PICTURE_KEY);
        verify(validator).validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(3L));
        verify(objectStorage).upload(
                org.mockito.ArgumentMatchers.eq(PICTURE_KEY),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.eq("image/jpeg"));
    }

    @Test
    void uploadProfilePicture_ShouldReuseExistingKeyWhenReplacing() {
        var existingKey = "profile-pictures/custom.jpg";
        user.setProfilePictureKey(existingKey);
        var file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1});
        var processed = new ProcessedImage(new byte[] {2}, "image/jpeg");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(imageProcessor.process(org.mockito.ArgumentMatchers.any())).thenReturn(processed);
        when(objectStorage.getPresignedUrl(existingKey)).thenReturn("https://storage/replaced.jpg");

        assertThat(service.uploadProfilePicture(USER_ID, file)).isEqualTo("https://storage/replaced.jpg");

        verify(objectStorage).upload(
                org.mockito.ArgumentMatchers.eq(existingKey),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("image/jpeg"));
        assertThat(user.getProfilePictureKey()).isEqualTo(existingKey);
    }

    @Test
    void uploadProfilePicture_ShouldNotStoreInvalidImage() {
        var file = new MockMultipartFile("file", "avatar.txt", "text/plain", new byte[] {1});
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new InvalidProfilePictureException("Unsupported file type"))
                .when(validator)
                .validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong());

        assertThatThrownBy(() -> service.uploadProfilePicture(USER_ID, file))
                .isInstanceOf(InvalidProfilePictureException.class);

        verifyNoInteractions(imageProcessor, objectStorage);
    }

    @Test
    void uploadProfilePicture_ShouldFailBeforeReadingFileWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        var file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1});

        assertThatThrownBy(() -> service.uploadProfilePicture(USER_ID, file))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(validator, imageProcessor, objectStorage);
    }

    @Test
    void uploadProfilePicture_ShouldPropagateStorageFailureWithoutChangingUserKey() {
        var existingKey = "profile-pictures/existing.jpg";
        user.setProfilePictureKey(existingKey);
        var file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1});
        var processed = new ProcessedImage(new byte[] {2}, "image/jpeg");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(imageProcessor.process(org.mockito.ArgumentMatchers.any())).thenReturn(processed);
        org.mockito.Mockito.doThrow(new FileStorageException("provider details", new RuntimeException()))
                .when(objectStorage)
                .upload(org.mockito.ArgumentMatchers.eq(existingKey), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("image/jpeg"));

        assertThatThrownBy(() -> service.uploadProfilePicture(USER_ID, file))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("provider details");

        assertThat(user.getProfilePictureKey()).isEqualTo(existingKey);
    }

    @Test
    void getProfilePictureUrl_ShouldReturnNullWhenKeyIsMissing() {
        assertThat(service.getProfilePictureUrl(null)).isNull();
        verifyNoInteractions(objectStorage);
    }
}
