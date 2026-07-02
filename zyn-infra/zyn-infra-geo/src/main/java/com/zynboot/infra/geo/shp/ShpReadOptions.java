package com.zynboot.infra.geo.shp;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public record ShpReadOptions(
        Charset charset,
        boolean normalizeFieldNames,
        boolean includeGeometry,
        boolean includeWkt,
        boolean includeGeoJson,
        boolean includeCsv,
        String typeName
) {
    public ShpReadOptions {
        charset = charset == null ? StandardCharsets.UTF_8 : charset;
    }

    public static ShpReadOptions defaults() {
        return new ShpReadOptions(StandardCharsets.UTF_8, true, true, false, false, false, null);
    }
}
