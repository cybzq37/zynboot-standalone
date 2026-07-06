package com.zynboot.jitu.response.geo;

import lombok.Data;

import java.util.Map;

@Data
public class GeoResolveRes {

    private String lnglat;

    private String code;

    private String address;

    private Object aoi;

    private Map<String, Object> fenceInfo;
}
