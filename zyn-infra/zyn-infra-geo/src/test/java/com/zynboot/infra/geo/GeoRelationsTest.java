package com.zynboot.infra.geo;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoRelationsTest {

    @Test
    void shouldEvaluateCommonSpatialRelations() throws Exception {
        Geometry polygon = GeoFormatUtils.wktToGeometry("POLYGON ((0 0, 2 0, 2 2, 0 2, 0 0))");
        Geometry point = GeoFormatUtils.wktToGeometry("POINT (1 1)");
        Geometry line = GeoFormatUtils.wktToGeometry("LINESTRING (2 2, 3 3)");

        assertTrue(GeoRelations.intersects(polygon, point));
        assertTrue(GeoRelations.contains(polygon, point));
        assertTrue(GeoRelations.within(point, polygon));
        assertTrue(GeoRelations.touches(polygon, line));
        assertEquals(0.0, GeoRelations.distance(polygon, point), 0.000001);
    }

    @Test
    void shouldRejectUnexpectedGeometryType() throws Exception {
        Geometry point = GeoFormatUtils.wktToGeometry("POINT (1 1)");

        assertThrows(IllegalArgumentException.class, () -> GeoRelations.requirePolygon(point, "point"));
    }
}
