package com.zynboot.kit.okhttp;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

/**
 * OkHttp 重试拦截器，对超时和指定状态码自动重试。
 */
public class RetryInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(RetryInterceptor.class);

    private final int maxAttempts;
    private final long backoffMs;
    private final Set<Integer> retryOnStatusCodes;

    public RetryInterceptor(int maxAttempts, long backoffMs, Set<Integer> retryOnStatusCodes) {
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;
        this.retryOnStatusCodes = retryOnStatusCodes;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        IOException lastException = null;
        Response response = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (response != null) {
                    response.close();
                }
                response = chain.proceed(request);

                if (response.isSuccessful() || !retryOnStatusCodes.contains(response.code()) || attempt == maxAttempts) {
                    return response;
                }

                log.warn("retrying request attempt={}/{} status={} url={}",
                        attempt, maxAttempts, response.code(), request.url());
                sleep(backoffMs * attempt);

            } catch (IOException e) {
                lastException = e;
                if (attempt == maxAttempts) {
                    throw e;
                }
                log.warn("retrying request attempt={}/{} error={} url={}",
                        attempt, maxAttempts, e.getMessage(), request.url());
                sleep(backoffMs * attempt);
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        return response;
    }

    private void sleep(long ms) throws IOException {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("retry interrupted", e);
        }
    }
}
