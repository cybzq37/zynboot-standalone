package com.zynboot.infra.web.version;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记控制器或单个方法的 API 版本号，方法级优先于类级。
 *
 * <p>由 {@link VersionedRequestMappingHandlerMapping} 在注册时自动为
 * 路径加上 {@code /v}<i>version</i> 前缀。
 *
 * <h4>使用示例</h4>
 * <pre>
 * // 类级别：整个 Controller 的所有方法都加 /v1 前缀
 * @ApiVersion("1")
 * @RequestMapping("/map/layer")
 * public class LayerController { ... }
 *
 * // 方法级别：仅该方法加 /v2 前缀（覆盖类级别的 /v1）
 * @ApiVersion("2")
 * @GetMapping("/search")
 * public ApiResponse<?> search() { ... }
 * </pre>
 *
 * <p>可通过 {@code zyn.web.api-version.enabled=false} 关闭全局版本前缀。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {

    String value();
}
