package com.zynboot.infra.geo.shp;

import com.zynboot.infra.geo.GeoFormatUtils;
import com.zynboot.infra.geo.GeometryType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.io.IOException;
import java.util.List;

final class ShpGeometryResolver {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private ShpGeometryResolver() {
    }

    static ResolvedWriteContext resolveForWrite(List<ShpFeatureData> features, GeometryType configuredType,
                                                Integer configuredSrid) throws IOException {
        GeometryType resolvedType = configuredType;
        Class<? extends Geometry> binding = null;
        Integer resolvedSrid = configuredSrid;

        if (resolvedType != null) {
            binding = geometryBinding(resolvedType);
        }

        if (resolvedType == null || resolvedSrid == null) {
            for (ShpFeatureData feature : features) {
                Geometry geometry = feature.geometry();
                if (geometry == null) {
                    continue;
                }
                if (resolvedType == null) {
                    resolvedType = geometryTypeOf(geometry);
                    binding = geometryBinding(resolvedType);
                }
                if (resolvedSrid == null && geometry.getSRID() > 0) {
                    resolvedSrid = geometry.getSRID();
                }
                if (resolvedType != null && resolvedSrid != null) {
                    break;
                }
            }
        }

        if (resolvedType == null) {
            throw new IllegalArgumentException("geometryType must be provided when features do not contain geometry");
        }
        return new ResolvedWriteContext(new ResolvedGeometry(resolvedType, binding), resolvedSrid);
    }

    static Geometry resolve(Geometry geometry, GeometryType expectedType) throws IOException {
        if (geometry == null) {
            return null;
        }
        if (expectedType == null) {
            return geometry;
        }
        return convertGeometryType(geometry, expectedType);
    }

    private static Geometry convertGeometryType(Geometry geometry, GeometryType expectedType) {
        return switch (expectedType) {
            case POINT -> {
                if (!(geometry instanceof Point)) {
                    throw new IllegalArgumentException("Expected Point but got: " + geometry.getGeometryType());
                }
                yield geometry;
            }
            case MULTIPOINT -> {
                if (geometry instanceof MultiPoint) {
                    yield geometry;
                }
                if (geometry instanceof Point point) {
                    yield GEOMETRY_FACTORY.createMultiPoint(new Point[]{(Point) point.copy()});
                }
                throw new IllegalArgumentException("Expected Point or MultiPoint but got: " + geometry.getGeometryType());
            }
            case LINESTRING -> GeoFormatUtils.toLineString(geometry);
            case MULTILINESTRING -> {
                if (geometry instanceof MultiLineString) {
                    yield geometry;
                }
                if (geometry instanceof LineString lineString) {
                    yield GEOMETRY_FACTORY.createMultiLineString(new LineString[]{(LineString) lineString.copy()});
                }
                throw new IllegalArgumentException("Expected LineString or MultiLineString but got: " + geometry.getGeometryType());
            }
            case POLYGON -> {
                if (!(geometry instanceof Polygon)) {
                    throw new IllegalArgumentException("Expected Polygon but got: " + geometry.getGeometryType());
                }
                yield geometry;
            }
            case MULTIPOLYGON -> {
                if (geometry instanceof MultiPolygon) {
                    yield geometry;
                }
                if (geometry instanceof Polygon polygon) {
                    yield GEOMETRY_FACTORY.createMultiPolygon(new Polygon[]{(Polygon) polygon.copy()});
                }
                throw new IllegalArgumentException("Expected Polygon or MultiPolygon but got: " + geometry.getGeometryType());
            }
        };
    }

    private static GeometryType geometryTypeOf(Geometry geometry) {
        if (geometry instanceof Point) {
            return GeometryType.POINT;
        }
        if (geometry instanceof MultiPoint) {
            return GeometryType.MULTIPOINT;
        }
        if (geometry instanceof LineString) {
            return GeometryType.LINESTRING;
        }
        if (geometry instanceof MultiLineString) {
            return GeometryType.MULTILINESTRING;
        }
        if (geometry instanceof Polygon) {
            return GeometryType.POLYGON;
        }
        if (geometry instanceof MultiPolygon) {
            return GeometryType.MULTIPOLYGON;
        }
        throw new IllegalArgumentException("Unsupported geometry type: " + geometry.getGeometryType());
    }

    private static Class<? extends Geometry> geometryBinding(GeometryType geometryType) {
        return switch (geometryType) {
            case POINT -> Point.class;
            case MULTIPOINT -> MultiPoint.class;
            case LINESTRING -> LineString.class;
            case MULTILINESTRING -> MultiLineString.class;
            case POLYGON -> Polygon.class;
            case MULTIPOLYGON -> MultiPolygon.class;
        };
    }

    record ResolvedGeometry(
            GeometryType geometryType,
            Class<? extends Geometry> binding
    ) {
    }

    record ResolvedWriteContext(
            ResolvedGeometry resolvedGeometry,
            Integer srid
    ) {
    }
}
