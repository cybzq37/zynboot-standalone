package com.zynboot.jitu.integration.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class UpstreamGeoResponse {

    private String status;

    private String message;

    private String count;

    private List<GeocodeItem> geocodes;

    @Data
    public static class GeocodeItem {

        @JsonProperty("formatted_address")
        private String formattedAddress;

        private String location;
    }
}
