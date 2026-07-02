package com.zynboot.kit.okhttp;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * HTTP 请求构建器，链式 API。
 *
 * <pre>
 * // GET
 * client.request("https://api.example.com/users")
 *     .param("page", "1")
 *     .get()
 *     .execute();
 *
 * // POST JSON
 * client.request("https://api.example.com/users")
 *     .header("Authorization", "Bearer token")
 *     .json("{\"name\":\"test\"}")
 *     .post()
 *     .execute();
 *
 * // 流式下载
 * client.request("https://example.com/file.zip")
 *     .download(Path.of("/tmp/file.zip"));
 * </pre>
 */
public class RequestBuilder {

    private static final String JSON_CT = "application/json";
    private static final String XML_CT = "application/xml";
    private static final String TEXT_CT = "text/plain";
    private static final String FORM_CT = "application/x-www-form-urlencoded";
    private static final String BINARY_CT = "application/octet-stream";

    private final HttpClient client;
    private final String url;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, String> params = new LinkedHashMap<>();

    private String jsonBody;
    private String xmlBody;
    private String textBody;
    private Map<String, String> formFields;
    private List<FormPart> files;
    private byte[] binaryBody;
    private InputStream streamBody;
    private String streamFileName;
    private String streamContentType;
    private long streamContentLength = -1;

    private long perRequestTimeoutMs = -1;

    RequestBuilder(HttpClient client, String url) {
        this.client = client;
        this.url = url;
    }

    // ==================== 配置 ====================

    public RequestBuilder header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public RequestBuilder headers(Map<String, String> headers) {
        if (headers != null) this.headers.putAll(headers);
        return this;
    }

    public RequestBuilder param(String name, String value) {
        params.put(name, value);
        return this;
    }

    public RequestBuilder params(Map<String, String> params) {
        if (params != null) this.params.putAll(params);
        return this;
    }

    public RequestBuilder timeout(long timeout, TimeUnit unit) {
        this.perRequestTimeoutMs = unit.toMillis(timeout);
        return this;
    }

    // ==================== 请求体 ====================

    public RequestBuilder json(String json) {
        this.jsonBody = json;
        return this;
    }

    public RequestBuilder xml(String xml) {
        this.xmlBody = xml;
        return this;
    }

    public RequestBuilder text(String text) {
        this.textBody = text;
        return this;
    }

    public RequestBuilder form(Map<String, String> fields) {
        this.formFields = fields;
        return this;
    }

    public RequestBuilder file(FormPart file) {
        this.files = List.of(file);
        return this;
    }

    public RequestBuilder files(List<FormPart> files) {
        this.files = files;
        return this;
    }

    public RequestBuilder binary(byte[] data) {
        this.binaryBody = data;
        return this;
    }

    public RequestBuilder stream(InputStream inputStream, String fileName, String contentType, long contentLength) {
        this.streamBody = inputStream;
        this.streamFileName = fileName;
        this.streamContentType = contentType;
        this.streamContentLength = contentLength;
        return this;
    }

    // ==================== 执行 ====================

    public HttpResponse get() {
        return execute("GET", null);
    }

    public HttpResponse head() {
        return execute("HEAD", null);
    }

    public HttpResponse post() {
        return execute("POST", buildBody());
    }

    public HttpResponse put() {
        return execute("PUT", buildBody());
    }

    public HttpResponse patch() {
        return execute("PATCH", buildBody());
    }

    public HttpResponse delete() {
        return execute("DELETE", buildBody());
    }

    /**
     * 流式下载到文件。
     *
     * @param targetPath 目标文件路径
     * @return 下载字节数，失败返回 -1
     */
    public long download(Path targetPath) {
        Request req = new Request.Builder()
                .url(buildUrl())
                .headers(buildHeaders())
                .get()
                .build();
        return client.download(req, targetPath);
    }

    /**
     * 流式读取 SSE 响应。
     *
     * @param onLine 每行回调
     */
    public void stream(Consumer<String> onLine) {
        Request req = new Request.Builder()
                .url(buildUrl())
                .headers(buildHeaders())
                .get()
                .build();
        client.stream(req, onLine);
    }

    // ==================== 内部 ====================

    private HttpResponse execute(String method, RequestBody body) {
        Request.Builder rb = new Request.Builder()
                .url(buildUrl())
                .headers(buildHeaders());

        Request req;
        if (body != null && !method.equals("GET") && !method.equals("HEAD")) {
            req = rb.method(method, body).build();
        } else {
            req = rb.method(method, null).build();
        }

        if (perRequestTimeoutMs > 0) {
            return client.executeWithTimeout(req, perRequestTimeoutMs);
        }
        return client.execute(req);
    }

    private String buildUrl() {
        if (params.isEmpty()) return url;
        HttpUrl parsed = HttpUrl.parse(url);
        if (parsed == null) throw new IllegalArgumentException("Invalid url: " + url);
        HttpUrl.Builder builder = parsed.newBuilder();
        params.forEach(builder::addQueryParameter);
        return builder.build().toString();
    }

    private okhttp3.Headers buildHeaders() {
        okhttp3.Headers.Builder hb = new okhttp3.Headers.Builder();
        headers.forEach((k, v) -> {
            if (k != null && v != null) hb.add(k, v);
        });
        return hb.build();
    }

    private RequestBody buildBody() {
        // JSON
        if (jsonBody != null) {
            return RequestBody.create(jsonBody, MediaType.parse(JSON_CT));
        }
        // XML
        if (xmlBody != null) {
            return RequestBody.create(xmlBody, MediaType.parse(XML_CT));
        }
        // Text
        if (textBody != null) {
            return RequestBody.create(textBody, MediaType.parse(TEXT_CT));
        }
        // Binary
        if (binaryBody != null) {
            return RequestBody.create(binaryBody, MediaType.parse(BINARY_CT));
        }
        // Stream
        if (streamBody != null) {
            MediaType mt = MediaType.parse(streamContentType != null ? streamContentType : BINARY_CT);
            return new StreamingRequestBody(streamBody, mt, streamContentLength);
        }
        // Multipart (files, optionally with fields)
        if (files != null) {
            MultipartBody.Builder mb = new MultipartBody.Builder().setType(MultipartBody.FORM);
            for (FormPart f : files) {
                if (f == null) continue;
                MediaType mt = MediaType.parse(f.contentType());
                RequestBody rb = switch (f) {
                    case FormPart.Bytes b -> RequestBody.create(b.data(), mt);
                    case FormPart.FileRef fr -> RequestBody.create(fr.file(), mt);
                    case FormPart.PathRef pr -> RequestBody.create(pr.path().toFile(), mt);
                };
                mb.addFormDataPart(f.fieldName(), f.fileName(), rb);
            }
            if (formFields != null) {
                formFields.forEach(mb::addFormDataPart);
            }
            return mb.build();
        }
        // Form (fields only)
        if (formFields != null) {
            FormBody.Builder fb = new FormBody.Builder();
            formFields.forEach(fb::add);
            return fb.build();
        }
        return null;
    }
}
