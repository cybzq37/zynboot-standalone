package com.zynboot.map.domain.aggregate;

import com.zynboot.map.infrastructure.entity.MapLayerGroup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LayerGroupAggregateTest {

    @Test
    void shouldCreateGroup() {
        LayerGroupAggregate group = LayerGroupAggregate.create(null, "基础底图");

        assertThat(group.getName()).isEqualTo("基础底图");
        assertThat(group.getEntity().getParentId()).isNull();
    }

    @Test
    void shouldReconstituteFromEntity() {
        MapLayerGroup entity = new MapLayerGroup();
        entity.setId("g-001");
        entity.setName("业务图层");

        LayerGroupAggregate group = LayerGroupAggregate.from(entity);

        assertThat(group.getId()).isEqualTo("g-001");
        assertThat(group.getName()).isEqualTo("业务图层");
    }

    @Test
    void shouldUpdateInfo() {
        LayerGroupAggregate group = LayerGroupAggregate.create(null, "底图");
        group.updateInfo("底图V2", "所有底图", 1, "icon-map", "#FF0000");

        assertThat(group.getName()).isEqualTo("底图V2");
        assertThat(group.getEntity().getDescription()).isEqualTo("所有底图");
        assertThat(group.getEntity().getIcon()).isEqualTo("icon-map");
    }
}
