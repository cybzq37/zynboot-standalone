package com.zynboot.infra.mybatis.interceptor;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zynboot.infra.mybatis.util.PageUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 分页参数拦截器。
 * <p>
 * 自动将 Mapper 方法中的 {@link Pageable} 参数转为 MyBatis-Plus {@link Page}，
 * 由 {@code PaginationInnerInterceptor} 完成物理分页后，再将结果转为 Spring Data {@link PageImpl}。
 * <p>
 * Mapper 方法签名示例：
 * <pre>
 * public interface UserMapper extends BaseMapper&lt;User&gt; {
 *     Page&lt;User&gt; selectPage(Pageable pageable, @Param("name") String name);
 * }
 * </pre>
 * <p>
 * 工作流程：
 * <ol>
 *   <li>检测参数中的 {@link Pageable}，替换为 MyBatis-Plus {@link Page}</li>
 *   <li>{@code PaginationInnerInterceptor} 识别 {@link IPage} 并执行物理分页</li>
 *   <li>将 {@link IPage} 结果转为 Spring Data {@link PageImpl}</li>
 * </ol>
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class,
                        ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class PageableInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        Object parameter = args[1];

        Pageable pageable = extractPageable(parameter);
        if (pageable == null) {
            return invocation.proceed();
        }

        // 创建 MyBatis-Plus Page 并替换参数中的 Pageable
        Page<Object> mpPage = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        replacePageable(parameter, mpPage);

        // PaginationInnerInterceptor 会识别参数中的 IPage 并执行物理分页
        Object result = invocation.proceed();

        // 将 IPage 结果转为 Spring Data Page
        if (result instanceof IPage<?> iPage) {
            return toSpringPage(iPage);
        }

        // 兜底：从 mpPage 获取结果
        if (mpPage.getTotal() > 0 || (mpPage.getRecords() != null && !mpPage.getRecords().isEmpty())) {
            return toSpringPage(mpPage);
        }

        return result;
    }

    /**
     * 将参数 Map 中的 Pageable 替换为 Page，
     * 同时放入 "page" key 供 PaginationInnerInterceptor 发现。
     */
    @SuppressWarnings("unchecked")
    private void replacePageable(Object parameter, Page<Object> mpPage) {
        if (!(parameter instanceof Map)) {
            return;
        }
        Map<String, Object> paramMap = (Map<String, Object>) parameter;

        // 替换 "pageable" key 中的 Pageable → Page
        if (paramMap.get("pageable") instanceof Pageable) {
            paramMap.put("pageable", mpPage);
        }

        // 遍历替换其他位置的 Pageable
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            if (entry.getValue() instanceof Pageable && !"pageable".equals(entry.getKey())) {
                paramMap.put(entry.getKey(), mpPage);
            }
        }

        // 确保 PaginationInnerInterceptor 能找到 IPage
        paramMap.put("page", mpPage);
    }

    @SuppressWarnings("unchecked")
    private org.springframework.data.domain.Page<Object> toSpringPage(IPage<?> iPage) {
        return (org.springframework.data.domain.Page<Object>) PageUtils.toSpringPage(iPage);
    }

    private Pageable extractPageable(Object parameter) {
        if (parameter instanceof Pageable pageable) {
            return pageable;
        }
        if (parameter instanceof Map) {
            return findPageableInMap((Map<?, ?>) parameter);
        }
        return null;
    }

    private Pageable findPageableInMap(Map<?, ?> map) {
        Object val = map.get("pageable");
        if (val instanceof Pageable pageable) {
            return pageable;
        }
        for (Object v : map.values()) {
            if (v instanceof Pageable pageable) {
                return pageable;
            }
        }
        return null;
    }
}
