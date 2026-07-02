package com.zynboot.map.command.instance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InstanceSaveCmd {
    @NotBlank
    private String name;
    private String description;
    private Double centerLng;
    private Double centerLat;
    private Integer zoom;
    private String basemapId;
    private Boolean isPublic;
}
