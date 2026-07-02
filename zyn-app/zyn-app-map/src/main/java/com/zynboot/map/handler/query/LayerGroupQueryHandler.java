package com.zynboot.map.handler.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.map.infrastructure.entity.MapLayerGroup;
import com.zynboot.map.infrastructure.mapper.MapLayerGroupMapper;
import com.zynboot.map.response.group.GroupTreeRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LayerGroupQueryHandler {

    private final MapLayerGroupMapper mapper;

    public List<GroupTreeRes> getTree() {
        List<MapLayerGroup> all = mapper.selectList(
                new LambdaQueryWrapper<MapLayerGroup>().orderByAsc(MapLayerGroup::getSortOrder));
        return buildTree(all, null);
    }

    private List<GroupTreeRes> buildTree(List<MapLayerGroup> all, String parentId) {
        Map<String, List<MapLayerGroup>> parentMap = all.stream()
                .collect(Collectors.groupingBy(
                        g -> g.getParentId() == null ? "__root__" : g.getParentId()));
        String key = parentId == null ? "__root__" : parentId;
        return parentMap.getOrDefault(key, List.of()).stream()
                .map(g -> GroupTreeRes.builder()
                        .id(g.getId())
                        .parentId(g.getParentId())
                        .name(g.getName())
                        .description(g.getDescription())
                        .sortOrder(g.getSortOrder())
                        .icon(g.getIcon())
                        .color(g.getColor())
                        .children(buildTree(all, g.getId()))
                        .build())
                .collect(Collectors.toList());
    }
}
