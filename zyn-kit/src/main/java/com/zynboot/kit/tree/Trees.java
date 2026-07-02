package com.zynboot.kit.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 树结构工具类。
 * <p>
 * 提供构建、扁平化、搜索、路径查找、剪枝等通用树操作。
 */
public final class Trees {

    private Trees() {
    }

    // ==================== 构建树 ====================

    /**
     * 构建树，pid 为 null 的节点视为根节点。
     */
    public static <T> List<TreeNode<T>> build(List<TreeNode<T>> nodes) {
        return build(nodes, (Predicate<TreeNode<T>>) null, null);
    }

    /**
     * 构建树，指定根节点 ID。
     */
    public static <T> List<TreeNode<T>> build(List<TreeNode<T>> nodes, T rootId) {
        return build(nodes, rootId, null);
    }

    /**
     * 构建树，指定根节点 ID 和排序比较器。
     */
    public static <T> List<TreeNode<T>> build(List<TreeNode<T>> nodes, T rootId, Comparator<TreeNode<T>> comparator) {
        Predicate<TreeNode<T>> predicate = node -> isRoot(node.getPid(), rootId);
        return build(nodes, predicate, comparator);
    }

    /**
     * 构建树，使用自定义根节点判断条件。
     */
    public static <T> List<TreeNode<T>> build(List<TreeNode<T>> nodes, Predicate<TreeNode<T>> rootPredicate) {
        return build(nodes, rootPredicate, null);
    }

    /**
     * 构建树，使用自定义根节点判断条件和排序比较器。
     */
    public static <T> List<TreeNode<T>> build(List<TreeNode<T>> nodes, Predicate<TreeNode<T>> rootPredicate, Comparator<TreeNode<T>> comparator) {
        if (nodes == null || nodes.isEmpty()) return Collections.emptyList();

        Map<T, TreeNode<T>> map = new LinkedHashMap<>(nodes.size());
        for (TreeNode<T> node : nodes) {
            if (node == null) continue;
            if (node.getChildren() == null) node.setChildren(new ArrayList<>());
            map.put(node.getId(), node);
        }

        Predicate<TreeNode<T>> isRoot = rootPredicate != null
                ? rootPredicate
                : node -> isRoot(node.getPid(), null);

        List<TreeNode<T>> roots = new ArrayList<>();
        for (TreeNode<T> node : nodes) {
            if (node == null) continue;
            if (isRoot.test(node)) {
                roots.add(node);
            } else {
                TreeNode<T> parent = map.get(node.getPid());
                if (parent != null) {
                    parent.addChild(node);
                } else {
                    roots.add(node);
                }
            }
        }

        if (comparator != null) sortRecursively(roots, comparator);
        return roots;
    }

    // ==================== 扁平化 ====================

    /**
     * 将树结构扁平化为列表（深度优先遍历）。
     */
    public static <T> List<TreeNode<T>> flatten(List<TreeNode<T>> tree) {
        if (tree == null || tree.isEmpty()) return Collections.emptyList();
        List<TreeNode<T>> result = new ArrayList<>();
        flattenRecursively(tree, result);
        return result;
    }

    // ==================== 搜索 ====================

    /**
     * 在树中查找指定 ID 的节点。
     */
    public static <T> TreeNode<T> findById(List<TreeNode<T>> tree, T id) {
        if (tree == null || tree.isEmpty() || id == null) return null;
        return findByIdRecursively(tree, id);
    }

    /**
     * 在树中查找指定 code 的节点。
     */
    public static <T> TreeNode<T> findByCode(List<TreeNode<T>> tree, String code) {
        if (tree == null || tree.isEmpty() || code == null) return null;
        return findByCodeRecursively(tree, code);
    }

    /**
     * 在树中查找所有满足条件的节点。
     */
    public static <T> List<TreeNode<T>> find(List<TreeNode<T>> tree, Predicate<TreeNode<T>> predicate) {
        if (tree == null || tree.isEmpty()) return Collections.emptyList();
        List<TreeNode<T>> result = new ArrayList<>();
        findRecursively(tree, predicate, result);
        return result;
    }

    /**
     * 查找从根到指定节点的路径。
     */
    public static <T> List<TreeNode<T>> findPath(List<TreeNode<T>> tree, T id) {
        if (tree == null || tree.isEmpty() || id == null) return Collections.emptyList();
        List<TreeNode<T>> path = new ArrayList<>();
        if (findPathRecursively(tree, id, path)) return path;
        return Collections.emptyList();
    }

    // ==================== 排序 ====================

    /**
     * 按 sort 字段升序排列，sort 相同时按 label 排序。
     */
    public static <T> Comparator<TreeNode<T>> sortBySortThenLabel() {
        return Comparator.comparing(TreeNode<T>::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(TreeNode<T>::getLabel, Comparator.nullsLast(String::compareTo));
    }

    /**
     * 对树递归排序。
     */
    public static <T> void sort(List<TreeNode<T>> tree, Comparator<TreeNode<T>> comparator) {
        if (tree == null || tree.isEmpty() || comparator == null) return;
        sortRecursively(tree, comparator);
    }

    // ==================== 剪枝 ====================

    /**
     * 剪枝：保留满足条件的节点及其祖先路径。
     * <p>
     * 不满足条件且无满足条件的后代的节点将被移除。
     *
     * @param tree      树
     * @param predicate 保留条件
     * @return 剪枝后的树
     */
    public static <T> List<TreeNode<T>> prune(List<TreeNode<T>> tree, Predicate<TreeNode<T>> predicate) {
        if (tree == null || tree.isEmpty()) return Collections.emptyList();
        List<TreeNode<T>> result = new ArrayList<>();
        for (TreeNode<T> node : tree) {
            if (node == null) continue;
            TreeNode<T> pruned = pruneNode(node, predicate);
            if (pruned != null) result.add(pruned);
        }
        return result;
    }

    // ==================== 统计 ====================

    /**
     * 统计树的总节点数。
     */
    public static <T> int count(List<TreeNode<T>> tree) {
        if (tree == null || tree.isEmpty()) return 0;
        return countRecursively(tree);
    }

    /**
     * 获取树的最大深度。
     */
    public static <T> int maxDepth(List<TreeNode<T>> tree) {
        if (tree == null || tree.isEmpty()) return 0;
        return maxDepthRecursively(tree, 0);
    }

    /**
     * 获取单个节点的深度（从 1 开始）。
     */
    public static <T> int depth(List<TreeNode<T>> tree, T id) {
        if (tree == null || tree.isEmpty() || id == null) return 0;
        List<TreeNode<T>> path = findPath(tree, id);
        return path.isEmpty() ? 0 : path.size();
    }

    // ==================== 转换 ====================

    /**
     * 树转 Map（id → TreeNode），用于快速查找。
     */
    public static <T> Map<T, TreeNode<T>> toMap(List<TreeNode<T>> tree) {
        if (tree == null || tree.isEmpty()) return Collections.emptyMap();
        Map<T, TreeNode<T>> map = new LinkedHashMap<>();
        toMapRecursively(tree, map);
        return map;
    }

    // ==================== 内部实现 ====================

    private static <T> boolean isRoot(T pid, T rootId) {
        if (pid == null) return true;
        return rootId != null && Objects.equals(pid, rootId);
    }

    private static <T> void flattenRecursively(List<TreeNode<T>> nodes, List<TreeNode<T>> result) {
        for (TreeNode<T> node : nodes) {
            if (node == null) continue;
            result.add(node);
            List<TreeNode<T>> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                flattenRecursively(children, result);
            }
        }
    }

    private static <T> TreeNode<T> findByIdRecursively(List<TreeNode<T>> nodes, T id) {
        for (TreeNode<T> node : nodes) {
            if (node == null) continue;
            if (Objects.equals(node.getId(), id)) return node;
            List<TreeNode<T>> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                TreeNode<T> found = findByIdRecursively(children, id);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T> TreeNode<T> findByCodeRecursively(List<TreeNode<T>> nodes, String code) {
        for (TreeNode<T> node : nodes) {
            if (node == null) continue;
            if (code.equals(node.getCode())) return node;
            List<TreeNode<T>> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                TreeNode<T> found = findByCodeRecursively(children, code);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T> void findRecursively(List<TreeNode<T>> nodes, Predicate<TreeNode<T>> predicate, List<TreeNode<T>> result) {
        for (TreeNode<T> node : nodes) {
            if (node == null) continue;
            if (predicate.test(node)) result.add(node);
            List<TreeNode<T>> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                findRecursively(children, predicate, result);
            }
        }
    }

    private static <T> boolean findPathRecursively(List<TreeNode<T>> nodes, T id, List<TreeNode<T>> path) {
        for (TreeNode<T> node : nodes) {
            if (node == null) continue;
            path.add(node);
            if (Objects.equals(node.getId(), id)) return true;
            List<TreeNode<T>> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                if (findPathRecursively(children, id, path)) return true;
            }
            path.remove(path.size() - 1);
        }
        return false;
    }

    private static <T> void sortRecursively(List<TreeNode<T>> nodes, Comparator<TreeNode<T>> comparator) {
        nodes.sort(comparator);
        for (TreeNode<T> node : nodes) {
            List<TreeNode<T>> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                sortRecursively(children, comparator);
            }
        }
    }

    private static <T> int countRecursively(List<TreeNode<T>> nodes) {
        int count = 0;
        for (TreeNode<T> node : nodes) {
            if (node == null) continue;
            count++;
            List<TreeNode<T>> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                count += countRecursively(children);
            }
        }
        return count;
    }

    private static <T> int maxDepthRecursively(List<TreeNode<T>> nodes, int currentDepth) {
        int maxChildDepth = currentDepth;
        for (TreeNode<T> node : nodes) {
            if (node == null) continue;
            List<TreeNode<T>> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                int childDepth = maxDepthRecursively(children, currentDepth + 1);
                maxChildDepth = Math.max(maxChildDepth, childDepth);
            }
        }
        return maxChildDepth;
    }

    private static <T> TreeNode<T> pruneNode(TreeNode<T> node, Predicate<TreeNode<T>> predicate) {
        List<TreeNode<T>> children = node.getChildren();
        List<TreeNode<T>> prunedChildren = new ArrayList<>();
        if (children != null) {
            for (TreeNode<T> child : children) {
                if (child == null) continue;
                TreeNode<T> prunedChild = pruneNode(child, predicate);
                if (prunedChild != null) prunedChildren.add(prunedChild);
            }
        }
        boolean selfMatch = predicate.test(node);
        if (selfMatch || !prunedChildren.isEmpty()) {
            node.setChildren(prunedChildren);
            return node;
        }
        return null;
    }

    private static <T> void toMapRecursively(List<TreeNode<T>> nodes, Map<T, TreeNode<T>> map) {
        for (TreeNode<T> node : nodes) {
            if (node == null) continue;
            map.put(node.getId(), node);
            List<TreeNode<T>> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                toMapRecursively(children, map);
            }
        }
    }
}
