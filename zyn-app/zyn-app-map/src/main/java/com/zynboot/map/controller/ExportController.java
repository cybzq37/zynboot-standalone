package com.zynboot.map.controller;

import com.zynboot.map.service.MapExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.zynboot.infra.web.version.ApiVersion;

/**
 * 数据导出（流式输出，支持 GeoJSON / CSV）。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map")
public class ExportController {

    private final MapExportService exportService;

    @GetMapping("/layer/{layerId}/export")
    public StreamingResponseBody export(
            @PathVariable String layerId,
            @RequestParam(defaultValue = "geojson") String format,
            @RequestParam(required = false) String sourceId,
            HttpServletResponse response) {

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
