package com.zynboot.infra.geo.shp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShpFieldNameNormalizerTest {

    @Test
    void shouldConvertChineseToLowerCamelPinyin() {
        assertEquals("daoLuMing", ShpFieldNameNormalizer.normalize("道路名"));
    }

    @Test
    void shouldConvertMixedChineseAndAsciiToLowerCamelName() {
        assertEquals("roadDaoLuName1", ShpFieldNameNormalizer.normalize("road_道路-name1"));
    }

    @Test
    void shouldPrefixWhenNormalizedNameStartsWithDigit() {
        assertEquals("field2025DaoLu", ShpFieldNameNormalizer.normalize("2025道路"));
    }
}
