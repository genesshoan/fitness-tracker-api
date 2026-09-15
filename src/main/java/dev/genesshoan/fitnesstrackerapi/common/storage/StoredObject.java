package dev.genesshoan.fitnesstrackerapi.common.storage;

import java.io.IOException;
import java.io.InputStream;

public record StoredObject(InputStream content, String contentType, long size) implements AutoCloseable {

    @Override
    public void close() throws IOException {
        content.close();
    }
}
