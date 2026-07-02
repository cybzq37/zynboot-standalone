package com.zynboot.map.response.feature;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class FeaturePageRes {
    List<Map<String, Object>> items;
    long total;
    int pageNum;
    int pageSize;
    String querySourceId;
    String querySourceType;
}
