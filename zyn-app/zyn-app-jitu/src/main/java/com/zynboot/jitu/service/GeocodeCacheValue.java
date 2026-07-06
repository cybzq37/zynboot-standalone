package com.zynboot.jitu.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeCacheValue {

    private String formattedAddress;

    private double lng;

    private double lat;
}
