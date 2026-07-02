package com.zynboot.kit.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用树节点。
 * <p>
 * {@code T} 为节点 ID 类型（如 Long、String、Integer）。
 * 核心字段 {@code id}/{@code pid}/{@code children} 驱动树结构，
 * 其余字段为通用业务属性，也可通过 {@link #ext} 透传任意数据。
 *
 * <pre>
 * TreeNode&lt;Long&gt; node = new TreeNode&lt;&gt;(1L, 0L, "根节点");
 * node.setCode("root");
 * node.putExtra("icon", "home");
 * </pre>
 */
public class TreeNode<T> {

    private T id;
    private T pid;
    private String code;
    private String label;
    private Integer sort;
    private final Map<String, Object> ext = new LinkedHashMap<>();
    private List<TreeNode<T>> children = new ArrayList<>();

    public TreeNode() {
    }

    public TreeNode(T id, T pid, String label) {
        this.id = id;
        this.pid = pid;
        this.label = label;
    }

    // ==================== children 操作 ====================

    public void addChild(TreeNode<T> child) {
        if (child != null) {
            ensureChildren().add(child);
        }
    }

    public void addChildren(List<TreeNode<T>> children) {
        if (children != null && !children.isEmpty()) {
            ensureChildren().addAll(children);
        }
    }

    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    /**
     * 确保 children 列表已初始化。
     */
    private List<TreeNode<T>> ensureChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

    // ==================== ext 操作 ====================

    /**
     * 添加扩展属性。
     */
    public TreeNode<T> putExtra(String key, Object value) {
        ext.put(key, value);
        return this;
    }

    /**
     * 批量添加扩展属性。
     */
    public TreeNode<T> putAllExtra(Map<String, Object> extra) {
        if (extra != null) {
            ext.putAll(extra);
        }
        return this;
    }

    /**
     * 获取扩展属性。
     */
    @SuppressWarnings("unchecked")
    public <V> V getExtra(String key) {
        return (V) ext.get(key);
    }

    /**
     * 获取扩展属性，不存在时返回默认值。
     */
    @SuppressWarnings("unchecked")
    public <V> V getExtra(String key, V defaultValue) {
        Object value = ext.get(key);
        return value != null ? (V) value : defaultValue;
    }

    // ==================== getter/setter ====================

    public T getId() {
        return id;
    }

    public void setId(T id) {
        this.id = id;
    }

    public T getPid() {
        return pid;
    }

    public void setPid(T pid) {
        this.pid = pid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Map<String, Object> getExt() {
        return Collections.unmodifiableMap(ext);
    }

    public void setExt(Map<String, Object> ext) {
        this.ext.clear();
        if (ext != null) {
            this.ext.putAll(ext);
        }
    }

    public List<TreeNode<T>> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode<T>> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "TreeNode{id=" + id + ", pid=" + pid + ", label=" + label + "}";
    }
}
