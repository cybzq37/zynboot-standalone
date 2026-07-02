package com.zynboot.map.controller;

import com.zynboot.infra.web.version.ApiVersion;
import com.zynboot.kit.exception.BizException;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.domain.aggregate.SourceAggregate;
import com.zynboot.map.response.source.RasterMetaRes;
import com.zynboot.map.service.MapQueryFacadeService;
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
@ApiVersion("1")
@RequestMapping("/map/source")
public class RasterController {

    private final MapQueryFacadeService queryFacadeService;
    private final com.zynboot.infra.storage.service.StorageService storageService;

    @GetMapping("/{id}/raster/meta")
    public ApiResponse<RasterMetaRes> getMeta(@PathVariable String id) {
        return ApiResponse.ok(queryFacadeService.getRasterMeta(id));
    }

    @GetMapping("/{id}/raster/download")
    public void download(@PathVariable String id, HttpServletResponse response) {
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
