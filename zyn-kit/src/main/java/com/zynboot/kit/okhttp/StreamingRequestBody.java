package com.zynboot.kit.okhttp;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.IOException;
import java.io.InputStream;

/**
 * 流式 RequestBody，从 InputStream 读取数据，不加载到内存。
 */
class StreamingRequestBody extends RequestBody {

    private final InputStream inputStream;
    private final MediaType mediaType;
    private final long contentLength;

    StreamingRequestBody(InputStream inputStream, MediaType mediaType, long contentLength) {
        this.inputStream = inputStream;
        this.mediaType = mediaType;
        this.contentLength = contentLength;
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        try (Source source = Okio.source(inputStream)) {
            sink.writeAll(source);
        }
    }
}
