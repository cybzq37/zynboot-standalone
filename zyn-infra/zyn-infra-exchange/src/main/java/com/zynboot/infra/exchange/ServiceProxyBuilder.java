package com.zynboot.infra.exchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * HttpExchange 客户端构建工具。
 */
public final class ServiceProxyBuilder {

    private ServiceProxyBuilder() {
    }

    public static <T> T build(Class<T> serviceType, String baseUrl,
                              long connectTimeoutMs, long readTimeoutMs,
                              boolean followRedirects, boolean forwardAuth) {
        return build(serviceType, baseUrl, connectTimeoutMs, readTimeoutMs, followRedirects, forwardAuth, List.of());
    }

    public static <T> T build(Class<T> serviceType, String baseUrl,
                              long connectTimeoutMs, long readTimeoutMs,
                              boolean followRedirects, boolean forwardAuth,
                              List<HttpServiceArgumentResolver> argumentResolvers) {
        return build(serviceType, baseUrl, connectTimeoutMs, readTimeoutMs, followRedirects, forwardAuth, argumentResolvers, null);
    }

    public static <T> T build(Class<T> serviceType, String baseUrl,
                              long connectTimeoutMs, long readTimeoutMs,
                              boolean followRedirects, boolean forwardAuth,
                              List<HttpServiceArgumentResolver> argumentResolvers,
                              ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(followRedirects
                        ? HttpClient.Redirect.NORMAL
                        : HttpClient.Redirect.NEVER)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        RestClient.Builder clientBuilder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory);

        if (forwardAuth) {
            clientBuilder.requestInterceptor((request, body, execution) -> {
                var attrs = RequestContextHolder.getRequestAttributes();
                if (attrs instanceof ServletRequestAttributes servletAttrs) {
                    String auth = servletAttrs.getRequest().getHeader("Authorization");
                    if (auth != null && !auth.isBlank()) {
                        request.getHeaders().set("Authorization", auth);
                    }
                }
                return execution.execute(request, body);
            });
        }

        // 某些上游 API 返回 JSON 内容但 Content-Type 为 text/plain，需要扩展 Jackson 转换器的支持类型。
        if (objectMapper != null) {
            MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
            List<MediaType> supportedTypes = new java.util.ArrayList<>(jacksonConverter.getSupportedMediaTypes());
            supportedTypes.add(MediaType.TEXT_PLAIN);
            jacksonConverter.setSupportedMediaTypes(supportedTypes);
            clientBuilder.messageConverters(converters -> {
                converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                converters.add(jacksonConverter);
            });
        }

        RestClient client = clientBuilder.build();

        var factoryBuilder = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client));

        for (HttpServiceArgumentResolver resolver : argumentResolvers) {
            factoryBuilder.customArgumentResolver(resolver);
        }

        return factoryBuilder.build().createClient(serviceType);
    }
}
