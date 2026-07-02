package com.zynboot.map.domain.aggregate;

import com.zynboot.map.infrastructure.entity.MapLayerSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceAggregateTest {

    @Test
    void shouldCreateSource() {
        SourceAggregate source = SourceAggregate.create("l-001", "2024建筑", "FILE", "SHP");

        assertThat(source.getLayerId()).isEqualTo("l-001");
        assertThat(source.getType()).isEqualTo("FILE");
        assertThat(source.getFormat()).isEqualTo("SHP");
        assertThat(source.getStatus()).isEqualTo("PENDING");
        assertThat(source.getFeatureCount()).isEqualTo(0);
    }

    @Test
    void shouldMarkCompleted() {
        SourceAggregate source = SourceAggregate.create("l-001", "数据", "FILE", "GEOJSON");
        source.markCompleted(500);

        assertThat(source.getStatus()).isEqualTo("COMPLETED");
        assertThat(source.getFeatureCount()).isEqualTo(500);
    }

    @Test
    void shouldMarkFailed() {
        SourceAggregate source = SourceAggregate.create("l-001", "数据", "FILE", "CSV");
        source.markFailed("几何无效");

        assertThat(source.getStatus()).isEqualTo("FAILED");
        assertThat(source.getEntity().getMessage()).isEqualTo("几何无效");
    }

    @Test
    void shouldReconstituteFromEntity() {
        MapLayerSource entity = new MapLayerSource();
        entity.setId("s-001");
        entity.setLayerId("l-001");
        entity.setType("POSTGIS");

        SourceAggregate source = SourceAggregate.from(entity);

        assertThat(source.getId()).isEqualTo("s-001");
        assertThat(source.getType()).isEqualTo("POSTGIS");
    }
}
