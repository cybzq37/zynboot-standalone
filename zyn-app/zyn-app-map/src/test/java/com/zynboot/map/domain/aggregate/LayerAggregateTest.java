package com.zynboot.map.domain.aggregate;

import com.zynboot.map.infrastructure.entity.MapLayer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LayerAggregateTest {

    @Test
    void shouldCreateLayerWithDefaults() {
        LayerAggregate layer = LayerAggregate.create("g1", "建筑图层", "VECTOR", 4326);

        assertThat(layer.getName()).isEqualTo("建筑图层");
        assertThat(layer.getType()).isEqualTo("VECTOR");
        assertThat(layer.getTargetSrid()).isEqualTo(4326);
        assertThat(layer.getFeatureCount()).isEqualTo(0);
        assertThat(layer.getSourceCount()).isEqualTo(0);
    }

    @Test
    void shouldReconstituteFromEntity() {
        MapLayer entity = new MapLayer();
        entity.setId("l-001");
        entity.setName("道路");
        entity.setType("VECTOR");

        LayerAggregate layer = LayerAggregate.from(entity);

        assertThat(layer.getId()).isEqualTo("l-001");
        assertThat(layer.getName()).isEqualTo("道路");
    }

    @Test
    void shouldUpdateInfoWithNonNullFieldsOnly() {
        LayerAggregate layer = LayerAggregate.create("g1", "建筑", "VECTOR", 4326);
        layer.updateInfo("建筑V2", "城市建筑", "描述", 1, true, true, true, 5, 18, 0.8);

        assertThat(layer.getName()).isEqualTo("建筑V2");
        assertThat(layer.getEntity().getTitle()).isEqualTo("城市建筑");
        assertThat(layer.getEntity().getMinZoom()).isEqualTo(5);
        assertThat(layer.getEntity().getOpacity()).isEqualTo(0.8);
    }

    @Test
    void shouldIncrementFeatureCount() {
        LayerAggregate layer = LayerAggregate.create("g1", "建筑", "VECTOR", 4326);
        layer.incrementFeatureCount(100);
        layer.incrementFeatureCount(50);

        assertThat(layer.getFeatureCount()).isEqualTo(150);
    }

    @Test
    void shouldIncrementSourceCount() {
        LayerAggregate layer = LayerAggregate.create("g1", "建筑", "VECTOR", 4326);
        layer.incrementSourceCount();
        layer.incrementSourceCount();

        assertThat(layer.getSourceCount()).isEqualTo(2);
    }
}
