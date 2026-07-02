package com.zynboot.kit.okhttp;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okio.BufferedSource;

/**
 * OkHttp 客户端封装。
 * <p>
 * 通过 {@link #request(String)} 创建 {@link RequestBuilder} 链式构建请求。
 *
 * <pre>
 * // Spring 注入
 * private final HttpClient httpClient;
 *
 * // GET
 * HttpResponse resp = httpClient.request("https://api.example.com/users")
 *     .param("page", "1")
 *     .get();
 *
 * // POST JSON
 * HttpResponse resp = httpClient.request("https://api.example.com/users")
 *     .header("Authorization", "Bearer token")
 *     .json("{\"name\":\"test\"}")
 *     .post();
 *
 * // 流式下载
 * httpClient.request("https://example.com/file.zip")
 *     .download(Path.of("/tmp/file.zip"));
 * </pre>
 */
public class HttpClient {

    private static final Logger log = LoggerFactory.getLogger(HttpClient.class);

    private static volatile HttpClient instance;

    /**
     * 获取默认单例实例（懒加载，线程安全）。
     */
    public static HttpClient getInstance() {
        HttpClient h = instance;
        if (h == null) {
            synchronized (HttpClient.class) {
                h = instance;
                if (h == null) {
                    instance = h = new HttpClient();
                }
            }
        }
        return h;
    }

    final OkHttpClient client;
    final boolean throwOnHttpError;

    public HttpClient() {
        this(new OkHttpClient.Builder()
                .connectTimeout(HttpClientProperties.DEFAULT_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(HttpClientProperties.DEFAULT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(HttpClientProperties.DEFAULT_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(HttpClientProperties.DEFAULT_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build(), false);
    }

    public HttpClient(OkHttpClient client) {
        this(client, false);
    }

    public HttpClient(OkHttpClient client, boolean throwOnHttpError) {
        if (client == null) {
            throw new IllegalArgumentException("OkHttpClient must not be null");
        }
        this.client = client;
        this.throwOnHttpError = throwOnHttpError;
    }

    // ==================== Request Builder ====================

    /**
     * 创建请求构建器。
     */
    public RequestBuilder request(String url) {
        return new RequestBuilder(this, url);
    }

    // ==================== Execute ====================

    public HttpResponse execute(Request request) {
        try (Response response = client.newCall(request).execute()) {
            return toResponse(response);
        } catch (IOException e) {
            if (throwOnHttpError) {
                throw toHttpException(e, request);
            }
            log.error("http_request_failed method={} url={} error={}", request.method(), request.url(), e.getMessage());
            return HttpResponse.failure(e.getMessage());
        }
    }

    HttpResponse executeWithTimeout(Request request, long timeoutMs) {
        OkHttpClient timeoutClient = client.newBuilder()
                .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
        try (Response response = timeoutClient.newCall(request).execute()) {
            return toResponse(response);
        } catch (IOException e) {
            if (throwOnHttpError) {
                throw toHttpException(e, request);
            }
            log.error("http_request_failed method={} url={} error={}", request.method(), request.url(), e.getMessage());
            return HttpResponse.failure(e.getMessage());
        }
    }

    public HttpResponse executeBytes(Request request) {
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                return HttpResponse.success(response.code(), toHeaderMap(response), null);
            }
            return HttpResponse.successBytes(response.code(), toHeaderMap(response), body.bytes());
        } catch (IOException e) {
            if (throwOnHttpError) {
                throw toHttpException(e, request);
            }
            log.error("http_request_failed method={} url={} error={}", request.method(), request.url(), e.getMessage());
            return HttpResponse.failure(e.getMessage());
        }
    }

    // ==================== Streaming Download ====================

    public long download(Request request, Path targetPath) {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("download_failed status={} url={}", response.code(), request.url());
                return -1;
            }
            ResponseBody body = response.body();
            if (body == null) return 0;
            try (InputStream in = body.byteStream();
                 OutputStream out = Files.newOutputStream(targetPath)) {
                byte[] buf = new byte[8192];
                long total = 0;
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    total += n;
                }
                return total;
            }
        } catch (IOException e) {
            log.error("download_failed url={} error={}", request.url(), e.getMessage());
            return -1;
        }
    }

    // ==================== Streaming Upload ====================

    public HttpResponse uploadStream(String url, Map<String, String> headers,
                                     String fileName, InputStream inputStream,
                                     String contentType, long contentLength) {
        String ct = contentType != null ? contentType : "application/octet-stream";
        RequestBody body = new StreamingRequestBody(inputStream, okhttp3.MediaType.parse(ct), contentLength);
        Request request = new Request.Builder()
                .url(url)
                .headers(buildHeaders(headers))
                .post(body)
                .build();
        return execute(request);
    }

    // ==================== SSE / Streaming Response ====================

    public void stream(Request request, Consumer<String> onLine) {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("stream_failed status={} url={}", response.code(), request.url());
                return;
            }
            ResponseBody body = response.body();
            if (body == null) return;
            BufferedSource source = body.source();
            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line != null) {
                    onLine.accept(line);
                }
            }
        } catch (IOException e) {
            log.error("stream_failed url={} error={}", request.url(), e.getMessage());
        }
    }

    // ==================== Internal ====================

    private HttpResponse toResponse(Response response) throws IOException {
        ResponseBody body = response.body();
        if (body == null) {
            return HttpResponse.success(response.code(), toHeaderMap(response), null);
        }
        String text = body.string();
        HttpResponse result = HttpResponse.success(response.code(), toHeaderMap(response), text);
        if (throwOnHttpError && !result.isSuccessful()) {
            throw new HttpServerException(result.statusCode(), result.body());
        }
        return result;
    }

    private HttpClientException toHttpException(IOException e, Request request) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("timeout") || msg.contains("timed out"))) {
            return new HttpTimeoutException(request.url().toString(), e);
        }
        return new HttpConnectionException(request.url().toString(), e);
    }

    private Map<String, String> toHeaderMap(Response response) {
        return HttpResponse.toHeaderMap(response.headers());
    }

    Headers buildHeaders(Map<String, String> headers) {
        Headers.Builder builder = new Headers.Builder();
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (k != null && v != null) {
                    builder.add(k, v);
                }
            });
        }
        return builder.build();
    }
}
