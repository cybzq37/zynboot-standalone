package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.response.instance.InstanceRes;
import com.zynboot.map.response.instance.PublicMapConfigRes;
import com.zynboot.map.service.MapInstanceService;
import com.zynboot.map.service.MapTileReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map/public")
@Tag(name = "公开访问", description = "公开地图访问与匿名瓦片读取接口")
public class PublicAccessController {

    private final MapInstanceService instanceService;
    private final MapTileReadService tileReadService;

    @GetMapping("/{publishId}")
    @Operation(summary = "获取公开地图实例")
    public ApiResponse<InstanceRes> getPublicMap(@Parameter(description = "发布 ID") @PathVariable String publishId) {
        return ApiResponse.ok(instanceService.getPublicMap(publishId));
    }

    @GetMapping("/{publishId}/config")
    @Operation(summary = "获取公开地图配置")
    public ApiResponse<PublicMapConfigRes> getConfig(@Parameter(description = "发布 ID") @PathVariable String publishId) {
        return ApiResponse.ok(instanceService.getPublicConfig(publishId));
    }

    @GetMapping("/{publishId}/tile/{sourceId}/{z}/{x}/{y}.png")
    @Operation(summary = "读取公开地图栅格瓦片")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "PNG 栅格瓦片",
            content = @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary")))
    public void getPublicTile(
            @Parameter(description = "发布 ID") @PathVariable String publishId,
            @Parameter(description = "源 ID") @PathVariable String sourceId,
            @Parameter(description = "瓦片缩放级别", example = "12") @PathVariable int z,
            @Parameter(description = "瓦片列号", example = "3421") @PathVariable int x,
            @Parameter(description = "瓦片行号", example = "1678") @PathVariable int y,
            @Parameter(hidden = true) jakarta.servlet.http.HttpServletResponse response) throws Exception {

        instanceService.validateSourceBelongsToPublish(publishId, sourceId);
        MapTileReadService.TilePayload tile = tileReadService.readRasterTile(sourceId, z, x, y, "png");
        if (tile == null) {
            response.setStatus(204);
            return;
        }
        response.setContentType(tile.getContentType());
        response.setHeader("Cache-Control", "public, max-age=300");
        response.getOutputStream().write(tile.getData());
    }
}
