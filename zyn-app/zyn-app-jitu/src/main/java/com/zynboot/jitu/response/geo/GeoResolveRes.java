package com.zynboot.jitu.response.geo;

import lombok.Data;

import java.util.Map;

@Data
public class GeoResolveRes {

    /** 对应四段码的中心经纬度坐标 */
    private String lnglat;

    /** 绑定的四段码 */
    private String code;

    /** 解析后的标准化地址 */
    private String address;

    /** AOI 数据几何信息，GeoJSON 几何对象；BP 优先，命中即返回，否则查 BUILDING */
    private Object aoi;

    /** 围栏信息，包含图层要素 ID、GeoJSON、绑定的 AOI */
    private Map<String, Object> fenceInfo;
}
