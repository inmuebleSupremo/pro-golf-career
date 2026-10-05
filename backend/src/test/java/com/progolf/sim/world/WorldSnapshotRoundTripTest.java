package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.control.CareerGoal;
import com.progolf.sim.control.GoalType;
import com.progolf.sim.core.Attribute;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * world-snapshot: an autonomous world can be captured and rebuilt, and continuing the rebuilt world is
 * indistinguishable from continuing the original. The immutable {@link WorldSnapshot} is a record, so
 * structural equality over it is the determinism digest.
 */
class WorldSnapshotRoundTripTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void restoreThenAdvanceEqualsAdvance_midSeason() {
        World original = World.create(2026L, small());
        original.advanceSeason();
        original.advanceWeek();
        original.advanceWeek(); // mid-season, a few events resolved

        World restored = World.restore(2026L, small(), original.snapshot());
        // A pure round trip (no advance) is already identical.
        assertThat(restored.snapshot()).isEqualTo(original.snapshot());

        original.advanceSeason();
        restored.advanceSeason();
        assertThat(restored.snapshot()).as("restore-then-advance equals advance").isEqualTo(original.snapshot());
    }

    @Test
    void restoreThenAdvanceEqualsAdvance_acrossSeasons() {
        World original = World.create(7L, small());
        original.advanceSeason();
        original.advanceSeason(); // at a clean season boundary

        World restored = World.restore(7L, small(), original.snapshot());
        for (int i = 0; i < 3; i++) {
            original.advanceSeason();
            restored.advanceSeason();
        }
        assertThat(restored.snapshot()).isEqualTo(original.snapshot());
    }

    @Test
    void roundTripIsStableAcrossSeveralSeeds() {
        for (long seed : new long[] {1L, 42L, 0xC0FFEEL, 999L}) {
            World original = World.create(seed, small());
            original.advanceSeason();
            World restored = World.restore(seed, small(), original.snapshot());
            original.advanceSeason();
            restored.advanceSeason();
            assertThat(restored.snapshot()).as("seed %d", seed).isEqualTo(original.snapshot());
        }
    }

    @Test
    void snapshotRejectsPendingEventRatherThanSilentlyDroppingBallState() {
        World world = World.create(3L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);
        int guard = 0;
        while (!world.hasPendingPlayerEvent() && guard++ < 50) {
            world.advanceWeek();
        }
        assertThat(world.hasPendingPlayerEvent()).isTrue();
        assertThatThrownBy(world::snapshot)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot snapshot while a player event is pending");
    }

    @Test
    void aPlayerWorldRoundTripsWithItsControlState() {
        World original = World.create(4L, small());
        String id = original.activeGolferIds().get(0);
        original.assignPlayer(id);
        // Non-default control state that demonstrably influences advancement.
        original.setDevelopmentFocus(List.of(Attribute.DRIVING_DISTANCE, Attribute.PUTTING_ACCURACY));
        original.setCareerGoals(List.of(new CareerGoal(GoalType.CAREER_WINS, 5), new CareerGoal(GoalType.WIN_A_MAJOR, 1)));
        original.advanceSeason(); // generates pending offers/goals; sims the player's events unattended

        WorldSnapshot snap = original.snapshot();
        assertThat(snap.playerControl()).isNotNull();
        assertThat(snap.playerControl().golferId()).isEqualTo(id);

        World restored = World.restore(4L, small(), snap);
        assertThat(restored.snapshot()).isEqualTo(snap); // pure round trip

        original.advanceSeason();
        restored.advanceSeason();
        assertThat(restored.snapshot()).as("player-world restore-then-advance equals advance").isEqualTo(original.snapshot());
    }

    @Test
    void anActiveEquipmentDealSurvivesSnapshotAndKeepsPaying() {
        World original = World.create(9L, small());
        String id = original.activeGolferIds().get(0);
        original.assignPlayer(id);
        original.advanceSeason(); // free agent → brand-deal offers
        assertThat(original.pendingEquipmentDeals()).isNotEmpty();
        original.acceptEquipmentDeal(0); // sign: active deal + brand gear
        assertThat(original.activeEquipmentDeal()).isNotNull();

        WorldSnapshot snap = original.snapshot();
        World restored = World.restore(9L, small(), snap);

        // The active deal (and its granted gear) round-trips exactly...
        assertThat(restored.snapshot()).isEqualTo(snap);
        assertThat(restored.activeEquipmentDeal()).isEqualTo(original.activeEquipmentDeal());

        // ...and the restored world still honours it: advancing both stays identical (retainer, lock-in, expiry).
        original.advanceSeason();
        restored.advanceSeason();
        assertThat(restored.snapshot()).as("active deal keeps paying after restore").isEqualTo(original.snapshot());
    }

    @Test
    void anAutonomousSnapshotRestoresWithoutPlayerControl() {
        World world = World.create(4L, small());
        world.advanceSeason();
        WorldSnapshot snap = world.snapshot();
        assertThat(snap.playerControl()).isNull();
        assertThat(World.restore(4L, small(), snap).snapshot().playerControl()).isNull();
    }
}
