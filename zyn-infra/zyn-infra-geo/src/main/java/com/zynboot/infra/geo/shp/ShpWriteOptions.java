package com.zynboot.infra.geo.shp;

import com.zynboot.infra.geo.GeometryType;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public record ShpWriteOptions(
        Charset charset,
        String typeName,
        String geometryFieldName,
        GeometryType geometryType,
        Integer srid,
        boolean overwrite
) {
    public ShpWriteOptions {
        charset = charset == null ? StandardCharsets.UTF_8 : charset;
        geometryFieldName = (geometryFieldName == null || geometryFieldName.isBlank()) ? "the_geom" : geometryFieldName;
    }

    public static ShpWriteOptions defaults() {
        return new ShpWriteOptions(StandardCharsets.UTF_8, null, "the_geom", null, null, false);
    }
}
