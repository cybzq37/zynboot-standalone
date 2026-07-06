package com.zynboot.jitu.infrastructure.entity;

import lombok.Data;

@Data
public class JituFenceHit {

    private Long id;
    private String properties;
    private String geometry;
    private Double centerLng;
    private Double centerLat;
}
