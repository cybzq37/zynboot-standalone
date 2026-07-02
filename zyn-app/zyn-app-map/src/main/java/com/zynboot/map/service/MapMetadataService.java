package com.zynboot.map.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.util.IdUtils;
import com.zynboot.map.command.basemap.BasemapSaveCmd;
import com.zynboot.map.command.layer.LayerFieldSaveCmd;
import com.zynboot.map.command.layer.LayerStyleSaveCmd;
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

@Service
@RequiredArgsConstructor
public class MapMetadataService {

    private final MapBasemapMapper basemapMapper;
    private final MapLayerFieldMapper layerFieldMapper;
    private final MapLayerStyleMapper layerStyleMapper;

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
        return layerFieldMapper.selectList(new LambdaQueryWrapper<MapLayerField>()
                        .eq(MapLayerField::getLayerId, layerId)
                        .orderByAsc(MapLayerField::getSortOrder))
                .stream().map(this::toLayerFieldRes).toList();
    }

    @Transactional
    public LayerFieldRes createField(String layerId, LayerFieldSaveCmd cmd) {
        MapLayerField field = new MapLayerField();
        field.setId(IdUtils.uuid());
        field.setLayerId(layerId);
        applyField(field, cmd);
        layerFieldMapper.insert(field);
        return toLayerFieldRes(layerFieldMapper.selectById(field.getId()));
    }

    @Transactional
    public LayerFieldRes updateField(String id, LayerFieldSaveCmd cmd) {
        MapLayerField field = requireField(id);
        applyField(field, cmd);
        layerFieldMapper.updateById(field);
        return toLayerFieldRes(layerFieldMapper.selectById(id));
    }

    @Transactional
    public void deleteField(String id) {
        requireField(id);
        layerFieldMapper.deleteById(id);
    }

    public List<LayerStyleRes> listStyles(String layerId) {
        return layerStyleMapper.selectList(new LambdaQueryWrapper<MapLayerStyle>()
                        .eq(MapLayerStyle::getLayerId, layerId)
                        .orderByDesc(MapLayerStyle::getIsDefault)
                        .orderByDesc(MapLayerStyle::getCreateTime))
                .stream().map(this::toLayerStyleRes).toList();
    }

    @Transactional
    public LayerStyleRes createStyle(String layerId, LayerStyleSaveCmd cmd) {
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

    private MapLayerField requireField(String id) {
        MapLayerField field = layerFieldMapper.selectById(id);
        if (field == null) throw BizException.notFound("字段");
        return field;
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

    private void applyField(MapLayerField field, LayerFieldSaveCmd cmd) {
        field.setName(cmd.getName());
        field.setAlias(cmd.getAlias());
        field.setType(cmd.getType());
        field.setVisible(cmd.getVisible() != null ? cmd.getVisible() : true);
        field.setSortable(cmd.getSortable() != null ? cmd.getSortable() : false);
        field.setSearchable(cmd.getSearchable() != null ? cmd.getSearchable() : false);
        field.setSortOrder(cmd.getSortOrder() != null ? cmd.getSortOrder() : 0);
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
