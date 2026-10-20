package dev.genesshoan.fitnesstrackerapi.user.application.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/** Processed image bytes and metadata ready for object storage. */
public record ProcessedImage(byte[] data, String contentType) {

    /** Returns a new stream over the processed image bytes. */
    public InputStream inputStream() {
        return new ByteArrayInputStream(data);
    }

    /** Returns the number of bytes in the processed image. */
    public long size() {
        return data.length;
    }
}
