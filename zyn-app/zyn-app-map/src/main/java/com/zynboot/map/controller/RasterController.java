package com.zynboot.map.controller;

import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.response.source.RasterMetaRes;
import com.zynboot.map.service.MapQueryFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/map/source")
@Tag(name = "栅格源", description = "查询栅格源元数据并下载原始文件")
public class RasterController {

    private final MapQueryFacadeService queryFacadeService;
    private final com.zynboot.infra.storage.service.StorageService storageService;

    @GetMapping("/{id}/raster/meta")
    @Operation(summary = "获取栅格源元数据")
    public ApiResponse<RasterMetaRes> getMeta(@Parameter(description = "源 ID") @PathVariable String id) {
        return ApiResponse.ok(queryFacadeService.getRasterMeta(id));
    }

    @GetMapping("/{id}/raster/download")
    @Operation(summary = "下载栅格源文件")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "栅格文件流",
            content = @Content(schema = @Schema(type = "string", format = "binary")))
    public void download(
            @Parameter(description = "源 ID") @PathVariable String id,
            @Parameter(hidden = true) HttpServletResponse response) {
        SourceAggregate source = queryFacadeService.requireSource(id);
        try {
            String storageKey = source.getEntity().getStorageKey();
            String fileName = source.getEntity().getName();
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
            try (InputStream in = storageService.openStream(storageKey);
                 OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
            }
        } catch (Exception e) {
            log.error("Raster download failed: id={}", id, e);
            throw BizException.badRequest("下载失败");
        }
    }
}
