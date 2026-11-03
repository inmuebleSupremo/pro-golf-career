package com.progolf.app.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.progolf.sim.progression.CareerPreparation;
import com.progolf.sim.world.World;
import com.progolf.sim.world.WorldConfig;
import com.progolf.sim.world.WorldSnapshot;
import org.junit.jupiter.api.Test;

class CareerPreparationCompatibilityTest {
    @Test
    void oldSnapshotWithoutPreparationGetsFreshPerGolferDefaults() throws Exception {
        WorldConfig config = new WorldConfig(12, 6, 2, 6, 4);
        World original = World.create(612L, config);
        var mapper = FilesystemSaveGameStore.persistenceMapper();
        ObjectNode json = (ObjectNode) mapper.valueToTree(original.snapshot());
        json.remove("careerPreparation");
        WorldSnapshot old = mapper.treeToValue(json, WorldSnapshot.class);
        World loaded = World.restore(612L, config, old);
        assertThat(loaded.snapshot().careerPreparation()).hasSize(original.activeGolferIds().size());
        assertThat(loaded.snapshot().careerPreparation().values()).allMatch(CareerPreparation.initial()::equals);
    }
}
