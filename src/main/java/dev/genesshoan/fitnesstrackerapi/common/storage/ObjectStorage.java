package dev.genesshoan.fitnesstrackerapi.common.storage;

import java.io.InputStream;

public interface ObjectStorage {

    String generateUploadUrl(String objectKey);

    String generateDownloadUrl(String objectKey);

    InputStream getObject(String objectKey);

    void deleteObject(String objectKey);
}
