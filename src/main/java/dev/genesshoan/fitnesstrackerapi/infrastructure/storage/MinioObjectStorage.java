package dev.genesshoan.fitnesstrackerapi.infrastructure.storage;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.StorageException;
import dev.genesshoan.fitnesstrackerapi.common.storage.ObjectStorage;
import dev.genesshoan.fitnesstrackerapi.common.storage.UploadPolicy;
import dev.genesshoan.fitnesstrackerapi.infrastructure.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioObjectStorage implements ObjectStorage {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public UploadPolicy generateUploadPolicy(String objectKey, String contentType, long maxSizeBytes) {
        try {
            PostPolicy policy = new PostPolicy(
                    minioProperties.bucket(),
                    ZonedDateTime.now()
                            .plusSeconds(
                                    minioProperties.presignedUrlExpiration().toSeconds()));

            policy.addEqualsCondition("key", objectKey);
            policy.addStartsWithCondition("Content-Type", contentType);
            policy.addContentLengthRangeCondition(1, maxSizeBytes);

            Map<String, String> formData = new HashMap<>(minioClient.getPresignedPostFormData(policy));
            formData.put("key", objectKey);

            String uploadUrl = minioProperties.endpoint() + "/" + minioProperties.bucket();

            return new UploadPolicy(uploadUrl, formData);
        } catch (Exception e) {
            log.error("Failed to generate upload policy for key {}", objectKey, e);
            throw new StorageException("Failed to generate upload policy for key: " + objectKey, e);
        }
    }

    @Override
    public String generateDownloadUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.bucket())
                    .object(objectKey)
                    .expiry((int) minioProperties.presignedUrlExpiration().toSeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate download URL for key {}", objectKey, e);
            throw new StorageException("Failed to generate download URL for key: " + objectKey, e);
        }
    }

    @Override
    public InputStream getObject(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("Failed to download object with key {}", objectKey, e);
            throw new StorageException("Failed to download object with key: " + objectKey, e);
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete object with key {}", objectKey, e);
            throw new StorageException("Failed to delete object with key: " + objectKey, e);
        }
    }
}
