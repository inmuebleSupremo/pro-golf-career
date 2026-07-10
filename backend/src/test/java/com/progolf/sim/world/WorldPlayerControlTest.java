package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import java.util.List;
import org.junit.jupiter.api.Test;

/** player-control / world-progression (modified): the player's decisions apply to their golfer only. */
class WorldPlayerControlTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void assigningAPlayerDesignatesTheGolfer() {
        World world = World.create(1L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);
        assertThat(world.playerGolferId()).hasValue(id);
    }

    @Test
    void aRestingPlayerIsExcludedFromFieldsWhileTheWorldPlaysOn() {
        World world = World.create(2L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);
        world.setResting(true);
        world.advanceSeason();

        assertThat(world.careerStatisticsOf(id).events()).isZero(); // sat out every event
        boolean othersPlayed = world.activeGolferIds().stream()
                .filter(g -> !g.equals(id))
                .anyMatch(g -> world.careerStatisticsOf(g).events() > 0);
        assertThat(othersPlayed).isTrue(); // the rest of the world competed normally
    }

    @Test
    void developmentFollowsThePlayersFocus() {
        // Two same-seed worlds, same designated golfer, differing only in focus target.
        World a = World.create(3L, small());
        World b = World.create(3L, small());
        String id = a.activeGolferIds().get(0);
        a.assignPlayer(id);
        a.setDevelopmentFocus(List.of(Attribute.DRIVING_DISTANCE));
        b.assignPlayer(id);
        b.setDevelopmentFocus(List.of(Attribute.PUTTING_ACCURACY));
        a.advanceSeason();
        b.advanceSeason();

        Attributes attrsA = a.careerOf(id).player().attributes();
        Attributes attrsB = b.careerOf(id).player().attributes();
        // Each world develops the attribute it focused at least as much as the world that didn't.
        assertThat(attrsA.get(Attribute.DRIVING_DISTANCE)).isGreaterThanOrEqualTo(attrsB.get(Attribute.DRIVING_DISTANCE));
        assertThat(attrsB.get(Attribute.PUTTING_ACCURACY)).isGreaterThanOrEqualTo(attrsA.get(Attribute.PUTTING_ACCURACY));
    }

    @Test
    void sponsorshipOffersGoPendingForThePlayerAndAcceptingSignsOne() {
        World world = World.create(4L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);
        world.advanceSeason(); // season-end generates offers into the player's pending queue

        assertThat(world.pendingSponsorships()).isNotEmpty();
        assertThat(world.financialAccountOf(id).sponsorshipIncome()).isZero(); // nothing auto-signed

        int before = world.pendingSponsorships().size();
        world.acceptSponsorship(0);
        assertThat(world.pendingSponsorships()).hasSize(before - 1);
        assertThat(world.financialAccountOf(id).sponsorshipIncome()).isGreaterThan(0.0); // signing bonus credited
    }

    @Test
    void otherGolfersStayAiDrivenWhileThePlayerDefers() {
        World world = World.create(5L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);
        world.advanceSeason();
        world.advanceSeason(); // player never accepts an offer

        assertThat(world.financialAccountOf(id).sponsorshipIncome()).isZero(); // player deferred, signed nothing
        boolean anyAiSigned = world.activeGolferIds().stream()
                .filter(g -> !g.equals(id))
                .anyMatch(g -> world.financialAccountOf(g).sponsorshipIncome() > 0.0);
        assertThat(anyAiSigned).isTrue(); // AI golfers auto-signed as before
    }

    @Test
    void anIdlePlayerDoesNotPerturbTheCompetitiveWorld() {
        // Player-control must be a strict override: an assigned-but-idle player changes no competitive outcome.
        World withPlayer = World.create(6L, small());
        World autonomous = World.create(6L, small());
        withPlayer.assignPlayer(withPlayer.activeGolferIds().get(0)); // assigned, but never configured
        withPlayer.advanceSeason();
        withPlayer.advanceSeason();
        autonomous.advanceSeason();
        autonomous.advanceSeason();

        assertThat(withPlayer.newsFeed()).isEqualTo(autonomous.newsFeed());
        assertThat(withPlayer.records()).isEqualTo(autonomous.records());
    }
}
