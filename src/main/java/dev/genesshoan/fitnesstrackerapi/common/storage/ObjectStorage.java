package dev.genesshoan.fitnesstrackerapi.common.storage;

import java.io.InputStream;

public interface ObjectStorage {

    UploadPolicy generateUploadPolicy(String objectKey, String contentType, long maxSizeBytes);

    String generateDownloadUrl(String objectKey);

    InputStream getObject(String objectKey);

    void deleteObject(String objectKey);
}
