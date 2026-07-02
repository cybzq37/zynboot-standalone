package com.zynboot.infra.mybatis.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

/**
 * MyBatis-Plus 分页与 Spring Data 分页互转工具。
 * <p>
 * 统一使用 Spring Data 的 {@link Pageable} 作为入参、{@link org.springframework.data.domain.Page} 作为返回值，
 * 屏蔽 MyBatis-Plus 分页细节。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageUtils {

    /**
     * 将 Spring Data {@link Pageable} 转为 MyBatis-Plus {@link Page}。
     *
     * @param pageable Spring Data 分页参数，null 时返回第一页、不限大小
     * @return MyBatis-Plus 分页对象
     */
    public static <T> Page<T> toMybatisPage(Pageable pageable) {
        if (pageable == null) {
            return new Page<>(1, -1);
        }
        return new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    /**
     * 将 MyBatis-Plus {@link IPage} 转为 Spring Data {@link PageImpl}。
     *
     * @param mybatisPage MyBatis-Plus 分页查询结果
     * @return Spring Data 分页对象
     */
    public static <T> org.springframework.data.domain.Page<T> toSpringPage(IPage<T> mybatisPage) {
        if (mybatisPage == null) {
            return new PageImpl<>(Collections.emptyList());
        }
        List<T> content = mybatisPage.getRecords();
        if (content == null) {
            content = Collections.emptyList();
        }
        int pageNumber = (int) (mybatisPage.getCurrent() - 1);
        int pageSize = (int) mybatisPage.getSize();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(pageNumber, 0),
                Math.max(pageSize, 1)
        );
        return new PageImpl<>(content, pageable, mybatisPage.getTotal());
    }

    /**
     * 一步完成：将 {@link Pageable} 转为 MyBatis-Plus Page 执行查询，再转回 Spring Data Page。
     * <pre>
     * return PageUtils.toSpringPage(pageable, mpPage -> mpMapper.selectPage(mpPage, queryWrapper));
     * </pre>
     *
     * @param pageable Spring Data 分页参数
     * @param query    查询函数，接收 MyBatis-Plus Page，返回 IPage 结果
     * @return Spring Data 分页对象
     */
    public static <T> org.springframework.data.domain.Page<T> toSpringPage(
            Pageable pageable,
            java.util.function.Function<Page<T>, IPage<T>> query) {
        Page<T> mpPage = toMybatisPage(pageable);
        IPage<T> result = query.apply(mpPage);
        return toSpringPage(result);
    }
}
