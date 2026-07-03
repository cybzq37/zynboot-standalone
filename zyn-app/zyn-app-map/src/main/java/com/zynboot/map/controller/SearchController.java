package com.zynboot.map.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.map.service.MapFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * BM25 全文搜索控制器（基于 ParadeDB pg_search）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/map")
@Tag(name = "全文检索", description = "提供基于 BM25 的图层全文检索能力")
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
    @Operation(summary = "按 BM25 检索图层要素")
    public ApiResponse<List<Map<String, Object>>> search(
            @Parameter(description = "图层 ID") @PathVariable String layerId,
            @Parameter(description = "检索关键词") @RequestParam String query,
            @Parameter(description = "空间过滤框，格式 minx,miny,maxx,maxy") @RequestParam(required = false) String bbox,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(featureService.searchBm25(layerId, query, bbox, pageNum, pageSize));
    }
}
