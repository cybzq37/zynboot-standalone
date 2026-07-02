package com.zynboot.kit.okhttp;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

/**
 * 请求体大小限制拦截器。
 * <p>
 * 超过指定字节数的请求体将被拒绝，防止意外上传过大文件。
 */
public class MaxRequestSizeInterceptor implements Interceptor {

    private final long maxBytes;

    public MaxRequestSizeInterceptor(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        RequestBody body = request.body();

        if (body != null) {
            long contentLength = body.contentLength();
            if (contentLength > maxBytes) {
                throw new IOException(
                        "Request body too large: " + contentLength + " bytes (max=" + maxBytes + ")");
            }
        }

        return chain.proceed(request);
    }
}
