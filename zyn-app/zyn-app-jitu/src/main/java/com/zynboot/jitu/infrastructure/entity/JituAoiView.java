package com.zynboot.jitu.infrastructure.entity;

import lombok.Data;

@Data
public class JituAoiView {

    private String id;
    private String aoiType;
    private String aoiName;
    private String address;
    private String gbCode;
    private String kind;
    private Double longitude;
    private Double latitude;
    private String properties;
    private String geojson;
}
