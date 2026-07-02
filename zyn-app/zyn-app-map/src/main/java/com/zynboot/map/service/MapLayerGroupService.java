package com.zynboot.map.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.kit.exception.BizException;
import com.zynboot.map.command.group.GroupSaveCmd;
import com.zynboot.map.domain.aggregate.LayerGroupAggregate;
import com.zynboot.map.domain.repository.LayerGroupRepository;
import com.zynboot.map.handler.query.LayerGroupQueryHandler;
import com.zynboot.map.infrastructure.entity.MapLayer;
import com.zynboot.map.infrastructure.entity.MapLayerGroup;
import com.zynboot.map.infrastructure.mapper.MapLayerGroupMapper;
import com.zynboot.map.infrastructure.mapper.MapLayerMapper;
import com.zynboot.map.response.group.GroupTreeRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapLayerGroupService {

    private final LayerGroupRepository groupRepository;
    private final LayerGroupQueryHandler groupQueryHandler;
    private final MapLayerGroupMapper groupMapper;
    private final MapLayerMapper layerMapper;

    public List<GroupTreeRes> tree() {
        return groupQueryHandler.getTree();
    }

    @Transactional
    public void create(GroupSaveCmd cmd) {
        LayerGroupAggregate group = LayerGroupAggregate.create(cmd.getParentId(), cmd.getName());
        group.updateInfo(cmd.getName(), cmd.getDescription(), cmd.getSortOrder(), cmd.getIcon(), cmd.getColor());
        groupRepository.save(group);
    }

    @Transactional
    public void update(String id, GroupSaveCmd cmd) {
        LayerGroupAggregate group = groupRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("分组"));
        group.updateInfo(cmd.getName(), cmd.getDescription(), cmd.getSortOrder(), cmd.getIcon(), cmd.getColor());
        groupRepository.update(group);
    }

    @Transactional
    public void delete(String id) {
        // 检查是否存在子分组
        long childGroups = groupMapper.selectCount(
                new LambdaQueryWrapper<MapLayerGroup>().eq(MapLayerGroup::getParentId, id));
        if (childGroups > 0) {
            throw BizException.badRequest("分组下存在子分组，无法删除");
        }
        // 检查是否存在图层引用
        long layerCount = layerMapper.selectCount(
                new LambdaQueryWrapper<MapLayer>().eq(MapLayer::getGroupId, id));
        if (layerCount > 0) {
            throw BizException.badRequest("分组下存在图层，无法删除");
        }
        groupRepository.delete(id);
    }
}
