package com.zynboot.infra.geo.shp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShpSampleArchiveTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadAndWriteRealShpArchive() throws Exception {
        // 从 classpath 加载测试数据
        Path sampleZip = tempDir.resolve("shp_point.zip");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("shp_point.zip")) {
            assertNotNull(is, "classpath resource shp_point.zip not found");
            Files.copy(is, sampleZip);
        }

        ShpReadResult source = ShpReader.read(sampleZip.toString(), new ShpReadOptions(
                StandardCharsets.UTF_8, true, true, true, false, false, null
        ));
        assertFalse(source.features().isEmpty(), "sample shapefile should contain features");
        assertFalse(source.schema().fields().isEmpty(), "sample shapefile should contain fields");

        Path outputShp = tempDir.resolve("roundtrip.shp");
        ShpWriteResult writeResult = ShpWriter.write(outputShp.toString(), source.schema(), source.features());
        ShpReadResult roundTrip = ShpReader.read(outputShp.toString(), new ShpReadOptions(
                StandardCharsets.UTF_8, false, true, true, true, true,
                writeResult.schema().typeName()
        ));

        assertEquals(source.features().size(), roundTrip.features().size());
        assertEquals(source.schema().fields().size(), writeResult.schema().fields().size());

        for (int i = 0; i < source.features().size(); i++) {
            ShpFeatureData src = source.features().get(i);
            ShpFeatureData dst = roundTrip.features().get(i);
            assertNotNull(dst.geometry());
            assertEquals(src.geometry().toText(), dst.geometry().toText());

            for (ShpFieldMeta field : source.schema().fields()) {
                String sourceKey = field.normalizedName();
                String writtenKey = writeResult.schema().fieldNameMapping().get(field.originalName());
                assertNotNull(writtenKey, "mapping missing for " + field.originalName());

                Object srcVal = src.attributes().get(sourceKey);
                Object dstVal = dst.attributes().get(writtenKey);
                assertEquals(normalize(srcVal), normalize(dstVal),
                        "value mismatch for field " + field.originalName());
            }
        }
    }

    private Object normalize(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return new BigDecimal(value.toString()).stripTrailingZeros();
        return value.toString();
    }
}
