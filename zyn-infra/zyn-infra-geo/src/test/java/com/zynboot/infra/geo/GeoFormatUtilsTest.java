package com.zynboot.infra.geo;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoFormatUtilsTest {

    @Test
    void shouldConvertCsvToPolygonAndCloseRing() {
        Geometry geometry = GeoFormatUtils.csvToGeometry("0,0;1,0;1,1;0,1", GeometryType.POLYGON);

        assertEquals("Polygon", geometry.getGeometryType());
        assertEquals(5, geometry.getCoordinates().length);
        assertEquals("0.0,0.0;1.0,0.0;1.0,1.0;0.0,1.0;0.0,0.0", GeoFormatUtils.geometryToCsv(geometry));
    }

    @Test
    void shouldRoundTripBetweenWktAndGeoJson() throws Exception {
        String geoJson = GeoFormatUtils.wktToGeoJson("POINT (116.4 39.9)");
        String wkt = GeoFormatUtils.geoJsonToWkt(geoJson);

        assertTrue(geoJson.contains("\"coordinates\":[116.4,39.9]"));
        assertEquals("POINT (116.4 39.9)", wkt);
    }

    @Test
    void shouldConvertCsvToMultiLineString() {
        Geometry geometry = GeoFormatUtils.csvToGeometry("0,0;1,1|2,2;3,3", GeometryType.MULTILINESTRING);

        assertEquals("MultiLineString", geometry.getGeometryType());
        assertEquals(2, geometry.getNumGeometries());
        assertEquals("0.0,0.0;1.0,1.0|2.0,2.0;3.0,3.0", GeoFormatUtils.geometryToCsv(geometry));
    }

    @Test
    void shouldMergeConnectedMultiLineStringIntoLineString() {
        Geometry geometry = GeoFormatUtils.csvToGeometry("0,0;1,1|1,1;2,2", GeometryType.MULTILINESTRING);

        LineString lineString = GeoFormatUtils.toLineString(geometry);

        assertEquals("LineString", lineString.getGeometryType());
        assertEquals("0.0,0.0;1.0,1.0;2.0,2.0", GeoFormatUtils.geometryToCsv(lineString));
    }

    @Test
    void shouldRejectDisconnectedMultiLineStringToLineString() {
        Geometry geometry = GeoFormatUtils.csvToGeometry("0,0;1,1|2,2;3,3", GeometryType.MULTILINESTRING);

        assertThrows(IllegalArgumentException.class, () -> GeoFormatUtils.toLineString(geometry));
    }
}
