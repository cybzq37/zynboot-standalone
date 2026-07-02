package com.zynboot.map.command.layer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LayerStyleSaveCmd {
    @NotBlank
    private String name;
    @NotBlank
    private String type;
    @NotBlank
    private String styleJson;
    private Boolean isDefault;
}
