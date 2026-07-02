package com.zynboot.map.service;

import com.zynboot.kit.exception.BizException;
import com.zynboot.map.command.group.GroupSaveCmd;
import com.zynboot.map.domain.aggregate.LayerGroupAggregate;
import com.zynboot.map.domain.repository.LayerGroupRepository;
import com.zynboot.map.handler.query.LayerGroupQueryHandler;
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
        groupRepository.delete(id);
    }
}
