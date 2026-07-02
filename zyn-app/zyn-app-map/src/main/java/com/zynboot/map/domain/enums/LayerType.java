package com.zynboot.map.domain.enums;

import com.zynboot.kit.enums.IEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum LayerType implements IEnum<String> {

    RASTER("RASTER", "栅格"),
    VECTOR("VECTOR", "矢量");

    private final String code;
    private final String desc;

    LayerType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public String getDesc() { return desc; }
}
