package com.zynboot.map.controller;

import com.zynboot.map.service.MapExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 数据导出（流式输出，支持 GeoJSON / CSV）。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/map")
@Tag(name = "数据导出", description = "导出图层要素数据")
public class ExportController {

    private final MapExportService exportService;

    @GetMapping("/layer/{layerId}/export")
    @Operation(summary = "导出图层数据", description = "支持按图层导出 GeoJSON 或 CSV 文件")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "导出文件流",
            content = @Content(schema = @Schema(type = "string", format = "binary")))
    public StreamingResponseBody export(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @Parameter(description = "导出格式，支持 geojson、csv", example = "geojson") @RequestParam(defaultValue = "geojson") String format,
            @Parameter(description = "来源数据源 ID，不传时导出图层下全部要素") @RequestParam(required = false) String sourceId,
            @Parameter(hidden = true) HttpServletResponse response) {

        MapExportService.ExportPlan plan = exportService.prepare(layerId, format);

        String encodedFilename = URLEncoder.encode(plan.getFilename(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(plan.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);

        return outputStream -> {
            try {
                exportService.write(layerId, sourceId, plan.getFormat(), outputStream);
            } catch (Exception e) {
                log.error("Export failed: layerId={}, format={}", layerId, plan.getFormat(), e);
                outputStream.write(("{\"error\":\"" + e.getMessage() + "\"}").getBytes());
            }
        };
    }
}
