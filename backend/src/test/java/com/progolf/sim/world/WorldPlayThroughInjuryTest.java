package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.health.Injury;
import com.progolf.sim.health.InjurySeverity;
import com.progolf.sim.health.InjuryType;
import com.progolf.sim.health.PhysicalState;
import org.junit.jupiter.api.Test;

/**
 * injury-recovery play-through / world-progression: the controlled golfer may grind through a recovering
 * injury (frozen rehab) or rest to heal, while AI golfers rest recovering injuries and heal as before.
 */
class WorldPlayThroughInjuryTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    /** A MODERATE injury advanced into its recovering tail (5 -> 2 rehab weeks left, so it is playable). */
    private static PhysicalState recovering() {
        return PhysicalState.healthy(0.6)
                .withInjury(Injury.of(InjuryType.WRIST, InjurySeverity.MODERATE).advance(3));
    }

    @Test
    void theControlledPlayerCanGrindThroughAndTheirRehabFreezes() {
        World world = World.create(10L, small());
        String id = world.activeGolferIds().get(0); // the strongest golfer — an Elite regular who plays often
        world.assignPlayer(id);

        // Keep the player recovering each week until their event comes up (so a rest week can't heal it first).
        int guard = 0;
        while (!world.hasPendingPlayerEvent() && guard++ < 60) {
            world.injectPhysicalStateForTest(id, recovering());
            world.advanceWeek();
        }
        // A recovering player was admitted to a field — play-through eligibility works.
        assertThat(world.hasPendingPlayerEvent()).as("recovering player should be entered").isTrue();
        assertThat(world.physicalStateOf(id).canPlayThroughInjury()).isTrue();
        int weeksBefore = world.physicalStateOf(id).injury().orElseThrow().rehabWeeksRemaining();

        world.playerEvent().simEvent();
        world.completePlayerEvent();

        // They competed this week, so rehabilitation did NOT advance (grinding prolongs the injury).
        assertThat(world.physicalStateOf(id).injury()).isPresent();
        assertThat(world.physicalStateOf(id).injury().orElseThrow().rehabWeeksRemaining()).isEqualTo(weeksBefore);
    }

    @Test
    void aRestingPlayerHealsInsteadOfGrinding() {
        World world = World.create(11L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);
        world.setResting(true); // choose to rest rather than play through
        world.injectPhysicalStateForTest(id, recovering());
        int weeksBefore = world.physicalStateOf(id).injury().orElseThrow().rehabWeeksRemaining();

        world.advanceWeek(); // sits out — a rest week

        // Rehabilitation advanced because they did not compete.
        assertThat(world.physicalStateOf(id).injury().orElseThrow().rehabWeeksRemaining())
                .isEqualTo(weeksBefore - 1);
    }

    @Test
    void anAiGolferRestsARecoveringInjuryAndHeals() {
        World world = World.create(12L, small()); // no player assigned
        String ai = world.activeGolferIds().get(0);
        world.injectPhysicalStateForTest(ai, recovering());
        int weeksBefore = world.physicalStateOf(ai).injury().orElseThrow().rehabWeeksRemaining();

        world.advanceWeek();

        // AI golfers are not admitted to fields while recovering, so they rest and rehabilitation advances.
        assertThat(world.physicalStateOf(ai).injury().orElseThrow().rehabWeeksRemaining())
                .isEqualTo(weeksBefore - 1);
    }
}
