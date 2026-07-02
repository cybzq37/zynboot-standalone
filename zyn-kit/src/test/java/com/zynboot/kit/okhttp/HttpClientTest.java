package com.zynboot.kit.okhttp;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class HttpClientTest {

    private MockWebServer server;
    private HttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new HttpClient(new OkHttpClient(), false);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private String url(String path) {
        return server.url("/").toString() + path;
    }

    // ==================== GET ====================

    @Test
    void get_simple() {
        server.enqueue(new MockResponse().setBody("hello").setHeader("Content-Type", "text/plain"));
        HttpResponse resp = client.request(url("greet")).get();

        assertTrue(resp.isSuccessful());
        assertEquals(200, resp.statusCode());
        assertEquals("hello", resp.body());
    }

    @Test
    void get_withParams() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"q\":\"test\"}").setHeader("Content-Type", "application/json"));
        client.request(url("search")).param("q", "test").param("page", "1").get();

        RecordedRequest req = server.takeRequest();
        assertTrue(req.getPath().contains("q=test"));
        assertTrue(req.getPath().contains("page=1"));
    }

    @Test
    void get_withHeaders() throws Exception {
        server.enqueue(new MockResponse().setBody("ok"));
        client.request(url("api")).header("X-Token", "abc123").get();

        RecordedRequest req = server.takeRequest();
        assertEquals("abc123", req.getHeader("X-Token"));
    }

    // ==================== POST ====================

    @Test
    void post_json() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"id\":1}").setHeader("Content-Type", "application/json"));
        HttpResponse resp = client.request(url("users")).json("{\"name\":\"test\"}").post();

        assertTrue(resp.isSuccessful());
        assertEquals("{\"id\":1}", resp.body());
        RecordedRequest req = server.takeRequest();
        assertEquals("POST", req.getMethod());
        assertEquals("{\"name\":\"test\"}", req.getBody().readUtf8());
    }

    @Test
    void post_xml() throws Exception {
        server.enqueue(new MockResponse().setBody("ok"));
        client.request(url("xml")).xml("<root>test</root>").post();

        RecordedRequest req = server.takeRequest();
        assertTrue(req.getHeader("Content-Type").contains("xml"));
    }

    @Test
    void post_text() throws Exception {
        server.enqueue(new MockResponse().setBody("ok"));
        client.request(url("text")).text("hello world").post();

        RecordedRequest req = server.takeRequest();
        assertEquals("hello world", req.getBody().readUtf8());
    }

    @Test
    void post_form() throws Exception {
        server.enqueue(new MockResponse().setBody("ok"));
        client.request(url("login")).form(Map.of("user", "admin", "pass", "123")).post();

        RecordedRequest req = server.takeRequest();
        assertTrue(req.getHeader("Content-Type").contains("application/x-www-form-urlencoded"));
        String body = req.getBody().readUtf8();
        assertTrue(body.contains("user=admin"));
    }

    @Test
    void post_multipart() throws Exception {
        server.enqueue(new MockResponse().setBody("ok"));
        FormPart file = FormPart.of("file", "test.txt", "text/plain", "hello".getBytes());
        client.request(url("upload")).file(file).post();

        RecordedRequest req = server.takeRequest();
        assertTrue(req.getHeader("Content-Type").contains("multipart/form-data"));
    }

    @Test
    void post_binary() throws Exception {
        server.enqueue(new MockResponse().setBody("ok"));
        client.request(url("binary")).binary(new byte[]{1, 2, 3}).post();

        RecordedRequest req = server.takeRequest();
        assertEquals("application/octet-stream", req.getHeader("Content-Type"));
    }

    // ==================== PUT / PATCH / DELETE / HEAD ====================

    @Test
    void put_json() throws Exception {
        server.enqueue(new MockResponse().setBody("ok"));
        client.request(url("users/1")).json("{\"name\":\"new\"}").put();

        RecordedRequest req = server.takeRequest();
        assertEquals("PUT", req.getMethod());
    }

    @Test
    void patch_json() throws Exception {
        server.enqueue(new MockResponse().setBody("ok"));
        client.request(url("users/1")).json("{\"name\":\"patched\"}").patch();

        RecordedRequest req = server.takeRequest();
        assertEquals("PATCH", req.getMethod());
    }

    @Test
    void delete_withParams() throws Exception {
        server.enqueue(new MockResponse().setBody("ok"));
        client.request(url("items")).param("id", "42").delete();

        RecordedRequest req = server.takeRequest();
        assertEquals("DELETE", req.getMethod());
        assertTrue(req.getPath().contains("id=42"));
    }

    @Test
    void delete_json() throws Exception {
        server.enqueue(new MockResponse().setBody("ok"));
        client.request(url("bulk")).json("{\"ids\":[1,2]}").delete();

        RecordedRequest req = server.takeRequest();
        assertEquals("DELETE", req.getMethod());
    }

    @Test
    void head() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));
        client.request(url("resource")).head();

        RecordedRequest req = server.takeRequest();
        assertEquals("HEAD", req.getMethod());
    }

    // ==================== Binary Response ====================

    @Test
    void executeBytes() {
        byte[] binary = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        server.enqueue(new MockResponse()
                .setBody(new okio.Buffer().write(binary))
                .setHeader("Content-Type", "image/png"));

        HttpResponse resp = client.executeBytes(
                new okhttp3.Request.Builder().url(url("image")).build());

        assertTrue(resp.isSuccessful());
        assertArrayEquals(binary, resp.bodyBytes());
    }

    // ==================== Streaming Download ====================

    @Test
    void download() throws Exception {
        byte[] content = "file-content".getBytes(StandardCharsets.UTF_8);
        server.enqueue(new MockResponse()
                .setBody(new okio.Buffer().write(content))
                .setHeader("Content-Length", content.length));

        Path target = Files.createTempFile("download-test", ".txt");
        long bytes = client.request(url("download")).download(target);

        assertEquals(content.length, bytes);
        assertArrayEquals(content, Files.readAllBytes(target));
        Files.delete(target);
    }

    @Test
    void download_serverError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));
        Path target = Files.createTempFile("dl-fail", ".txt");

        long bytes = client.request(url("download")).download(target);
        assertEquals(-1, bytes);
        Files.delete(target);
    }

    // ==================== Streaming Upload ====================

    @Test
    void uploadStream() throws Exception {
        server.enqueue(new MockResponse().setBody("uploaded"));
        byte[] data = "stream-data".getBytes(StandardCharsets.UTF_8);

        HttpResponse resp = client.uploadStream(
                url("upload"), Map.of("Authorization", "Bearer tok"),
                "doc.txt", new ByteArrayInputStream(data), "text/plain", data.length);

        assertTrue(resp.isSuccessful());
        RecordedRequest req = server.takeRequest();
        assertEquals("Bearer tok", req.getHeader("Authorization"));
    }

    // ==================== SSE ====================

    @Test
    void stream_sse() {
        server.enqueue(new MockResponse()
                .setBody("data: event1\n\ndata: event2\n\n")
                .setHeader("Content-Type", "text/event-stream"));

        CopyOnWriteArrayList<String> lines = new CopyOnWriteArrayList<>();
        client.request(url("events")).stream(lines::add);

        assertTrue(lines.stream().anyMatch(l -> l.contains("event1")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("event2")));
    }

    // ==================== Per-request Timeout ====================

    @Test
    void requestBuilder_timeout() {
        server.enqueue(new MockResponse().setBody("ok"));

        HttpResponse resp = client.request(url("slow"))
                .timeout(5, TimeUnit.SECONDS)
                .get();

        assertTrue(resp.isSuccessful());
    }

    // ==================== Error Handling ====================

    @Test
    void serverError() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("internal error"));
        HttpResponse resp = client.request(url("error")).get();

        assertFalse(resp.isSuccessful());
        assertEquals(500, resp.statusCode());
        assertEquals("internal error", resp.body());
    }

    @Test
    void notFound() {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("not found"));
        HttpResponse resp = client.request(url("missing")).get();

        assertFalse(resp.isSuccessful());
        assertEquals(404, resp.statusCode());
    }

    @Test
    void nullBody() {
        server.enqueue(new MockResponse().setResponseCode(204));
        HttpResponse resp = client.request(url("no-content")).get();

        assertTrue(resp.isSuccessful());
        assertTrue(resp.body() == null || resp.body().isEmpty());
    }

    // ==================== Exceptions ====================

    @Test
    void throwOnHttpError_serverException() {
        HttpClient throwing = new HttpClient(new OkHttpClient(), true);
        server.enqueue(new MockResponse().setResponseCode(500).setBody("error"));

        HttpServerException ex = assertThrows(HttpServerException.class,
                () -> throwing.request(url("fail")).get());

        assertEquals(500, ex.getStatusCode());
        assertEquals("error", ex.getResponseBody());
    }

    @Test
    void exception_types() {
        HttpServerException serverEx = new HttpServerException(500, "body");
        assertEquals(500, serverEx.getStatusCode());
        assertEquals("body", serverEx.getResponseBody());

        HttpTimeoutException timeoutEx = new HttpTimeoutException("http://test", new IOException("timeout"));
        assertEquals("http://test", timeoutEx.getUrl());
        assertTrue(timeoutEx.getMessage().contains("timed out"));

        HttpConnectionException connEx = new HttpConnectionException("http://test", new IOException("refused"));
        assertEquals("http://test", connEx.getUrl());
        assertTrue(connEx.getMessage().contains("connection failed"));
    }

    // ==================== Retry ====================

    @Test
    void retry_successOnThirdAttempt() throws Exception {
        OkHttpClient retryClient = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor(3, 50, Set.of(503)))
                .build();
        HttpClient rc = new HttpClient(retryClient, false);

        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setBody("ok"));

        HttpResponse resp = rc.request(url("flaky")).get();
        assertTrue(resp.isSuccessful());
        assertEquals(3, server.getRequestCount());
    }

    @Test
    void retry_exhausted() throws Exception {
        OkHttpClient retryClient = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor(2, 50, Set.of(503)))
                .build();
        HttpClient rc = new HttpClient(retryClient, false);

        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(503));

        HttpResponse resp = rc.request(url("fail")).get();
        assertFalse(resp.isSuccessful());
        assertEquals(503, resp.statusCode());
    }

    @Test
    void retry_disabled() throws Exception {
        OkHttpClient noRetry = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor(1, 0, Set.of(503)))
                .build();
        HttpClient rc = new HttpClient(noRetry, false);

        server.enqueue(new MockResponse().setResponseCode(503));
        rc.request(url("no-retry")).get();
        assertEquals(1, server.getRequestCount());
    }

    // ==================== HttpResponse ====================

    @Test
    void response_headerCaseInsensitive() {
        server.enqueue(new MockResponse().setBody("ok").setHeader("X-Test", "val"));
        HttpResponse resp = client.request(url("h")).get();

        assertEquals("val", resp.header("X-Test"));
        assertEquals("val", resp.header("x-test"));
    }

    @Test
    void response_failure() {
        HttpResponse resp = HttpResponse.failure("connection refused");
        assertEquals(-1, resp.statusCode());
        assertEquals("connection refused", resp.errorMessage());
        assertFalse(resp.isSuccessful());
    }

    // ==================== FormPart ====================

    @Test
    void formDataFile_ofPath() throws Exception {
        Path tmp = Files.createTempFile("test", ".txt");
        FormPart f = FormPart.of("doc", tmp);

        assertEquals("doc", f.fieldName());
        assertTrue(f.fileName().endsWith(".txt"));
        assertTrue(f instanceof FormPart.PathRef);
        assertNotNull(((FormPart.PathRef) f).path());
        Files.delete(tmp);
    }

    @Test
    void formDataFile_ofBytes() {
        FormPart f = FormPart.of("avatar", "a.png", new byte[]{1, 2, 3});

        assertEquals("avatar", f.fieldName());
        assertEquals("a.png", f.fileName());
        assertEquals("application/octet-stream", f.contentType());
    }

    // ==================== HttpClient Singleton ====================

    @Test
    void singleton_returnsSameInstance() {
        HttpClient a = HttpClient.getInstance();
        HttpClient b = HttpClient.getInstance();
        assertSame(a, b);
    }

    @Test
    void constructor_nullClient_throws() {
        assertThrows(IllegalArgumentException.class, () -> new HttpClient(null, false));
    }
}
