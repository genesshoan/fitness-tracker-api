package dev.genesshoan.fitnesstrackerapi.infrastructure.storage.s3;

import java.io.InputStream;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.FileStorageException;
import dev.genesshoan.fitnesstrackerapi.common.storage.ObjectStoragePort;
import dev.genesshoan.fitnesstrackerapi.infrastructure.storage.s3.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * S3-compatible implementation of the {@link ObjectStoragePort}.
 *
 * <p>This adapter uses the AWS SDK v2 to interact with S3-compatible
 * object storage (e.g. MinIO, Backblaze B2) for uploading, retrieving,
 * and deleting profile pictures.
 */
@Component
@RequiredArgsConstructor
public class S3FileStorageAdapter implements ObjectStoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    /** {@inheritDoc} */
    @Override
    public void delete(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(storageProperties.bucket())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (SdkException e) {
            throw new FileStorageException("Could not delete file: " + key, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getPresignedUrl(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(storageProperties.bucket())
                    .key(key)
                    .build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(storageProperties.presignedUrlExpiration())
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(getObjectPresignRequest).url().toString();
        } catch (SdkException e) {
            throw new FileStorageException("Could not generate file URL: " + key, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void upload(String key, InputStream content, long size, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(storageProperties.bucket())
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(content, size));
        } catch (SdkException e) {
            throw new FileStorageException("Could not upload file: " + key, e);
        }
    }
}
