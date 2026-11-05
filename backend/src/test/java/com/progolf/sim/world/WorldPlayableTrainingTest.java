package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.course.Position2d;
import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.player.AttributeChange;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.progression.PlayableTrainingSession;
import com.progolf.sim.progression.PreparationFocus;
import com.progolf.sim.progression.TrainingChallenge;
import com.progolf.sim.shot.AimPoint;
import com.progolf.sim.shot.BallStrikeIntent;
import com.progolf.sim.putt.PhysicalPuttIntent;
import org.junit.jupiter.api.Test;

class WorldPlayableTrainingTest {
    private static final WorldConfig SMALL = new WorldConfig(24, 6, 2, 12, 4);

    private World playerWorld(long seed) {
        World world = World.create(seed, SMALL);
        world.createPlayer("Playable", "Training", Nationality.USA, 19, Archetype.ALL_ROUNDER);
        return world;
    }

    @Test
    void playableStartAwardsOnlyBaseAndExcludesSimulationForTheWeek() {
        World world = playerWorld(610);
        world.setStandingPreparationFocus(PreparationFocus.PUTTING);
        PlayableTrainingSession session = world.startPlayerTraining(PreparationFocus.WEDGES);
        String player = world.playerGolferId().orElseThrow();
        assertThat(session.status()).isEqualTo(PlayableTrainingSession.Status.ACTIVE);
        assertThat(session.challenges()).hasSize(8);
        assertThat(world.playerTrainingChallengePlayable()).isTrue();
        assertThat(session.baseProgress()).isEqualTo(0.95);
        assertThat(session.bonusProgress()).isZero();
        assertThat(world.playerPreparation().progressFor(PreparationFocus.WEDGES)).isEqualTo(0.95);
        assertThat(world.playerPreparation().standingFocus()).isEqualTo(PreparationFocus.PUTTING);
        assertThat(world.playerPreparation().committedFocus()).isEqualTo(PreparationFocus.WEDGES);
        assertThat(world.physicalStateOf(player).fatigue()).isEqualTo(0.035);
        assertThat(world.startPlayerTraining(PreparationFocus.WEDGES)).isEqualTo(session);
        assertThatThrownBy(() -> world.startPlayerTraining(PreparationFocus.DRIVING))
                .isInstanceOf(IllegalStateException.class);
        assertThat(world.commitPlayerPreparation(PreparationFocus.WEDGES)).isFalse();
        assertThat(world.playerPreparation().progressFor(PreparationFocus.WEDGES)).isEqualTo(0.95);
        World simulated = playerWorld(615);
        assertThat(simulated.commitPlayerPreparation(PreparationFocus.WEDGES)).isTrue();
        assertThatThrownBy(() -> simulated.startPlayerTraining(PreparationFocus.WEDGES))
                .isInstanceOf(IllegalStateException.class);
        assertThat(simulated.playerTrainingSession()).isNull();
    }

    @Test
    void impossibleChallengeGenerationLeavesTheWeeklyOpportunityUntouched() {
        World world = playerWorld(699);
        String playerId = world.playerGolferId().orElseThrow();
        var player = world.careerOf(playerId).player();
        player.state().setEquipment(0, 1, 0, 0);
        assertThatThrownBy(() -> world.startPlayerTraining(PreparationFocus.DRIVING))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no achievable driving");
        assertThat(world.playerPreparationAvailable()).isTrue();
        assertThat(world.playerTrainingSession()).isNull();
        assertThat(world.playerPreparation().progressFor(PreparationFocus.DRIVING)).isZero();
        assertThat(world.physicalStateOf(playerId).fatigue()).isZero();
    }

    @Test
    void incompatibleUnfinishedChallengePreservesBaseAndRejectsAnAttempt() {
        World world = playerWorld(698);
        String playerId = world.playerGolferId().orElseThrow();
        var session = world.startPlayerTraining(PreparationFocus.DRIVING);
        world.careerOf(playerId).player().state().setEquipment(0, 1, 0, 0);
        assertThat(world.playerTrainingChallengePlayable()).isFalse();
        var challenge = session.challenges().get(0);
        assertThatThrownBy(() -> world.playerTrainingRangeShot(session.id(), 1,
                new BallStrikeIntent(challenge.suggestedClub(),
                        new AimPoint(challenge.target().x(), challenge.target().y()), challenge.suggestedFamily())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incompatible");
        assertThat(world.playerTrainingSession().attempts()).isEmpty();
        assertThat(world.playerPreparation().progressFor(PreparationFocus.DRIVING))
                .isEqualTo(session.baseProgress());
        assertThat(world.abandonPlayerTraining(session.id()).bonusProgress()).isZero();
    }

    @Test
    void rangeAttemptsAdvanceOnceAndSettleOneBoundedBonus() {
        World world = playerWorld(611);
        PlayableTrainingSession session = world.startPlayerTraining(PreparationFocus.WEDGES);
        PlayableTrainingSession started = session;
        assertThatThrownBy(() -> world.playerTrainingRangeShot(started.id(), 2,
                new BallStrikeIntent(started.challenges().get(1).suggestedClub(), new AimPoint(0, 75))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(world.playerTrainingSession().attempts()).isEmpty();
        for (int number = 1; number <= 8; number++) {
            TrainingChallenge challenge = session.challenges().get(number - 1);
            BallStrikeIntent intent = new BallStrikeIntent(challenge.suggestedClub(),
                    new AimPoint(challenge.target().x(), challenge.target().y()), challenge.suggestedFamily());
            var shot = world.playerTrainingRangeShot(session.id(), number, intent);
            session = shot.session();
            assertThat(shot.attempt().number()).isEqualTo(number);
            assertThat(shot.attempt().score()).isBetween(0.0, 1.0);
            assertThat(shot.rangeShot()).isNotNull();
            var duplicate = world.playerTrainingRangeShot(session.id(), number, intent);
            assertThat(duplicate.attempt()).isEqualTo(shot.attempt());
            assertThat(duplicate.rangeShot()).isNull();
            assertThat(duplicate.session()).isEqualTo(session);
        }
        assertThat(session.status()).isEqualTo(PlayableTrainingSession.Status.COMPLETED);
        assertThat(session.nextChallenge()).isZero();
        assertThat(session.attempts()).hasSize(8);
        assertThat(session.bonusSettled()).isTrue();
        assertThat(session.bonusProgress()).isBetween(0.0, session.baseProgress() * 0.15);
        assertThat(world.playerPreparation().progressFor(PreparationFocus.WEDGES))
                .isCloseTo(session.baseProgress() + session.bonusProgress(),
                        org.assertj.core.data.Offset.offset(1e-9));
        PlayableTrainingSession completed = session;
        assertThatThrownBy(() -> world.playerTrainingRangeShot(completed.id(), 9,
                new BallStrikeIntent(completed.challenges().get(0).suggestedClub(), new AimPoint(0, 100))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(world.commitPlayerPreparation(PreparationFocus.WEDGES)).isFalse();
        World restored = World.restore(611, SMALL, world.snapshot());
        TrainingChallenge last = session.challenges().get(7);
        var retry = restored.playerTrainingRangeShot(session.id(), 8,
                new BallStrikeIntent(last.suggestedClub(), new AimPoint(last.target().x(), last.target().y()),
                        last.suggestedFamily()));
        assertThat(retry.rangeShot()).isNull();
        assertThat(retry.session()).isEqualTo(session);
        assertThat(restored.playerPreparation().progressFor(PreparationFocus.WEDGES))
                .isEqualTo(world.playerPreparation().progressFor(PreparationFocus.WEDGES));
    }

    @Test
    void puttingUsesTheExistingPhysicalSolverAndResumesAfterSnapshot() {
        World world = playerWorld(612);
        PlayableTrainingSession session = world.startPlayerTraining(PreparationFocus.PUTTING);
        TrainingChallenge challenge = session.challenges().get(0);
        Position2d direction = new Position2d(challenge.target().x() - challenge.start().x(),
                challenge.target().y() - challenge.start().y());
        var shot = world.playerTrainingPutt(session.id(), 1,
                new PhysicalPuttIntent(direction, challenge.distance() * 3));
        assertThat(shot.putt()).isNotNull();
        assertThat(shot.session().nextChallenge()).isEqualTo(2);
        World restored = World.restore(612, SMALL, world.snapshot());
        assertThat(restored.playerTrainingSession()).isEqualTo(shot.session());
        var duplicate = restored.playerTrainingPutt(session.id(), 1,
                new PhysicalPuttIntent(direction, challenge.distance() * 3));
        assertThat(duplicate.putt()).isNull();
        assertThat(duplicate.attempt()).isEqualTo(shot.attempt());
        assertThat(restored.playerTrainingSession().nextChallenge()).isEqualTo(2);
        PlayableTrainingSession resumed = restored.playerTrainingSession();
        for (int number = 2; number <= 8; number++) {
            TrainingChallenge next = resumed.challenges().get(number - 1);
            Position2d line = new Position2d(next.target().x() - next.start().x(),
                    next.target().y() - next.start().y());
            resumed = restored.playerTrainingPutt(session.id(), number,
                    new PhysicalPuttIntent(line, next.distance() * 3)).session();
        }
        assertThat(resumed.status()).isEqualTo(PlayableTrainingSession.Status.COMPLETED);
        assertThat(resumed.attempts()).hasSize(8);
        assertThat(resumed.bonusSettled()).isTrue();
        assertThat(resumed.bonusProgress()).isBetween(0.0, resumed.baseProgress() * 0.15);
    }

    @Test
    void abandonmentAndAdvancementKeepBaseButNeverGrantAPlayableBonus() {
        World abandoned = playerWorld(613);
        PlayableTrainingSession session = abandoned.startPlayerTraining(PreparationFocus.IRONS);
        assertThat(abandoned.abandonPlayerTraining(session.id()).status())
                .isEqualTo(PlayableTrainingSession.Status.ABANDONED);
        assertThat(abandoned.abandonPlayerTraining(session.id()).bonusProgress()).isZero();
        assertThat(abandoned.playerPreparation().progressFor(PreparationFocus.IRONS)).isEqualTo(0.95);
        assertThatThrownBy(() -> abandoned.playerTrainingRangeShot(session.id(), 1,
                new BallStrikeIntent(session.challenges().get(0).suggestedClub(), new AimPoint(0, 100))))
                .isInstanceOf(IllegalStateException.class);

        World advanced = playerWorld(614);
        PlayableTrainingSession active = advanced.startPlayerTraining(PreparationFocus.DRIVING);
        advanced.advanceWeek();
        assertThat(advanced.playerTrainingSession().status()).isEqualTo(PlayableTrainingSession.Status.ABANDONED);
        assertThat(advanced.playerTrainingSession().bonusProgress()).isZero();
        assertThat(advanced.playerPreparation().progressFor(PreparationFocus.DRIVING)).isEqualTo(0.95);
        assertThatThrownBy(() -> advanced.playerTrainingRangeShot(active.id(), 1,
                new BallStrikeIntent(active.challenges().get(0).suggestedClub(), new AimPoint(0, 200))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void playableBaseAndBonusRespectDevelopmentPointSeasonalLimit() {
        World world = playerWorld(507);
        String player = world.playerGolferId().orElseThrow();
        world.advanceSeason();
        world.spendDevelopmentPoints(java.util.Map.of(Attribute.WEDGES, 6));
        int pointsAfterPurchase = world.playerDevelopmentPoints();
        int wedgesBefore = world.careerOf(player).player().attributes().get(Attribute.WEDGES);
        PlayableTrainingSession session = world.startPlayerTraining(PreparationFocus.WEDGES);
        for (int number = 1; number <= 8; number++) {
            TrainingChallenge challenge = session.challenges().get(number - 1);
            session = world.playerTrainingRangeShot(session.id(), number,
                    new BallStrikeIntent(challenge.suggestedClub(),
                            new AimPoint(challenge.target().x(), challenge.target().y()),
                            challenge.suggestedFamily())).session();
        }
        assertThat(session.status()).isEqualTo(PlayableTrainingSession.Status.COMPLETED);
        assertThat(session.attributeImprovements()).isEmpty();
        assertThat(world.careerOf(player).player().attributes().get(Attribute.WEDGES)).isEqualTo(wedgesBefore);
        assertThat(world.playerDevelopmentPoints()).isEqualTo(pointsAfterPurchase);
        assertThat(world.playerDevelopmentOptions().stream().filter(o -> o.attribute() == Attribute.WEDGES)
                .findFirst().orElseThrow().remainingSeasonal()).isZero();
        assertThat(world.playerPreparation().progressFor(PreparationFocus.WEDGES)).isPositive();
    }

    @Test
    void bulkSeasonAdvanceAbandonsAnActiveSessionAndContinuesAutomaticPreparation() {
        World world = playerWorld(616);
        PlayableTrainingSession session = world.startPlayerTraining(PreparationFocus.PUTTING);
        world.advanceSeason();
        assertThat(world.currentSeason()).isEqualTo(2);
        assertThat(world.playerTrainingSession().id()).isEqualTo(session.id());
        assertThat(world.playerTrainingSession().status()).isEqualTo(PlayableTrainingSession.Status.ABANDONED);
        assertThat(world.playerTrainingSession().bonusProgress()).isZero();
        assertThat(world.playerPreparation().progressFor(PreparationFocus.PUTTING)).isEqualTo(0.95);
        assertThat(world.playerPreparation().progressFor(PreparationFocus.DRIVING)).isPositive();
    }
}
