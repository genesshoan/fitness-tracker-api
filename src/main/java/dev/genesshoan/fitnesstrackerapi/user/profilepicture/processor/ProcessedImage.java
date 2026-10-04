package dev.genesshoan.fitnesstrackerapi.user.profilepicture.processor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public record ProcessedImage(byte[] data, String contentType) {

    public InputStream inputStream() {
        return new ByteArrayInputStream(data);
    }

    public long size() {
        return data.length;
    }
}
