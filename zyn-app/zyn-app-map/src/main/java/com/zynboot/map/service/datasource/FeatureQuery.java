package com.zynboot.map.service.datasource;

import lombok.Data;

import java.util.List;

/**
 * 要素查询条件（filter + sort），参考 Elasticsearch DSL 风格。
 * <p>
 * JSON 示例：
 * <pre>
 * {
 *   "filter": [
 *     {"field": "name", "op": "like", "value": "天安门"},
 *     {"field": "code", "op": "eq", "value": "001"}
 *   ],
 *   "sort": [
 *     {"field": "name", "order": "asc"}
 *   ]
 * }
 * </pre>
 */
@Data
public class FeatureQuery {

    private List<FilterCondition> filter;
    private List<SortCondition> sort;

    @Data
    public static class FilterCondition {
        /** 字段名（必须 map_layer_field.searchable=true） */
        private String field;
        /** 操作符：eq/neq/like/gt/gte/lt/lte/in/isnull/notnull */
        private String op;
        /** 值（JSON 原生类型） */
        private Object value;
    }

    @Data
    public static class SortCondition {
        /** 字段名（必须 map_layer_field.sortable=true） */
        private String field;
        /** 排序方向：asc/desc */
        private String order;
    }
}
