package com.progolf.app.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.world.DevelopmentTourCatalogue;
import com.progolf.sim.world.World;
import com.progolf.sim.world.WorldConfig;
import com.progolf.sim.world.WorldSnapshot;
import org.junit.jupiter.api.Test;

class DevelopmentTourCataloguePersistenceTest {
    private static final WorldConfig STANDARD = new WorldConfig(8, 30, 1, 4, 12, 4, 1);

    @Test
    void versionOneAndTwoJsonRoundTripsKeepTheirRespectiveHostNames() throws Exception {
        var mapper = FilesystemSaveGameStore.persistenceMapper();
        World newWorld = World.create(47L, STANDARD);
        var event = newWorld.currentSchedule().stream()
                .filter(e -> "hudson-valley".equals(e.catalogueId())).findFirst().orElseThrow();
        ObjectNode oldJson = (ObjectNode) mapper.valueToTree(newWorld.snapshot());
        oldJson.put("developmentCatalogueVersion", DevelopmentTourCatalogue.ORIGINAL_NAMING_VERSION);
        World oldWorld = World.restore(47L, STANDARD, mapper.treeToValue(oldJson, WorldSnapshot.class));
        assertThat(oldWorld.hostIdentityFor(event).name()).isEqualTo("Royal Hollow Golf Links");
        assertThat(newWorld.hostIdentityFor(event).name()).isEqualTo("Royal Hollow Golf Club");

        for (World world : new World[] {oldWorld, newWorld}) {
            String json = mapper.writeValueAsString(world.snapshot());
            World restored = World.restore(47L, STANDARD, mapper.readValue(json, WorldSnapshot.class));
            assertThat(restored.developmentCatalogueVersion()).isEqualTo(world.developmentCatalogueVersion());
            assertThat(restored.currentSchedule()).isEqualTo(world.currentSchedule());
            assertThat(restored.hostIdentityFor(event)).isEqualTo(world.hostIdentityFor(event));
        }
    }

    @Test
    void jsonRoundTripPreservesCatalogueAndScheduledHostsWithoutCourseGeometry() throws Exception {
        World world = World.create(111L, STANDARD);
        var mapper = FilesystemSaveGameStore.persistenceMapper();
        String json = mapper.writeValueAsString(world.snapshot());
        ObjectNode tree = (ObjectNode) mapper.readTree(json);
        assertThat(tree.get("developmentCatalogueVersion").asInt()).isEqualTo(2);
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
