package com.zynboot.map.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.util.IdUtils;
import com.zynboot.map.command.basemap.BasemapSaveCmd;
import com.zynboot.map.command.layer.LayerFieldItemCmd;
import com.zynboot.map.command.layer.LayerStyleSaveCmd;
import com.zynboot.map.domain.repository.LayerRepository;
import com.zynboot.map.infrastructure.entity.MapBasemap;
import com.zynboot.map.infrastructure.entity.MapLayerField;
import com.zynboot.map.infrastructure.entity.MapLayerStyle;
import com.zynboot.map.infrastructure.mapper.MapBasemapMapper;
import com.zynboot.map.infrastructure.mapper.MapLayerFieldMapper;
import com.zynboot.map.infrastructure.mapper.MapLayerStyleMapper;
import com.zynboot.map.response.basemap.BasemapRes;
import com.zynboot.map.response.layer.LayerFieldRes;
import com.zynboot.map.response.layer.LayerStyleRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapMetadataService {

    private final MapBasemapMapper basemapMapper;
    private final MapLayerFieldMapper layerFieldMapper;
    private final MapLayerStyleMapper layerStyleMapper;
    private final LayerRepository layerRepository;

    public List<BasemapRes> listBasemaps() {
        return basemapMapper.selectList(new LambdaQueryWrapper<MapBasemap>().orderByAsc(MapBasemap::getSortOrder))
                .stream().map(this::toBasemapRes).toList();
    }

    public BasemapRes getDefaultBasemap() {
        MapBasemap basemap = basemapMapper.selectOne(
                new LambdaQueryWrapper<MapBasemap>().eq(MapBasemap::getIsDefault, true));
        if (basemap == null) throw BizException.notFound("默认底图");
        return toBasemapRes(basemap);
    }

    @Transactional
    public BasemapRes createBasemap(BasemapSaveCmd cmd) {
        MapBasemap basemap = new MapBasemap();
        basemap.setId(IdUtils.uuid());
        applyBasemap(basemap, cmd);
        basemapMapper.insert(basemap);
        return toBasemapRes(basemapMapper.selectById(basemap.getId()));
    }

    @Transactional
    public BasemapRes updateBasemap(String id, BasemapSaveCmd cmd) {
        MapBasemap basemap = requireBasemap(id);
        applyBasemap(basemap, cmd);
        basemapMapper.updateById(basemap);
        return toBasemapRes(basemapMapper.selectById(id));
    }

    @Transactional
    public void deleteBasemap(String id) {
        requireBasemap(id);
        basemapMapper.deleteById(id);
    }

    public List<LayerFieldRes> listFields(String layerId) {
        requireLayer(layerId);
        return layerFieldMapper.selectList(new LambdaQueryWrapper<MapLayerField>()
                        .eq(MapLayerField::getLayerId, layerId)
                        .orderByAsc(MapLayerField::getSortOrder))
                .stream().map(this::toLayerFieldRes).toList();
    }

    @Transactional
    public List<LayerFieldRes> createFields(String layerId, List<LayerFieldItemCmd> items) {
        requireLayer(layerId);
        if (items == null || items.isEmpty()) {
            throw BizException.badRequest("字段列表不能为空");
        }
        // 校验：新增时 name/type 必填
        for (int i = 0; i < items.size(); i++) {
            LayerFieldItemCmd item = items.get(i);
            if (item.getName() == null || item.getName().isBlank()) {
                throw BizException.badRequest("第 " + (i + 1) + " 个字段缺少 name");
            }
            if (item.getType() == null || item.getType().isBlank()) {
                throw BizException.badRequest("第 " + (i + 1) + " 个字段缺少 type");
            }
        }
        for (LayerFieldItemCmd item : items) {
            MapLayerField field = new MapLayerField();
            field.setId(IdUtils.uuid());
            field.setLayerId(layerId);
            applyFieldItem(field, item);
            layerFieldMapper.insert(field);
        }
        return listFields(layerId);
    }

    @Transactional
    public List<LayerFieldRes> updateFields(String layerId, List<LayerFieldItemCmd> items) {
        requireLayer(layerId);
        if (items == null || items.isEmpty()) {
            throw BizException.badRequest("字段列表不能为空");
        }
        // 校验：更新时每个对象必须含 id
        for (int i = 0; i < items.size(); i++) {
            LayerFieldItemCmd item = items.get(i);
            if (item.getId() == null || item.getId().isBlank()) {
                throw BizException.badRequest("第 " + (i + 1) + " 个字段缺少 id");
            }
        }
        // 加载该图层现有字段
        List<MapLayerField> existing = layerFieldMapper.selectList(
                new LambdaQueryWrapper<MapLayerField>().eq(MapLayerField::getLayerId, layerId));
        Map<String, MapLayerField> existingById = existing.stream()
                .collect(Collectors.toMap(MapLayerField::getId, Function.identity()));
        // 部分更新：仅非 null 字段被写入
        for (LayerFieldItemCmd item : items) {
            MapLayerField field = existingById.get(item.getId());
            if (field == null) {
                throw BizException.badRequest("字段 id 不存在或不属于该图层: " + item.getId());
            }
            applyFieldItemPartial(field, item);
            layerFieldMapper.updateById(field);
        }
        return listFields(layerId);
    }

    @Transactional
    public void deleteField(String id) {
        MapLayerField field = layerFieldMapper.selectById(id);
        if (field == null) {
            throw BizException.notFound("字段");
        }
        layerFieldMapper.deleteById(id);
    }

    @Transactional
    public void deleteFields(String layerId, List<String> ids) {
        requireLayer(layerId);
        if (ids == null || ids.isEmpty()) {
            throw BizException.badRequest("id 列表不能为空");
        }
        // 仅删除属于该图层的字段，防止越权删除其他图层字段
        layerFieldMapper.delete(new LambdaQueryWrapper<MapLayerField>()
                .eq(MapLayerField::getLayerId, layerId)
                .in(MapLayerField::getId, ids));
    }

    public List<LayerStyleRes> listStyles(String layerId) {
        requireLayer(layerId);
        return layerStyleMapper.selectList(new LambdaQueryWrapper<MapLayerStyle>()
                        .eq(MapLayerStyle::getLayerId, layerId)
                        .orderByDesc(MapLayerStyle::getIsDefault)
                        .orderByDesc(MapLayerStyle::getCreateTime))
                .stream().map(this::toLayerStyleRes).toList();
    }

    @Transactional
    public LayerStyleRes createStyle(String layerId, LayerStyleSaveCmd cmd) {
        requireLayer(layerId);
        MapLayerStyle style = new MapLayerStyle();
        style.setId(IdUtils.uuid());
        style.setLayerId(layerId);
        applyStyle(style, cmd);
        layerStyleMapper.insert(style);
        return toLayerStyleRes(layerStyleMapper.selectById(style.getId()));
    }

    @Transactional
    public LayerStyleRes updateStyle(String id, LayerStyleSaveCmd cmd) {
        MapLayerStyle style = requireStyle(id);
        applyStyle(style, cmd);
        layerStyleMapper.updateById(style);
        return toLayerStyleRes(layerStyleMapper.selectById(id));
    }

    @Transactional
    public void deleteStyle(String id) {
        requireStyle(id);
        layerStyleMapper.deleteById(id);
    }

    private MapBasemap requireBasemap(String id) {
        MapBasemap basemap = basemapMapper.selectById(id);
        if (basemap == null) throw BizException.notFound("底图");
        return basemap;
    }

    private void requireLayer(String layerId) {
        if (layerRepository.findById(layerId).isEmpty()) {
            throw BizException.notFound("图层");
        }
    }

    private MapLayerStyle requireStyle(String id) {
        MapLayerStyle style = layerStyleMapper.selectById(id);
        if (style == null) throw BizException.notFound("样式");
        return style;
    }

    private void applyBasemap(MapBasemap basemap, BasemapSaveCmd cmd) {
        basemap.setName(cmd.getName());
        basemap.setDescription(cmd.getDescription());
        basemap.setType(cmd.getType());
        basemap.setUrl(cmd.getUrl());
        basemap.setSrid(cmd.getSrid() != null ? cmd.getSrid() : 3857);
        basemap.setAttribution(cmd.getAttribution());
        basemap.setMinZoom(cmd.getMinZoom() != null ? cmd.getMinZoom() : 0);
        basemap.setMaxZoom(cmd.getMaxZoom() != null ? cmd.getMaxZoom() : 24);
        basemap.setThumbnailUrl(cmd.getThumbnailUrl());
        basemap.setIsDefault(cmd.getIsDefault() != null ? cmd.getIsDefault() : false);
        basemap.setSortOrder(cmd.getSortOrder() != null ? cmd.getSortOrder() : 0);
        basemap.setWmsLayers(cmd.getWmsLayers());
        basemap.setWmtsLayer(cmd.getWmtsLayer());
        basemap.setWmtsStyle(cmd.getWmtsStyle());
        basemap.setWmtsMatrixSet(cmd.getWmtsMatrixSet());
    }

    private void applyFieldItem(MapLayerField field, LayerFieldItemCmd cmd) {
        field.setName(cmd.getName());
        field.setAlias(cmd.getAlias());
        field.setType(cmd.getType());
        field.setVisible(cmd.getVisible() != null ? cmd.getVisible() : true);
        field.setSortable(cmd.getSortable() != null ? cmd.getSortable() : false);
        field.setSearchable(cmd.getSearchable() != null ? cmd.getSearchable() : false);
        boolean required = cmd.getRequired() != null ? cmd.getRequired() : false;
        field.setRequired(required);
        // required=false 且未传 default_value 时按类型自动初始化零值
        String defaultValue = cmd.getDefaultValue();
        if ((defaultValue == null || defaultValue.isBlank()) && !required) {
            defaultValue = typeZeroValue(cmd.getType());
        }
        field.setDefaultValue(defaultValue);
        field.setSortOrder(cmd.getSortOrder() != null ? cmd.getSortOrder() : 0);
    }

    /** 部分更新：仅非 null 字段被写入，未传字段保持不变 */
    private void applyFieldItemPartial(MapLayerField field, LayerFieldItemCmd cmd) {
        if (cmd.getName() != null) field.setName(cmd.getName());
        if (cmd.getAlias() != null) field.setAlias(cmd.getAlias());
        if (cmd.getType() != null) field.setType(cmd.getType());
        if (cmd.getVisible() != null) field.setVisible(cmd.getVisible());
        if (cmd.getSortable() != null) field.setSortable(cmd.getSortable());
        if (cmd.getSearchable() != null) field.setSearchable(cmd.getSearchable());
        if (cmd.getRequired() != null) field.setRequired(cmd.getRequired());
        if (cmd.getDefaultValue() != null) field.setDefaultValue(cmd.getDefaultValue());
        if (cmd.getSortOrder() != null) field.setSortOrder(cmd.getSortOrder());
    }

    /** required=false 且未传 default_value 时的类型零值 */
    private String typeZeroValue(String type) {
        if (type == null) return null;
        return switch (type.toUpperCase()) {
            case "STRING"  -> null;
            case "INTEGER" -> "0";
            case "DOUBLE"  -> "0.0";
            case "BOOLEAN" -> "false";
            case "DATE"    -> null;
            default -> null;
        };
    }

    private void applyStyle(MapLayerStyle style, LayerStyleSaveCmd cmd) {
        style.setName(cmd.getName());
        style.setType(cmd.getType());
        style.setStyleJson(cmd.getStyleJson());
        style.setIsDefault(cmd.getIsDefault() != null ? cmd.getIsDefault() : false);
    }

    private BasemapRes toBasemapRes(MapBasemap basemap) {
        return BasemapRes.builder()
                .id(basemap.getId())
                .name(basemap.getName())
                .description(basemap.getDescription())
                .type(basemap.getType())
                .url(basemap.getUrl())
                .srid(basemap.getSrid())
                .attribution(basemap.getAttribution())
                .minZoom(basemap.getMinZoom())
                .maxZoom(basemap.getMaxZoom())
                .thumbnailUrl(basemap.getThumbnailUrl())
                .isDefault(basemap.getIsDefault())
                .sortOrder(basemap.getSortOrder())
                .wmsLayers(basemap.getWmsLayers())
                .wmtsLayer(basemap.getWmtsLayer())
                .wmtsStyle(basemap.getWmtsStyle())
                .wmtsMatrixSet(basemap.getWmtsMatrixSet())
                .createTime(basemap.getCreateTime())
                .updateTime(basemap.getUpdateTime())
                .build();
    }

    private LayerFieldRes toLayerFieldRes(MapLayerField field) {
        return LayerFieldRes.builder()
                .id(field.getId())
                .layerId(field.getLayerId())
                .name(field.getName())
                .alias(field.getAlias())
                .type(field.getType())
                .visible(field.getVisible())
                .sortable(field.getSortable())
                .searchable(field.getSearchable())
                .required(field.getRequired())
                .defaultValue(field.getDefaultValue())
                .sortOrder(field.getSortOrder())
                .build();
    }

    private LayerStyleRes toLayerStyleRes(MapLayerStyle style) {
        return LayerStyleRes.builder()
                .id(style.getId())
                .layerId(style.getLayerId())
                .name(style.getName())
                .type(style.getType())
                .styleJson(style.getStyleJson())
                .isDefault(style.getIsDefault())
                .createTime(style.getCreateTime())
                .updateTime(style.getUpdateTime())
                .build();
    }
}
