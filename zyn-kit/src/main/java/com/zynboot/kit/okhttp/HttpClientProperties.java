package com.zynboot.kit.okhttp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ConfigurationProperties(prefix = "zyn.kit.okhttp")
public class HttpClientProperties {

    public static final long DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
    public static final long DEFAULT_READ_TIMEOUT_SECONDS = 10;
    public static final long DEFAULT_WRITE_TIMEOUT_SECONDS = 10;
    public static final long DEFAULT_CALL_TIMEOUT_SECONDS = 30;

    /** 建立连接超时（秒）。 */
    private long connectTimeoutSeconds = DEFAULT_CONNECT_TIMEOUT_SECONDS;

    /** 读取响应超时（秒）。 */
    private long readTimeoutSeconds = DEFAULT_READ_TIMEOUT_SECONDS;

    /** 发送请求超时（秒）。 */
    private long writeTimeoutSeconds = DEFAULT_WRITE_TIMEOUT_SECONDS;

    /** 单次请求总超时（秒）。 */
    private long callTimeoutSeconds = DEFAULT_CALL_TIMEOUT_SECONDS;

    /** 连接池最大空闲连接数。 */
    private int maxConnections = 50;

    /** 空闲连接存活时间（秒）。 */
    private long keepAliveSeconds = 60;

    /** 请求体最大字节数，超过拦截器拒绝（0=不限制）。 */
    private long maxRequestBytes = 0;

    /** 是否跟随重定向（微服务内部一般 false）。 */
    private boolean followRedirects = false;

    /** 全局默认请求头（所有请求自动附加）。 */
    private Map<String, String> defaultHeaders = new HashMap<>();

    /** 是否在 HTTP 非 2xx 或 IO 异常时抛出异常。 */
    private boolean throwOnHttpError = false;

    /** 是否启用 OkHttp 请求日志拦截器。 */
    private boolean enableLogInterceptor = false;

    /** 日志中请求/响应体最大显示字节数。 */
    private int logMaxBodyBytes = 8192;

    /** 日志中请求/响应体最大显示字符数。 */
    private int logMaxBodyChars = 8192;

    /** 重试配置。 */
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Retry {
        /** 最大重试次数（含首次请求）。 */
        private int maxAttempts = 1;

        /** 重试退避基数（毫秒），实际等待 = backoffMs * attempt。 */
        private long backoffMs = 1000;

        /** 触发重试的 HTTP 状态码。 */
        private Set<Integer> retryOnStatusCodes = Set.of(502, 503, 504);
    }
}
