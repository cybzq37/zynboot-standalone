package com.zynboot.sys.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.kit.util.BeanUtils;
import com.zynboot.sys.command.resource.ResourceSaveCmd;
import com.zynboot.sys.domain.repository.ResourceRepository;
import com.zynboot.sys.infrastructure.entity.SysResource;
import com.zynboot.sys.response.resource.ResourceRes;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resource")
public class SysResourceController {

    private final ResourceRepository resourceRepository;

    @GetMapping
    @SaCheckPermission("system:resource:query")
    public ApiResponse<List<ResourceRes>> list() {
        return ApiResponse.ok(BeanUtils.copyList(resourceRepository.findAll(), ResourceRes.class));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:resource:query")
    public ApiResponse<ResourceRes> getById(@PathVariable String id) {
        SysResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("资源不存在"));
        return ApiResponse.ok(BeanUtils.copy(resource, ResourceRes.class));
    }

    @PostMapping
    @SaCheckPermission("system:resource:create")
    public ApiResponse<Void> create(@Valid @RequestBody ResourceSaveCmd cmd) {
        resourceRepository.save(BeanUtils.copy(cmd, SysResource.class));
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:resource:update")
    public ApiResponse<Void> update(@PathVariable String id, @Valid @RequestBody ResourceSaveCmd cmd) {
        SysResource resource = BeanUtils.copy(cmd, SysResource.class);
        resource.setId(id);
        resourceRepository.update(resource);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:resource:delete")
    public ApiResponse<Void> delete(@PathVariable String id) {
        resourceRepository.delete(id);
        return ApiResponse.ok(null);
    }
}
