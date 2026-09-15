package dev.genesshoan.fitnesstrackerapi.common.storage;

public interface ObjectStorage {

    String generateUploadUrl(String objectKey, String contentType);

    String generateDownloadUrl(String objectKey);

    StoredObject getObject(String objectKey);

    void deleteObject(String objectKey);
}
