package com.zynboot.jitu.command.geo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GeoResolveCmd {

    @NotNull
    @Min(1)
    @Max(2)
    private Integer type;

    @NotBlank
    private String address;
}
