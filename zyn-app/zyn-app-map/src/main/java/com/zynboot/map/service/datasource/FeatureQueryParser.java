package com.zynboot.map.service.datasource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.util.JsonUtils;
import com.zynboot.map.infrastructure.entity.MapLayerField;
import com.zynboot.map.infrastructure.mapper.MapLayerFieldMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 解析 query JSON 字符串为 {@link FeatureQuery}，并校验字段白名单。
 * <ul>
 *   <li>filter 字段必须在 map_layer_field 中且 searchable=true</li>
 *   <li>sort 字段必须在 map_layer_field 中且 sortable=true</li>
 *   <li>值按字段 type 做类型转换</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class FeatureQueryParser {

    private final MapLayerFieldMapper layerFieldMapper;

    /**
     * 解析 query 参数。
     *
     * @param layerId 图层 ID（用于加载字段定义）
     * @param query   query 参数 JSON 字符串，可为 null/空
     * @return 解析后的 FeatureQuery（永不为 null）
     */
    public FeatureQuery parse(String layerId, String query) {
        if (query == null || query.isBlank()) {
            FeatureQuery empty = new FeatureQuery();
            empty.setFilter(Collections.emptyList());
            empty.setSort(Collections.emptyList());
            return empty;
        }

        FeatureQuery featureQuery;
        try {
            featureQuery = JsonUtils.mapper().readValue(query, FeatureQuery.class);
        } catch (Exception e) {
            throw BizException.badRequest("query 参数 JSON 格式错误: " + e.getMessage());
        }
        if (featureQuery.getFilter() == null) {
            featureQuery.setFilter(Collections.emptyList());
        }
        if (featureQuery.getSort() == null) {
            featureQuery.setSort(Collections.emptyList());
        }

        if (featureQuery.getFilter().isEmpty() && featureQuery.getSort().isEmpty()) {
            return featureQuery;
        }

        // 加载图层字段定义
        List<MapLayerField> fields = layerFieldMapper.selectList(
                new LambdaQueryWrapper<MapLayerField>().eq(MapLayerField::getLayerId, layerId));
        Map<String, MapLayerField> fieldMap = fields.stream()
                .collect(Collectors.toMap(MapLayerField::getName, f -> f, (a, b) -> a));

        // 校验 filter
        for (FeatureQuery.FilterCondition cond : featureQuery.getFilter()) {
            MapLayerField field = fieldMap.get(cond.getField());
            if (field == null) {
                throw BizException.badRequest("筛选字段不存在: " + cond.getField());
            }
            if (!Boolean.TRUE.equals(field.getSearchable())) {
                throw BizException.badRequest("字段不可筛选: " + cond.getField());
            }
            validateOp(cond.getOp());
            cond.setValue(castValue(field, cond.getValue()));
        }

        // 校验 sort
        for (FeatureQuery.SortCondition sort : featureQuery.getSort()) {
            MapLayerField field = fieldMap.get(sort.getField());
            if (field == null) {
                throw BizException.badRequest("排序字段不存在: " + sort.getField());
            }
            if (!Boolean.TRUE.equals(field.getSortable())) {
                throw BizException.badRequest("字段不可排序: " + sort.getField());
            }
            if (sort.getOrder() == null || (!"asc".equalsIgnoreCase(sort.getOrder())
                    && !"desc".equalsIgnoreCase(sort.getOrder()))) {
                throw BizException.badRequest("排序方向必须为 asc 或 desc: " + sort.getOrder());
            }
        }

        return featureQuery;
    }

    private static final Set<String> VALID_OPS = Set.of(
            "eq", "neq", "like", "gt", "gte", "lt", "lte", "in", "isnull", "notnull");

    private void validateOp(String op) {
        if (op == null || !VALID_OPS.contains(op.toLowerCase())) {
            throw BizException.badRequest("不支持的操作符: " + op + "，支持: " + VALID_OPS);
        }
    }

    /**
     * 将值统一转为字符串。
     * properties->>'field' 始终返回 text，所有比较操作均以字符串进行；
     * 数值比较（gt/gte/lt/lte）在 SQL 中通过 (properties->>'field')::numeric 转型，字符串值可正确解析。
     */
    private Object castValue(MapLayerField field, Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
