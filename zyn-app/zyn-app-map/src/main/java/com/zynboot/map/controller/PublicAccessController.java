package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.response.instance.InstanceRes;
import com.zynboot.map.response.instance.PublicMapConfigRes;
import com.zynboot.map.service.MapInstanceService;
import com.zynboot.map.service.MapTileReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map/public")
public class PublicAccessController {

    private final MapInstanceService instanceService;
    private final MapTileReadService tileReadService;

    @GetMapping("/{publishId}")
    public ApiResponse<InstanceRes> getPublicMap(@PathVariable String publishId) {
        return ApiResponse.ok(instanceService.getPublicMap(publishId));
    }

    @GetMapping("/{publishId}/config")
    public ApiResponse<PublicMapConfigRes> getConfig(@PathVariable String publishId) {
        return ApiResponse.ok(instanceService.getPublicConfig(publishId));
    }

    @GetMapping("/{publishId}/tile/{sourceId}/{z}/{x}/{y}.png")
    public void getPublicTile(
            @PathVariable String publishId,
            @PathVariable String sourceId,
            @PathVariable int z,
            @PathVariable int x,
            @PathVariable int y,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {

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
