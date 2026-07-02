package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.service.MapFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.zynboot.infra.web.version.ApiVersion;

import java.util.List;
import java.util.Map;

/**
 * BM25 全文搜索控制器（基于 ParadeDB pg_search）。
 */
@RestController
@RequiredArgsConstructor
@ApiVersion("1")
@RequestMapping("/map")
public class SearchController {

    private final MapFeatureService featureService;

    /**
     * BM25 全文搜索。
     *
     * @param layerId 图层 ID
     * @param query   搜索关键词
     * @param bbox    可选空间过滤（minx,miny,maxx,maxy）
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    @GetMapping("/layer/{layerId}/search/bm25")
    public ApiResponse<List<Map<String, Object>>> search(
            @PathVariable String layerId,
            @RequestParam String query,
            @RequestParam(required = false) String bbox,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(featureService.searchBm25(layerId, query, bbox, pageNum, pageSize));
    }
}
