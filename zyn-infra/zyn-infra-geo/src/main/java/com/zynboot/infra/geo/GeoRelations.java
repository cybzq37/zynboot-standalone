package com.zynboot.infra.geo;

import org.locationtech.jts.geom.Geometry;

public final class GeoRelations {

    private GeoRelations() {
    }

    public static boolean intersects(Geometry source, Geometry target) {
        return source != null && target != null && source.intersects(target);
    }

    public static boolean intersectsByWkt(String sourceWkt, String targetWkt) throws Exception {
        return intersects(GeoFormatUtils.wktToGeometry(sourceWkt), GeoFormatUtils.wktToGeometry(targetWkt));
    }

    public static boolean contains(Geometry source, Geometry target) {
        return source != null && target != null && source.contains(target);
    }

    public static boolean within(Geometry source, Geometry target) {
        return source != null && target != null && source.within(target);
    }

    public static boolean touches(Geometry source, Geometry target) {
        return source != null && target != null && source.touches(target);
    }

    public static double distance(Geometry source, Geometry target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("source and target must not be null");
        }
        return source.distance(target);
    }

    public static void requirePoint(Geometry geometry, String paramName) {
        if (!isPointGeometry(geometry)) {
            throw new IllegalArgumentException(paramName + " must be Point or MultiPoint");
        }
    }

    public static void requireLine(Geometry geometry, String paramName) {
        if (!isLineGeometry(geometry)) {
            throw new IllegalArgumentException(paramName + " must be LineString or MultiLineString");
        }
    }

    public static void requirePolygon(Geometry geometry, String paramName) {
        if (!isPolygonGeometry(geometry)) {
            throw new IllegalArgumentException(paramName + " must be Polygon or MultiPolygon");
        }
    }

    private static boolean isPointGeometry(Geometry geometry) {
        return geometry != null && ("Point".equalsIgnoreCase(geometry.getGeometryType())
                || "MultiPoint".equalsIgnoreCase(geometry.getGeometryType()));
    }

    private static boolean isLineGeometry(Geometry geometry) {
        return geometry != null && ("LineString".equalsIgnoreCase(geometry.getGeometryType())
                || "MultiLineString".equalsIgnoreCase(geometry.getGeometryType()));
    }

    private static boolean isPolygonGeometry(Geometry geometry) {
        return geometry != null && ("Polygon".equalsIgnoreCase(geometry.getGeometryType())
                || "MultiPolygon".equalsIgnoreCase(geometry.getGeometryType()));
    }
}
