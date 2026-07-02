package com.zynboot.kit.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 通用树构建器：将任意列表映射为 {@link TreeNode} 列表并构建树。
 *
 * <pre>
 * List&lt;DeptTree&gt; tree = TreeBuilder.of(departments, Dept::getId, Dept::getParentId, Dept::getName)
 *     .codeMapper(Dept::getCode)
 *     .sortMapper(Dept::getSort)
 *     .rootId(0L)
 *     .customizer((dept, node) -&gt; node.putExtra("type", dept.getType()))
 *     .comparator(Trees.sortBySortThenLabel())
 *     .build();
 * </pre>
 *
 * @param <S> 源数据类型
 * @param <K> 节点 ID 类型
 */
public final class TreeBuilder<S, K> {

    private final List<S> source;
    private final Function<S, K> idMapper;
    private final Function<S, K> parentIdMapper;
    private final Function<S, String> labelMapper;
    private Function<S, String> codeMapper;
    private Function<S, Integer> sortMapper;
    private BiConsumer<S, TreeNode<K>> customizer;
    private Comparator<TreeNode<K>> comparator;
    private K rootId;
    private Predicate<TreeNode<K>> rootPredicate;

    private TreeBuilder(List<S> source,
                        Function<S, K> idMapper,
                        Function<S, K> parentIdMapper,
                        Function<S, String> labelMapper) {
        this.source = source;
        this.idMapper = idMapper;
        this.parentIdMapper = parentIdMapper;
        this.labelMapper = labelMapper;
    }

    /**
     * 创建构建器。
     *
     * @param source         源数据列表
     * @param idMapper       ID 提取函数
     * @param parentIdMapper 父 ID 提取函数
     * @param labelMapper    名称提取函数
     */
    public static <S, K> TreeBuilder<S, K> of(List<S> source,
                                               Function<S, K> idMapper,
                                               Function<S, K> parentIdMapper,
                                               Function<S, String> labelMapper) {
        return new TreeBuilder<>(source, idMapper, parentIdMapper, labelMapper);
    }

    /**
     * 快捷构建（使用默认参数）。
     */
    public static <S, K> List<TreeNode<K>> build(List<S> source,
                                                  Function<S, K> idMapper,
                                                  Function<S, K> parentIdMapper,
                                                  Function<S, String> labelMapper) {
        return of(source, idMapper, parentIdMapper, labelMapper).build();
    }

    /**
     * 设置编码提取函数。
     */
    public TreeBuilder<S, K> codeMapper(Function<S, String> codeMapper) {
        this.codeMapper = codeMapper;
        return this;
    }

    /**
     * 设置排序值提取函数。
     */
    public TreeBuilder<S, K> sortMapper(Function<S, Integer> sortMapper) {
        this.sortMapper = sortMapper;
        return this;
    }

    /**
     * 自定义节点（可扩展 ext 属性）。
     */
    public TreeBuilder<S, K> customizer(BiConsumer<S, TreeNode<K>> customizer) {
        this.customizer = customizer;
        return this;
    }

    /**
     * 设置排序比较器（构建后递归排序）。
     */
    public TreeBuilder<S, K> comparator(Comparator<TreeNode<K>> comparator) {
        this.comparator = comparator;
        return this;
    }

    /**
     * 设置根节点的 pid 值（pid 等于此值的节点作为根）。
     */
    public TreeBuilder<S, K> rootId(K rootId) {
        this.rootId = rootId;
        return this;
    }

    /**
     * 自定义根节点判断条件（优先级高于 rootId）。
     */
    public TreeBuilder<S, K> rootPredicate(Predicate<TreeNode<K>> rootPredicate) {
        this.rootPredicate = rootPredicate;
        return this;
    }

    /**
     * 构建树。
     */
    public List<TreeNode<K>> build() {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        List<TreeNode<K>> nodes = new ArrayList<>(source.size());
        for (S item : source) {
            TreeNode<K> node = new TreeNode<>();
            node.setId(idMapper.apply(item));
            node.setPid(parentIdMapper.apply(item));
            node.setLabel(labelMapper.apply(item));
            if (codeMapper != null) {
                node.setCode(codeMapper.apply(item));
            }
            if (sortMapper != null) {
                node.setSort(sortMapper.apply(item));
            }
            if (customizer != null) {
                customizer.accept(item, node);
            }
            nodes.add(node);
        }

        if (rootPredicate != null) {
            return Trees.build(nodes, rootPredicate, comparator);
        }
        return Trees.build(nodes, rootId, comparator);
    }
}
