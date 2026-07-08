package com.zynboot.map.domain.enums;

import com.zynboot.kit.enums.IEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SourceType implements IEnum<String> {

    LOCAL("LOCAL", "本地数据库图层"),
    FILE("FILE", "文件"),
    POSTGIS("POSTGIS", "PostGIS"),
    WMS("WMS", "WMS服务"),
    WFS("WFS", "WFS服务"),
    WMTS("WMTS", "WMTS服务"),
    TMS("TMS", "TMS服务"),
    XYZ("XYZ", "XYZ瓦片");

    private final String code;
    private final String desc;

    SourceType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public String getDesc() { return desc; }
}
