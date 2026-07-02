package com.zynboot.infra.storage.model;

import java.io.IOException;
import java.io.InputStream;

public record StoredObject(
        String key,
        String contentType,
        long size,
        InputStream inputStream
) implements AutoCloseable {

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
