package com.zynboot.infra.geo;

import org.geotools.api.referencing.operation.MathTransform;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GeoCrsUtilsTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Test
    void shouldDecodeAndCacheCrsAndTransform() throws Exception {
        assertNotNull(GeoCrsUtils.decode("EPSG:4326"));
        assertEquals(4326, GeoCrsUtils.toSrid("EPSG:4326"));

        MathTransform transformA = GeoCrsUtils.createTransform("EPSG:4326", "EPSG:3857");
        MathTransform transformB = GeoCrsUtils.createTransform("EPSG:4326", "EPSG:3857");

        assertSame(transformA, transformB);
    }

    @Test
    void shouldTransformGeometryToTargetSrid() throws Exception {
        Geometry point = GEOMETRY_FACTORY.createPoint(new Coordinate(0, 0));

        Geometry transformed = GeoCrsUtils.transform(point, "EPSG:4326", "EPSG:3857");

        assertEquals(3857, transformed.getSRID());
        assertEquals(0.0, transformed.getCoordinate().getX(), 0.000001);
        assertEquals(0.0, transformed.getCoordinate().getY(), 0.000001);
    }

    @Test
    void shouldTransformCsvIntoGeometry() throws Exception {
        Geometry geometry = GeoCrsUtils.transformCsv("0,0;1,1", GeometryType.LINESTRING, "EPSG:4326", "EPSG:3857");

        assertEquals("LineString", geometry.getGeometryType());
        assertEquals(3857, geometry.getSRID());
    }
}
