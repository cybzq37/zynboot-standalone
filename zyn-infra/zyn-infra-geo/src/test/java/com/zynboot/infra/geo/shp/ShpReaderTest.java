package com.zynboot.infra.geo.shp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShpReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadShapefileIntoStructuredResult() throws Exception {
        Path shpPath = ShpTestSupport.createPointShapefile(tempDir, "sample", "alice");

        ShpReadResult result = ShpReader.read(shpPath.toString());
        List<ShpFeatureData> features = result.features();

        assertEquals(1, features.size());
        assertEquals("sample", result.schema().typeName());
        assertEquals(1, result.schema().fields().size());
        assertEquals("name", result.schema().fields().getFirst().originalName());
        assertEquals("name", result.schema().fields().getFirst().normalizedName());
        assertEquals("alice", features.getFirst().attributes().get("name"));
        assertNotNull(features.getFirst().geometry());
    }

    @Test
    void shouldReadShapefileFromDirectoryPath() throws Exception {
        ShpTestSupport.createPointShapefile(tempDir, "sample", "alice");

        ShpReadResult result = ShpReader.read(tempDir.toString());

        assertEquals(1, result.features().size());
        assertEquals("alice", result.features().getFirst().attributes().get("name"));
    }

    @Test
    void shouldSelectShapefileByTypeNameFromDirectory() throws Exception {
        ShpTestSupport.createPointShapefile(tempDir, "sample", "alice");
        ShpTestSupport.createPointShapefile(tempDir, "roads", "beijing");
        ShpReadOptions options = new ShpReadOptions(StandardCharsets.UTF_8, true, true, true, true, true, "roads");

        ShpReadResult result = ShpReader.read(tempDir.toString(), options);

        assertEquals("roads", result.schema().typeName());
        assertEquals("beijing", result.features().getFirst().attributes().get("name"));
    }

    @Test
    void shouldFailWhenDirectoryContainsMultipleShapefilesWithoutTypeName() throws Exception {
        ShpTestSupport.createPointShapefile(tempDir, "sample", "alice");
        ShpTestSupport.createPointShapefile(tempDir, "roads", "beijing");

        assertThrows(java.io.IOException.class, () -> ShpReader.read(tempDir.toString()));
    }

    @Test
    void shouldReadShapefileFromZipPath() throws Exception {
        Path sourceDir = Files.createDirectories(tempDir.resolve("zip-source"));
        Path shpPath = ShpTestSupport.createPointShapefile(sourceDir, "sample", "alice");
        Path zipPath = tempDir.resolve("sample.zip");

        ShpTestSupport.zipFiles(ShpTestSupport.listSidecarFiles(shpPath), zipPath);

        ShpReadResult result = ShpReader.read(zipPath.toString());

        assertEquals(1, result.features().size());
        assertEquals("sample", result.schema().typeName());
        assertEquals("alice", result.features().getFirst().attributes().get("name"));
    }
}
