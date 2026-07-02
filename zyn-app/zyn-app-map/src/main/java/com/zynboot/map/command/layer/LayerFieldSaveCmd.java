package com.zynboot.map.command.layer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LayerFieldSaveCmd {
    @NotBlank
    private String name;
    private String alias;
    @NotBlank
    private String type;
    private Boolean visible;
    private Boolean sortable;
    private Boolean searchable;
    private Integer sortOrder;
}
