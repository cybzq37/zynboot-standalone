package com.zynboot.infra.geo.shp;

import com.zynboot.infra.geo.GeometryType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShpWriterTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteAndReadBackShapefileUsingSchemaAndFeatures() throws Exception {
        Path shpPath = tempDir.resolve("writer-sample.shp");
        ShpSchema schema = new ShpSchema("writer_sample", List.of(
                new ShpFieldMeta("道路名", "daoLuMing", String.class),
                new ShpFieldMeta("排序", "sortOrder", Integer.class)
        ));
        List<ShpFeatureData> features = List.of(new ShpFeatureData(
                "feature-1",
                Map.of("daoLuMing", "beijing road", "sortOrder", 1),
                GEOMETRY_FACTORY.createPoint(new Coordinate(116.4, 39.9))
        ));

        ShpWriteResult writeResult = ShpWriter.write(shpPath.toString(), schema, features, new ShpWriteOptions(
                StandardCharsets.UTF_8,
                "writer_sample",
                "the_geom",
                GeometryType.POINT,
                4326,
                false
        ));
        ShpReadResult readResult = ShpReader.read(shpPath.toString(), new ShpReadOptions(
                StandardCharsets.UTF_8,
                false,
                true,
                true,
                true,
                true,
                writeResult.schema().typeName()
        ));

        assertTrue(shpPath.toFile().exists());
        assertEquals(1, writeResult.featureCount());
        assertEquals("writer-sample", writeResult.schema().typeName());
        assertEquals("daoLuMing", writeResult.schema().fieldNameMapping().get("道路名"));
        assertEquals("beijing road", readResult.features().getFirst().attributes().get("daoLuMing"));
        assertEquals(1, readResult.features().getFirst().attributes().get("sortOrder"));
        assertEquals("POINT (116.4 39.9)", readResult.features().getFirst().geometry().toText());
    }

    @Test
    void shouldShortenAndDeduplicateDbfFieldNames() throws Exception {
        Path shpPath = tempDir.resolve("writer-long-fields.shp");
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("veryLongRoadName", "a");
        attributes.put("veryLongRoadNode", "b");
        ShpSchema schema = new ShpSchema("writer_long_fields", List.of(
                new ShpFieldMeta("道路名称", "veryLongRoadName", String.class),
                new ShpFieldMeta("道路节点", "veryLongRoadNode", String.class)
        ));
        List<ShpFeatureData> features = List.of(new ShpFeatureData(
                "feature-1",
                attributes,
                GEOMETRY_FACTORY.createPoint(new Coordinate(116.4, 39.9))
        ));

        ShpWriteResult writeResult = ShpWriter.write(shpPath.toString(), schema, features, new ShpWriteOptions(
                StandardCharsets.UTF_8,
                "writer_long_fields",
                "the_geom",
                GeometryType.POINT,
                4326,
                false
        ));

        assertEquals("veryLongRo", writeResult.schema().fieldNameMapping().get("道路名称"));
        assertEquals("veryLongR2", writeResult.schema().fieldNameMapping().get("道路节点"));
    }

    @Test
    void shouldSupportDirectWriteWithDefaultOptions() throws Exception {
        Path shpPath = tempDir.resolve("writer-defaults.shp");
        ShpSchema schema = new ShpSchema("writer_defaults", List.of(
                new ShpFieldMeta("名称", "name", String.class)
        ));
        List<ShpFeatureData> features = List.of(new ShpFeatureData(
                "feature-1",
                Map.of("name", "demo"),
                GEOMETRY_FACTORY.createPoint(new Coordinate(121.5, 31.2))
        ));

        ShpWriteResult writeResult = ShpWriter.write(shpPath.toString(), schema, features);
        ShpReadResult readResult = ShpReader.read(shpPath.toString(), new ShpReadOptions(
                StandardCharsets.UTF_8,
                false,
                true,
                true,
                true,
                true,
                writeResult.schema().typeName()
        ));

        assertEquals(1, writeResult.featureCount());
        assertEquals("demo", readResult.features().getFirst().attributes().get("name"));
    }
}
