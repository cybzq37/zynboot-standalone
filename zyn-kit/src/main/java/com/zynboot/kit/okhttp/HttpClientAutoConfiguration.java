package com.zynboot.kit.okhttp;

import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@AutoConfiguration
@EnableConfigurationProperties(HttpClientProperties.class)
public class HttpClientAutoConfiguration {

    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean
    public OkHttpClient okHttpClient(HttpClientProperties props,
                                     ObjectProvider<Interceptor> customInterceptors) {
        ConnectionPool pool = new ConnectionPool(
                props.getMaxConnections(),
                props.getKeepAliveSeconds(),
                TimeUnit.SECONDS
        );

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(props.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(props.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(props.getWriteTimeoutSeconds(), TimeUnit.SECONDS)
                .callTimeout(props.getCallTimeoutSeconds(), TimeUnit.SECONDS)
                .followRedirects(props.isFollowRedirects())
                .connectionPool(pool);

        // 请求体大小限制拦截器
        if (props.getMaxRequestBytes() > 0) {
            builder.addInterceptor(new MaxRequestSizeInterceptor(props.getMaxRequestBytes()));
        }

        // 全局默认请求头拦截器
        if (!props.getDefaultHeaders().isEmpty()) {
            builder.addInterceptor(chain -> {
                Request.Builder rb = chain.request().newBuilder();
                props.getDefaultHeaders().forEach(rb::header);
                return chain.proceed(rb.build());
            });
        }

        // 重试拦截器（最先执行）
        HttpClientProperties.Retry retry = props.getRetry();
        if (retry.getMaxAttempts() > 1) {
            builder.addInterceptor(new RetryInterceptor(
                    retry.getMaxAttempts(),
                    retry.getBackoffMs(),
                    retry.getRetryOnStatusCodes()
            ));
        }

        // 日志拦截器
        if (props.isEnableLogInterceptor()) {
            builder.addInterceptor(new LoggingInterceptor(
                    props.getLogMaxBodyBytes(),
                    props.getLogMaxBodyChars()
            ));
        }

        // 自定义拦截器
        customInterceptors.orderedStream().forEach(builder::addInterceptor);

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpClient httpClient(OkHttpClient okHttpClient, HttpClientProperties props) {
        return new HttpClient(okHttpClient, props.isThrowOnHttpError());
    }
}
