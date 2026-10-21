package com.progolf.app.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.progolf.sim.course.PinPlacementVersion;
import com.progolf.sim.putt.PuttingPolicy;
import com.progolf.sim.tournament.EventPrestige;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.world.ScheduledTournament;
import com.progolf.sim.world.World;
import com.progolf.sim.world.WorldConfig;
import com.progolf.sim.world.WorldSnapshot;
import org.junit.jupiter.api.Test;

/** Event policy serialization is additive: absent historical data remains legacy. */
class PhysicalPuttingPolicyPersistenceTest {
    private final ObjectMapper mapper = FilesystemSaveGameStore.persistenceMapper();

    @Test
    void scheduledTournamentRoundTripsAnExplicitPhysicalPolicy() throws Exception {
        ScheduledTournament event = new ScheduledTournament(3, TourTier.PRO, 2, EventPrestige.REGULAR, 71L,
                PinPlacementVersion.V5_EFFECTIVE_GREEN, PuttingPolicy.PHYSICAL_V1);

        ScheduledTournament restored = mapper.readValue(mapper.writeValueAsString(event), ScheduledTournament.class);

        assertThat(restored).isEqualTo(event);
        assertThat(restored.puttingPolicy()).isEqualTo(PuttingPolicy.PHYSICAL_V1);
    }

    @Test
    void prePhysicalPolicyPayloadDefaultsToLegacy() throws Exception {
        ScheduledTournament event = new ScheduledTournament(3, TourTier.PRO, 2, EventPrestige.REGULAR, 71L,
                PinPlacementVersion.V5_EFFECTIVE_GREEN, PuttingPolicy.PHYSICAL_V1);
        ObjectNode historical = (ObjectNode) mapper.readTree(mapper.writeValueAsString(event));
        historical.remove("puttingPolicy");

        ScheduledTournament restored = mapper.treeToValue((JsonNode) historical, ScheduledTournament.class);

        assertThat(restored.puttingPolicy()).isEqualTo(PuttingPolicy.LEGACY);
        assertThat(restored.pinPlacementVersion()).isEqualTo(PinPlacementVersion.V5_EFFECTIVE_GREEN);
    }

    @Test
    void snapshotReconstructionPreservesAnExplicitScheduledPhysicalPolicy() throws Exception {
        WorldConfig small = new WorldConfig(40, 6, 3, 20, 4);
        World original = World.create(91L, small);
        ObjectNode serialized = mapper.valueToTree(original.snapshot());
        ((ObjectNode) serialized.withArray("schedule").get(0)).put("puttingPolicy", PuttingPolicy.PHYSICAL_V1.name());

        WorldSnapshot snapshot = mapper.treeToValue(serialized, WorldSnapshot.class);
        World restored = World.restore(91L, small, snapshot);

        assertThat(restored.snapshot().schedule().getFirst().puttingPolicy()).isEqualTo(PuttingPolicy.PHYSICAL_V1);
        assertThat(restored.snapshot().schedule()).anySatisfy(event ->
                assertThat(event.puttingPolicy()).isEqualTo(PuttingPolicy.LEGACY));
    }
}
