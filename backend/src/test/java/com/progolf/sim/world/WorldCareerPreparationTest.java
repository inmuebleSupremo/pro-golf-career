package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.health.HealthSystem;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.progression.CareerPreparation;
import com.progolf.sim.progression.PreparationFocus;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorldCareerPreparationTest {
    private static final WorldConfig SMALL = new WorldConfig(24, 6, 2, 12, 4);

    private World playerWorld(long seed) {
        World world = World.create(seed, SMALL);
        world.createPlayer("Training", "Golfer", Nationality.USA, 19, Archetype.ALL_ROUNDER);
        return world;
    }

    @Test
    void standingChoiceIsFreeAndWeeklyCommitIsIdempotent() {
        World world = playerWorld(501);
        assertThat(world.playerPreparationAvailable()).isTrue();
        world.setStandingPreparationFocus(PreparationFocus.PUTTING);
        assertThat(world.playerPreparationAvailable()).isTrue();
        assertThat(world.commitPlayerPreparation(PreparationFocus.DRIVING)).isTrue();
        var first = world.playerPreparation();
        assertThat(first.committedFocus()).isEqualTo(PreparationFocus.DRIVING);
        double fatigue = world.physicalStateOf(world.playerGolferId().orElseThrow()).fatigue();
        assertThat(first.progressFor(PreparationFocus.DRIVING)).isCloseTo(1.02125,
                org.assertj.core.data.Offset.offset(1e-9));
        assertThat(fatigue).isCloseTo(0.035, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(first.progressFor(PreparationFocus.PUTTING)).isZero();
        assertThat(world.commitPlayerPreparation(PreparationFocus.RECOVERY)).isFalse();
        assertThat(world.playerPreparation()).isEqualTo(first);
        assertThat(world.physicalStateOf(world.playerGolferId().orElseThrow()).fatigue()).isEqualTo(fatigue);
        world.advanceSeason();
        assertThat(world.playerPreparation().progressFor(PreparationFocus.DRIVING))
                .isCloseTo(1.02125, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(world.playerPreparation().standingFocus()).isEqualTo(PreparationFocus.PUTTING);
    }

    @Test
    void focusProgressCarriesAcrossSeasonsAndSharesPurchaseCap() {
        World world = playerWorld(502);
        world.setStandingPreparationFocus(PreparationFocus.WEDGES);
        String id = world.playerGolferId().orElseThrow();
        world.advanceSeason();
        world.advanceSeason();
        assertThat(world.careerOf(id).player().attributeChanges().stream()
                .filter(c -> c.attribute() == Attribute.WEDGES && c.delta() > 0).count()).isPositive();
        assertThat(world.playerPreparation().progressFor(PreparationFocus.WEDGES)).isBetween(0.0, 12.0);
        assertThat(world.playerPreparation().progressFor(PreparationFocus.DRIVING)).isZero();
        int earnedThisSeason = world.careerOf(id).player().attributeChanges().stream()
                .filter(c -> c.attribute() == Attribute.WEDGES && c.season() == world.currentSeason()
                        && c.delta() > 0).mapToInt(c -> c.delta()).sum();
        int allowance = world.playerDevelopmentOptions().stream().filter(o -> o.attribute() == Attribute.WEDGES)
                .findFirst().orElseThrow().remainingSeasonal();
        assertThat(allowance).isEqualTo(6 - earnedThisSeason);
        world.spendDevelopmentPoints(Map.of(Attribute.WEDGES, allowance));
        assertThat(world.playerDevelopmentOptions().stream().filter(o -> o.attribute() == Attribute.WEDGES)
                .findFirst().orElseThrow().remainingSeasonal()).isZero();
        assertThatThrownBy(() -> world.spendDevelopmentPoints(Map.of(Attribute.WEDGES, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void trainingAndPointPurchasesUseOneSeasonalAllowanceInBothOrders() {
        WorldConfig longSeason = new WorldConfig(12, 20, 2, 6, 4);
        World world = World.create(506, longSeason);
        String id = world.createPlayer("Training", "Golfer", Nationality.USA, 19, Archetype.ALL_ROUNDER);
        world.setStandingPreparationFocus(PreparationFocus.WEDGES);
        world.advanceSeason(); // bank Development Points for the following season
        while (world.currentSeason() == 2 && world.currentWeek() <= 15) {
            world.advanceWeek();
            while (world.hasPendingPlayerEvent()) {
                world.playerEvent().simEvent();
                world.completePlayerEvent();
            }
        }
        int trained = world.careerOf(id).player().attributeChanges().stream()
                .filter(c -> c.season() == 2 && c.attribute() == Attribute.WEDGES && c.delta() > 0)
                .mapToInt(c -> c.delta()).sum();
        assertThat(trained).isPositive();
        int allowance = world.playerDevelopmentOptions().stream().filter(o -> o.attribute() == Attribute.WEDGES)
                .findFirst().orElseThrow().remainingSeasonal();
        assertThat(allowance).isEqualTo(6 - trained);
        world.spendDevelopmentPoints(Map.of(Attribute.WEDGES, allowance));
        assertThat(world.playerDevelopmentOptions().stream().filter(o -> o.attribute() == Attribute.WEDGES)
                .findFirst().orElseThrow().remainingSeasonal()).isZero();
        world.advanceSeason();
        int boughtSeason = 2;
        assertThat(world.careerOf(id).player().attributeChanges().stream()
                .filter(c -> c.season() == boughtSeason && c.attribute() == Attribute.WEDGES && c.delta() > 0)
                .mapToInt(c -> c.delta()).sum()).isEqualTo(6);

        World reverse = playerWorld(507);
        String reverseId = reverse.playerGolferId().orElseThrow();
        reverse.advanceSeason();
        reverse.spendDevelopmentPoints(Map.of(Attribute.WEDGES, 6));
        reverse.setStandingPreparationFocus(PreparationFocus.WEDGES);
        reverse.advanceSeason();
        assertThat(reverse.careerOf(reverseId).player().attributeChanges().stream()
                .filter(c -> c.season() == 2 && c.attribute() == Attribute.WEDGES && c.delta() > 0)
                .mapToInt(c -> c.delta()).sum()).isEqualTo(6);
        assertThat(reverse.playerPreparation().progressFor(PreparationFocus.WEDGES)).isPositive();
    }

    @Test
    void fatigueRecoveryAndReadinessSurviveSkippedWeeksAndDoNotStack() {
        World world = playerWorld(503);
        String id = world.playerGolferId().orElseThrow();
        world.injectPhysicalStateForTest(id, world.physicalStateOf(id).withFatigue(0.4));
        assertThat(world.commitPlayerPreparation(PreparationFocus.RECOVERY)).isTrue();
        assertThat(world.physicalStateOf(id).fatigue()).isCloseTo(0.22, org.assertj.core.data.Offset.offset(1e-9));
        world.setResting(true);
        world.advanceWeek();
        assertThat(world.commitPlayerPreparation(PreparationFocus.TOURNAMENT_PREPARATION)).isTrue();
        assertThat(world.playerPreparation().ready()).isTrue();
        world.advanceWeek();
        assertThat(world.playerPreparation().ready()).isTrue();
        assertThat(world.commitPlayerPreparation(PreparationFocus.TOURNAMENT_PREPARATION)).isTrue();
        assertThat(world.playerPreparation().ready()).isTrue();
        world.advanceWeek();
        assertThat(world.playerPreparation().ready()).isTrue();
        world.setResting(false);
        world.advanceSeason();
        assertThat(world.playerPreparation().ready()).isFalse();
        assertThat(HealthSystem.TRAINING_FATIGUE).isEqualTo(0.035);
    }

    @Test
    void bulkSimulationPreparesAiAndRestoredWorldCannotDuplicateCurrentWeek() {
        World world = playerWorld(504);
        String ai = world.activeGolferIds().stream().filter(id -> !id.equals(world.playerGolferId().orElseThrow()))
                .findFirst().orElseThrow();
        world.commitPlayerPreparation(PreparationFocus.PUTTING);
        World loaded = World.restore(504, SMALL, world.snapshot());
        assertThat(loaded.commitPlayerPreparation(PreparationFocus.PUTTING)).isFalse();
        assertThat(loaded.playerPreparation()).isEqualTo(world.playerPreparation());
        loaded.advanceSeason();
        CareerPreparation aiState = loaded.snapshot().careerPreparation().get(ai);
        assertThat(aiState.consumedSeason()).isPositive();
        assertThat(aiState.progress().values().stream().mapToDouble(Double::doubleValue).sum()).isPositive();
    }

    @Test
    void tournamentPauseAndResumeKeepTheSingleWeeklyCommit() {
        World world = World.create(505, SMALL);
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);
        world.setStandingPreparationFocus(PreparationFocus.TOURNAMENT_PREPARATION);
        int guard = 0;
        while (!world.hasPendingPlayerEvent() && guard++ < 20) world.advanceWeek();
        assertThat(world.hasPendingPlayerEvent()).isTrue();
        CareerPreparation paused = world.playerPreparation();
        assertThat(paused.consumed(world.currentSeason(), world.currentWeek())).isTrue();
        assertThat(paused.ready()).isFalse();
        assertThat(world.careerOf(id).player().toTournamentGolferState(0).mentalSupport()).isGreaterThanOrEqualTo(
                World.TOURNAMENT_READINESS);
        assertThat(world.careerOf(id).player().toGolferState(0).mentalSupport()).isLessThan(
                world.careerOf(id).player().toTournamentGolferState(0).mentalSupport());
        assertThat(world.commitPlayerPreparation(PreparationFocus.WEDGES)).isFalse();
        assertThatThrownBy(world::advanceWeek).isInstanceOf(IllegalStateException.class);
        assertThat(world.playerPreparation()).isEqualTo(paused);
        world.playerEvent().simEvent();
        world.completePlayerEvent();
        assertThat(world.hasPendingPlayerEvent()).isFalse();
        assertThat(world.playerPreparation().consumed(paused.consumedSeason(), paused.consumedWeek())).isTrue();
    }
}
