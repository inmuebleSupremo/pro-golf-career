package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.putt.PuttingPolicy;
import org.junit.jupiter.api.Test;

class PhysicalPuttingEventActivationTest {
    @Test
    void changesOnlyTheExplicitUnstartedEventAndLeavesAllOthersLegacy() {
        World world = World.create(717L, new WorldConfig(40, 6, 3, 20, 4));
        long selected = world.snapshot().schedule().getFirst().tournamentId();

        world.setPuttingPolicyForUnstartedEvent(selected, PuttingPolicy.PHYSICAL_V1);

        assertThat(world.snapshot().schedule()).filteredOn(event -> event.tournamentId() == selected)
                .allSatisfy(event -> assertThat(event.puttingPolicy()).isEqualTo(PuttingPolicy.PHYSICAL_V1));
        assertThat(world.snapshot().schedule()).filteredOn(event -> event.tournamentId() != selected)
                .allSatisfy(event -> assertThat(event.puttingPolicy()).isEqualTo(PuttingPolicy.LEGACY));
    }

    @Test
    void rejectsUnknownScheduleEvents() {
        World world = World.create(717L, new WorldConfig(40, 6, 3, 20, 4));
        assertThatThrownBy(() -> world.setPuttingPolicyForUnstartedEvent(Long.MAX_VALUE, PuttingPolicy.PHYSICAL_V1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
