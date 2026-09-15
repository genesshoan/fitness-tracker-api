package dev.genesshoan.fitnesstrackerapi.infrastructure.storage;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.StorageException;
import dev.genesshoan.fitnesstrackerapi.common.storage.ObjectStorage;
import dev.genesshoan.fitnesstrackerapi.infrastructure.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinioObjectStorage implements ObjectStorage {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public String generateUploadUrl(String objectKey) {

        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioProperties.bucket())
                    .object(objectKey)
                    .expiry((int) minioProperties.presignedUrlExpiration().toSeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to generate upload URL", e);
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
            throw new StorageException("Failed to generate download URL", e);
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
            throw new StorageException("Failed to download object", e);
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
            throw new StorageException("Failed to delete object", e);
        }
    }
}
