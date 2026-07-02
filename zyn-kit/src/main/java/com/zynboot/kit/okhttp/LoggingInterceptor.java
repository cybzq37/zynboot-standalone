package com.zynboot.kit.okhttp;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Headers;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp 请求/响应日志拦截器（结构化日志）。
 * <p>
 * 仅在 DEBUG 级别启用时生效，自动截断过大的请求/响应体。
 */
public class LoggingInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private final int maxBodyBytes;
    private final int maxBodyChars;

    public LoggingInterceptor() {
        this(8192, 8192);
    }

    public LoggingInterceptor(int maxBodyBytes, int maxBodyChars) {
        this.maxBodyBytes = maxBodyBytes;
        this.maxBodyChars = maxBodyChars;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (!log.isDebugEnabled()) {
            return chain.proceed(request);
        }

        String requestBodySummary = summarizeRequestBody(request);
        log.debug("http_request method={} url={} body_size={} body={}",
                request.method(),
                request.url(),
                requestBodySummary.length(),
                requestBodySummary);

        long startNs = System.nanoTime();
        Response response = chain.proceed(request);
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            log.debug("http_response status={} url={} took_ms={} body=<null>",
                    response.code(),
                    response.request().url(),
                    tookMs);
            return response;
        }

        String responseBodySummary = summarizeResponseBody(response, responseBody);
        log.debug("http_response status={} url={} took_ms={} body_size={} body={}",
                response.code(),
                response.request().url(),
                tookMs,
                responseBodySummary.length(),
                responseBodySummary);

        return response;
    }

    private String summarizeRequestBody(Request request) {
        RequestBody body = request.body();
        if (body == null) return "<empty>";

        MediaType mediaType = body.contentType();
        long contentLength;
        try {
            contentLength = body.contentLength();
        } catch (IOException ex) {
            contentLength = -1;
        }

        if (isFileLike(mediaType, request.headers())) {
            return "<file body, size=" + formatLength(contentLength) + ">";
        }
        if (!isTextLike(mediaType)) {
            return "<binary body, size=" + formatLength(contentLength) + ">";
        }
        if (contentLength > maxBodyBytes) {
            return "<text body too large, size=" + contentLength + ">";
        }

        try {
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            if (buffer.size() > maxBodyBytes) {
                return "<text body too large, size=" + buffer.size() + ">";
            }
            Charset charset = mediaType != null ? mediaType.charset(UTF8) : UTF8;
            String text = buffer.readString(charset == null ? UTF8 : charset);
            if (text.length() > maxBodyChars) {
                return "<text body too long, chars=" + text.length() + ">";
            }
            return text;
        } catch (IOException ex) {
            return "<body read failed: " + ex.getMessage() + ">";
        }
    }

    private String summarizeResponseBody(Response response, ResponseBody responseBody) {
        MediaType mediaType = responseBody.contentType();
        long contentLength = responseBody.contentLength();
        Headers headers = response.headers();

        if (isFileLike(mediaType, headers)) {
            return "<file body, size=" + formatLength(contentLength) + ">";
        }
        if (!isTextLike(mediaType)) {
            return "<binary body, size=" + formatLength(contentLength) + ">";
        }
        if (contentLength > maxBodyBytes) {
            return "<text body too large, size=" + contentLength + ">";
        }

        try {
            String text = response.peekBody(maxBodyBytes).string();
            if (text.length() > maxBodyChars) {
                return "<text body too long, chars=" + text.length() + ">";
            }
            return text;
        } catch (IOException ex) {
            return "<body peek failed: " + ex.getMessage() + ">";
        }
    }

    private boolean isTextLike(MediaType mediaType) {
        if (mediaType == null) return false;
        String type = mediaType.type();
        String subtype = mediaType.subtype();
        if ("text".equalsIgnoreCase(type)) return true;
        if (subtype == null) return false;
        String normalized = subtype.toLowerCase();
        return normalized.contains("json")
                || normalized.contains("xml")
                || normalized.contains("x-www-form-urlencoded")
                || normalized.contains("graphql")
                || normalized.contains("javascript")
                || normalized.contains("html")
                || normalized.contains("plain");
    }

    private boolean isFileLike(MediaType mediaType, Headers headers) {
        String disposition = headers.get("Content-Disposition");
        if (disposition != null && disposition.toLowerCase().contains("attachment")) return true;
        if (mediaType == null) return false;
        String type = mediaType.type();
        String subtype = mediaType.subtype();
        if ("image".equalsIgnoreCase(type)
                || "video".equalsIgnoreCase(type)
                || "audio".equalsIgnoreCase(type)
                || ("application".equalsIgnoreCase(type) && "octet-stream".equalsIgnoreCase(subtype))) {
            return true;
        }
        if (subtype == null) return false;
        String normalized = subtype.toLowerCase();
        return normalized.contains("zip")
                || normalized.contains("pdf")
                || normalized.contains("msword")
                || normalized.contains("excel")
                || normalized.contains("wordprocessingml")
                || normalized.contains("spreadsheetml");
    }

    private String formatLength(long len) {
        return len < 0 ? "unknown" : len + " bytes";
    }
}
