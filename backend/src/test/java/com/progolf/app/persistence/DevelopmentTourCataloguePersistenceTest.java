package com.progolf.app.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.world.World;
import com.progolf.sim.world.WorldConfig;
import com.progolf.sim.world.WorldSnapshot;
import org.junit.jupiter.api.Test;

class DevelopmentTourCataloguePersistenceTest {
    private static final WorldConfig STANDARD = new WorldConfig(8, 30, 1, 4, 12, 4, 1);

    @Test
    void jsonRoundTripPreservesCatalogueAndScheduledHostsWithoutCourseGeometry() throws Exception {
        World world = World.create(111L, STANDARD);
        var mapper = FilesystemSaveGameStore.persistenceMapper();
        String json = mapper.writeValueAsString(world.snapshot());
        ObjectNode tree = (ObjectNode) mapper.readTree(json);
        assertThat(tree.get("developmentCatalogueVersion").asInt()).isEqualTo(1);
        assertThat(tree.withArray("schedule").toString()).contains("\"catalogueId\":");
        assertThat(json).doesNotContain("\"holes\":");
        WorldSnapshot persisted = mapper.readValue(json, WorldSnapshot.class);
        World restored = World.restore(111L, STANDARD, persisted);
        assertThat(restored.currentSchedule()).isEqualTo(world.currentSchedule());
        assertThat(restored.snapshot()).isEqualTo(world.snapshot());
        world.advanceSeason();
        restored.advanceSeason();
        assertThat(restored.currentSchedule()).isEqualTo(world.currentSchedule());
    }

    @Test
    void omittedCatalogueProvenanceAndEventKeysRemainLegacyAfterJsonRead() throws Exception {
        World world = World.create(111L, STANDARD);
        var mapper = FilesystemSaveGameStore.persistenceMapper();
        ObjectNode json = (ObjectNode) mapper.valueToTree(world.snapshot());
        json.remove("developmentCatalogueVersion");
        for (var event : json.withArray("schedule")) {
            ((ObjectNode) event).remove("catalogueId");
            if ("DEVELOPMENT".equals(event.get("tier").asText())) {
                ((ObjectNode) event).put("courseIndex", 0);
            }
        }
        World legacy = World.restore(111L, STANDARD, mapper.treeToValue(json, WorldSnapshot.class));
        assertThat(legacy.developmentCatalogueVersion()).isNull();
        assertThat(legacy.currentSchedule().stream().filter(e -> e.tier() == TourTier.DEVELOPMENT)
                .map(e -> e.catalogueId()).toList()).containsOnlyNulls();
        legacy.advanceSeason();
        assertThat(legacy.currentSchedule().stream().filter(e -> e.tier() == TourTier.DEVELOPMENT)
                .map(e -> e.catalogueId()).toList()).containsOnlyNulls();
    }
}
