package com.zynboot.infra.web.version;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API 版本自动配置：通过 Filter 将 /v{version}/ 前缀路由重写为无版本路径。
 *
 * <p>客户端访问 {@code /v1/map/layer} 时，Filter 将请求内部 forward 到
 * {@code /map/layer}，匹配 {@code @RequestMapping("/map/layer")} 的 Controller。
 * {@link ApiVersion} 注解在此模式下仅作文档/元数据标记。
 *
 * <p>可通过 {@code zyn.web.api-version.enabled=false} 关闭。
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "zyn.web.api-version", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ApiVersionAutoConfiguration {

    private static final Pattern VERSION_PATH = Pattern.compile("^/v(\\d+)(/.*)");

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> apiVersionFilter() {
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                String uri = request.getRequestURI();
                Matcher m = VERSION_PATH.matcher(uri);
                if (m.matches()) {
                    String version = m.group(1);
                    String path = m.group(2);
                    request.setAttribute("apiVersion", version);
                    request.getRequestDispatcher(path).forward(request, response);
                } else {
                    chain.doFilter(request, response);
                }
            }
        };
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(-100); // 在其他 Filter 之前执行
        registration.addUrlPatterns("/*");
        return registration;
    }
}
