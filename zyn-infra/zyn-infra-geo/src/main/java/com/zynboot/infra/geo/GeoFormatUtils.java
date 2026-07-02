package com.zynboot.infra.geo;

import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.operation.linemerge.LineMerger;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class GeoFormatUtils {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final GeometryJSON GEOMETRY_JSON = new GeometryJSON();

    private GeoFormatUtils() {
    }

    public static String geometryToWkt(Geometry geometry) {
        return new WKTWriter().write(geometry);
    }

    public static Geometry wktToGeometry(String wkt) throws Exception {
        return new WKTReader().read(wkt);
    }

    public static String geometryToGeoJson(Geometry geometry) throws IOException {
        StringWriter writer = new StringWriter();
        GEOMETRY_JSON.write(geometry, writer);
        return writer.toString();
    }

    public static Geometry geoJsonToGeometry(String geoJson) throws IOException {
        return GEOMETRY_JSON.read(new StringReader(geoJson));
    }

    public static String wktToGeoJson(String wkt) throws Exception {
        return geometryToGeoJson(wktToGeometry(wkt));
    }

    public static String geoJsonToWkt(String geoJson) throws Exception {
        return geometryToWkt(geoJsonToGeometry(geoJson));
    }

    public static LineString toLineString(Geometry geometry) {
        if (geometry == null) {
            throw new IllegalArgumentException("geometry must not be null");
        }
        if (geometry instanceof LineString lineString) {
            return lineString;
        }
        if (!"MultiLineString".equalsIgnoreCase(geometry.getGeometryType())) {
            throw new IllegalArgumentException("geometry must be LineString or MultiLineString");
        }

        LineMerger lineMerger = new LineMerger();
        lineMerger.add(geometry);
        Collection<?> mergedLines = lineMerger.getMergedLineStrings();
        if (mergedLines.size() != 1) {
            throw new IllegalArgumentException("MultiLineString cannot be merged into a single LineString");
        }
        return (LineString) mergedLines.iterator().next();
    }

    public static String geometryToCsv(Geometry geometry) {
        if (geometry == null) {
            throw new IllegalArgumentException("geometry must not be null");
        }
        String geometryType = geometry.getGeometryType();
        if ("MultiLineString".equalsIgnoreCase(geometryType)
                || "MultiPoint".equalsIgnoreCase(geometryType)
                || "MultiPolygon".equalsIgnoreCase(geometryType)) {
            List<String> parts = new ArrayList<>(geometry.getNumGeometries());
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                parts.add(coordinatesToCsv(geometry.getGeometryN(i).getCoordinates()));
            }
            return String.join("|", parts);
        }

        return coordinatesToCsv(geometry.getCoordinates());
    }

    private static String coordinatesToCsv(Coordinate[] coordinates) {
        List<String> points = new ArrayList<>(coordinates.length);
        for (Coordinate coordinate : coordinates) {
            points.add(coordinate.getX() + "," + coordinate.getY());
        }
        return String.join(";", points);
    }

    public static Geometry csvToGeometry(String csv, String geometryType) {
        return csvToGeometry(csv, GeometryType.from(geometryType));
    }

    public static Geometry csvToGeometry(String csv, GeometryType geometryType) {
        GeometryType resolvedType = geometryType == null ? GeometryType.POINT : geometryType;
        return switch (resolvedType) {
            case POINT -> createPoint(csv);
            case MULTIPOINT -> createMultiPoint(csv);
            case LINESTRING -> createLineString(csv);
            case MULTILINESTRING -> createMultiLineString(csv);
            case POLYGON -> createPolygon(parseRequiredCoordinates(csv));
            case MULTIPOLYGON -> createMultiPolygon(csv);
        };
    }

    public static String csvToWkt(String csv, String geometryType) {
        return geometryToWkt(csvToGeometry(csv, geometryType));
    }

    public static String csvToWkt(String csv, GeometryType geometryType) {
        return geometryToWkt(csvToGeometry(csv, geometryType));
    }

    public static String csvToGeoJson(String csv, String geometryType) throws IOException {
        return geometryToGeoJson(csvToGeometry(csv, geometryType));
    }

    public static String csvToGeoJson(String csv, GeometryType geometryType) throws IOException {
        return geometryToGeoJson(csvToGeometry(csv, geometryType));
    }

    public static String wktToCsv(String wkt) throws Exception {
        return geometryToCsv(wktToGeometry(wkt));
    }

    public static String geoJsonToCsv(String geoJson) throws IOException {
        return geometryToCsv(geoJsonToGeometry(geoJson));
    }

    private static List<Coordinate> parseCsvCoordinates(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(";"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(pair -> {
                    String[] values = pair.split(",");
                    if (values.length != 2) {
                        throw new IllegalArgumentException("Invalid coordinate: " + pair);
                    }
                    return new Coordinate(Double.parseDouble(values[0].trim()), Double.parseDouble(values[1].trim()));
                })
                .toList();
    }

    private static List<Coordinate> parseRequiredCoordinates(String csv) {
        List<Coordinate> coordinates = parseCsvCoordinates(csv);
        if (coordinates.isEmpty()) {
            throw new IllegalArgumentException("CSV coordinates cannot be empty");
        }
        return coordinates;
    }

    private static Geometry createPoint(String csv) {
        return GEOMETRY_FACTORY.createPoint(parseRequiredCoordinates(csv).get(0));
    }

    private static Geometry createLineString(String csv) {
        List<Coordinate> coordinates = parseRequiredCoordinates(csv);
        return GEOMETRY_FACTORY.createLineString(coordinates.toArray(new Coordinate[0]));
    }

    private static Geometry createPolygon(List<Coordinate> coordinates) {
        List<Coordinate> polygonCoords = new ArrayList<>(coordinates);
        if (!polygonCoords.get(0).equals2D(polygonCoords.get(polygonCoords.size() - 1))) {
            polygonCoords.add(new Coordinate(polygonCoords.get(0)));
        }
        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(polygonCoords.toArray(new Coordinate[0]));
        return GEOMETRY_FACTORY.createPolygon(shell, null);
    }

    private static Geometry createMultiLineString(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("CSV coordinates cannot be empty");
        }

        List<LineString> lineStrings = Arrays.stream(csv.split("\\|"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(GeoFormatUtils::parseCsvCoordinates)
                .map(coordinates -> GEOMETRY_FACTORY.createLineString(coordinates.toArray(new Coordinate[0])))
                .toList();

        if (lineStrings.isEmpty()) {
            throw new IllegalArgumentException("CSV coordinates cannot be empty");
        }
        return GEOMETRY_FACTORY.createMultiLineString(lineStrings.toArray(LineString[]::new));
    }

    private static Geometry createMultiPoint(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("CSV coordinates cannot be empty");
        }

        List<Point> points = Arrays.stream(csv.split("\\|"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(GeoFormatUtils::parseCsvCoordinates)
                .map(coordinates -> {
                    if (coordinates.size() != 1) {
                        throw new IllegalArgumentException("MultiPoint segment must contain exactly one coordinate pair");
                    }
                    return GEOMETRY_FACTORY.createPoint(coordinates.get(0));
                })
                .toList();

        if (points.isEmpty()) {
            throw new IllegalArgumentException("CSV coordinates cannot be empty");
        }
        return GEOMETRY_FACTORY.createMultiPoint(points.toArray(Point[]::new));
    }

    private static Geometry createMultiPolygon(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("CSV coordinates cannot be empty");
        }

        List<Polygon> polygons = Arrays.stream(csv.split("\\|"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(part -> (Polygon) createPolygon(parseCsvCoordinates(part)))
                .toList();

        if (polygons.isEmpty()) {
            throw new IllegalArgumentException("CSV coordinates cannot be empty");
        }
        return GEOMETRY_FACTORY.createMultiPolygon(polygons.toArray(Polygon[]::new));
    }
}
