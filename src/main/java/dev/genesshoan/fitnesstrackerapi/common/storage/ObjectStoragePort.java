package dev.genesshoan.fitnesstrackerapi.common.storage;

import java.io.InputStream;

/**
 * Application port for storing and retrieving binary objects.
 *
 * <p>Infrastructure adapters implement this contract so application services
 * do not depend on a specific object-storage provider.
 */
public interface ObjectStoragePort {

    /** Stores an object under the supplied key. */
    void upload(String key, InputStream content, long size, String contentType);

    /** Deletes an object; implementations should treat missing objects as successful. */
    void delete(String key);

    /** Creates a temporary URL that grants read access to an object. */
    String getPresignedUrl(String key);
}
