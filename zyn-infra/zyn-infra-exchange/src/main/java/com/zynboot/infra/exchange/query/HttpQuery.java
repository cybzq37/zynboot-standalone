package com.zynboot.infra.exchange.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 HTTP 接口方法参数，将 POJO 展开为 URL 查询参数。
 * <p>
 * Spring 6.x 的 HttpServiceProxyFactory 不支持 POJO 自动展开为 query params，
 * 此注解配合 {@link HttpQueryArgumentResolver} 使用。
 * Spring 7.1+ 将原生支持此功能（#32142），届时可移除。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpQuery {
}
