package dev.genesshoan.fitnesstrackerapi.user.profilepicture.port;

import java.io.InputStream;

public interface FileStoragePort {

    void upload(String key, InputStream content, long size, String contentType);

    void delete(String key);

    String getPresignedUrl(String key);
}
