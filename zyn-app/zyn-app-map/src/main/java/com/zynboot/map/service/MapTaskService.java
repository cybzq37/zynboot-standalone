package com.zynboot.map.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.util.IdUtils;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.domain.repository.SourceRepository;
import com.zynboot.map.infrastructure.entity.MapAsyncTask;
import com.zynboot.map.infrastructure.mapper.MapAsyncTaskMapper;
import com.zynboot.map.response.task.TaskRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MapTaskService {

    private final MapAsyncTaskMapper taskMapper;
    private final SourceRepository sourceRepository;
    private final TileService tileService;

    public List<TaskRes> list(String type, String status) {
        return taskMapper.selectList(new LambdaQueryWrapper<MapAsyncTask>()
                        .eq(type != null && !type.isBlank(), MapAsyncTask::getType, type)
                        .eq(status != null && !status.isBlank(), MapAsyncTask::getStatus, status)
                        .orderByDesc(MapAsyncTask::getCreatedAt))
                .stream()
                .map(this::toRes)
                .toList();
    }

    public TaskRes getById(String id) {
        return toRes(requireTask(id));
    }

    @Transactional
    public TaskRes cancel(String id) {
        MapAsyncTask task = requireTask(id);
        if (!"PENDING".equals(task.getStatus()) && !"RUNNING".equals(task.getStatus())) {
            throw BizException.badRequest("任务已结束，无法取消");
        }
        task.setStatus("CANCELLED");
        task.setFinishedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        return toRes(task);
    }

    @Transactional
    public TaskRes submitTileTask(String sourceId) {
        SourceAggregate source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> BizException.notFound("数据源"));
        if (!"FILE".equals(source.getType())) {
            throw BizException.badRequest("仅 FILE 类型数据源支持切片任务");
        }
        String storageKey = source.getEntity().getStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            throw BizException.badRequest("数据源未绑定栅格文件");
        }
        MapAsyncTask existing = taskMapper.selectOne(new LambdaQueryWrapper<MapAsyncTask>()
                .eq(MapAsyncTask::getSourceId, sourceId)
                .eq(MapAsyncTask::getType, "TILE")
                .in(MapAsyncTask::getStatus, "PENDING", "RUNNING")
                .last("LIMIT 1"));
        if (existing != null) {
            return toRes(existing);
        }

        MapAsyncTask task = new MapAsyncTask();
        task.setId(IdUtils.uuid());
        task.setType("TILE");
        task.setSourceId(sourceId);
        task.setLayerId(source.getLayerId());
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setTotalCount(1);
        task.setProcessedCount(0);
        task.setErrorCount(0);
        task.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(task);

        // 在事务提交后再触发异步切片，避免 worker 线程读到未提交的 task 行
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                tileService.tileAsync(task.getId(), sourceId, storageKey, source.getLayerId());
            }
        });
        return toRes(task);
    }

    private MapAsyncTask requireTask(String id) {
        MapAsyncTask task = taskMapper.selectById(id);
        if (task == null) {
            throw BizException.notFound("任务");
        }
        return task;
    }

    public TaskRes toRes(MapAsyncTask task) {
        return TaskRes.builder()
                .id(task.getId())
                .type(task.getType())
                .sourceId(task.getSourceId())
                .layerId(task.getLayerId())
                .status(task.getStatus())
                .progress(task.getProgress())
                .totalCount(task.getTotalCount())
                .processedCount(task.getProcessedCount())
                .errorCount(task.getErrorCount())
                .errorMessage(task.getErrorMessage())
                .startedAt(task.getStartedAt())
                .finishedAt(task.getFinishedAt())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
