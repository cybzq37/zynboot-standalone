package com.zynboot.map.command.feature;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeatureSaveCmd {

    private String sourceId;

    @NotBlank
    private String properties;

    /**
     * GeoJSON geometry.
     */
    @NotBlank
    private String geometry;
}
