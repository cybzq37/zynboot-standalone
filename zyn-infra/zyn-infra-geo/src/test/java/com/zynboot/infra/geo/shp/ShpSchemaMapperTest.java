package com.zynboot.infra.geo.shp;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShpSchemaMapperTest {

    @Test
    void shouldNormalizeChineseFieldNamesAndKeepMapping() {
        ShpReadOptions options = new ShpReadOptions(Charset.forName("GBK"),
                true, true, true, true, true, "zhsample");
        Map<String, Class<?>> rawFields = new LinkedHashMap<>();
        rawFields.put("道路名", String.class);
        ShpSchema schema = ShpSchemaMapper.buildSchema("zhsample", rawFields, options);
        Map<String, Object> rawAttributes = new HashMap<>();
        rawAttributes.put("道路名", "beijing");

        ShpFeatureData feature = ShpSchemaMapper.buildFeatureData("feature-1", rawAttributes, null, schema.fieldNameMapping());

        assertEquals("beijing", feature.attributes().get("daoLuMing"));
        assertEquals("daoLuMing", schema.fieldNameMapping().get("道路名"));
    }

    @Test
    void shouldAppendSuffixWhenNormalizedFieldNamesConflict() {
        ShpReadOptions options = new ShpReadOptions(StandardCharsets.UTF_8,
                true, true, true, true, true, "zhsample");
        Map<String, Class<?>> rawFields = new LinkedHashMap<>();
        rawFields.put("道路名", String.class);
        rawFields.put("道路_名", String.class);
        Map<String, Object> rawAttributes = new LinkedHashMap<>();
        rawAttributes.put("道路名", "a");
        rawAttributes.put("道路_名", "b");

        ShpSchema schema = ShpSchemaMapper.buildSchema("zhsample", rawFields, options);
        ShpFeatureData feature = ShpSchemaMapper.buildFeatureData("feature-1", rawAttributes, null, schema.fieldNameMapping());

        assertEquals("daoLuMing", schema.fieldNameMapping().get("道路名"));
        assertEquals("daoLuMing2", schema.fieldNameMapping().get("道路_名"));
        assertEquals("a", feature.attributes().get("daoLuMing"));
        assertEquals("b", feature.attributes().get("daoLuMing2"));
    }
}
