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
import com.progolf.sim.progression.TrainingChallengeGenerator;
import com.progolf.sim.progression.TrainingScoring;
import com.progolf.sim.shot.ClubSpec;
import com.progolf.sim.shot.ShotFamilyEligibility;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.shot.AimPoint;
import com.progolf.sim.shot.BallStrikeIntent;
import com.progolf.sim.shot.GroundMotionPoint;
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
    void persistedGreensideChallengeUsesThePhysicalHoleAndSharedTrace() {
        World world = playerWorld(611);
        var session = world.startPlayerTraining(PreparationFocus.WEDGES);
        assertThat(playerWorld(611).startPlayerTraining(PreparationFocus.WEDGES).challenges())
                .isEqualTo(session.challenges());
        assertThat(session.challenges()).hasSize(8).allMatch(TrainingChallenge::greenside);
        assertThat(session.challenges().stream().filter(challenge -> challenge.suggestedFamily()
                == com.progolf.sim.shot.ShotFamily.BUNKER).count()).isGreaterThanOrEqualTo(1);
        int greensideNumber = session.challenges().stream().filter(challenge -> challenge.greenside()
                && challenge.suggestedFamily() != com.progolf.sim.shot.ShotFamily.BUNKER)
                .findFirst().orElseThrow().number();
        for (int number = 1; number < greensideNumber; number++) {
            var challenge = session.challenges().get(number - 1);
            session = world.playerTrainingRangeShot(session.id(), number, new BallStrikeIntent(
                    challenge.suggestedClub(), new AimPoint(challenge.target().x(), challenge.target().y()),
                    challenge.suggestedFamily())).session();
        }
        var challenge = session.challenges().get(greensideNumber - 1);
        var scene = world.playerTrainingGreensideScene();
        assertThat(scene).isNotNull();
        assertThat(scene.model().greenSurface()).isNotNull();
        assertThat(scene.model().greenSurface().footprint()).isEqualTo(scene.model().geometry().regions().stream()
                .filter(region -> region.surface() == com.progolf.sim.spatial.Surface.GREEN).findFirst().orElseThrow().boundary());
        assertThat(scene.ball().position()).isEqualTo(challenge.start());
        assertThat(scene.ball().lie()).isIn(com.progolf.sim.spatial.Surface.FRINGE,
                com.progolf.sim.spatial.Surface.FIRST_CUT, com.progolf.sim.spatial.Surface.PRIMARY_ROUGH,
                com.progolf.sim.spatial.Surface.FAIRWAY);
        assertThat(scene.model().cupPosition()).isEqualTo(challenge.target());
        var beforeShot = world.snapshot();
        World restored = World.restore(611, SMALL, beforeShot);
        assertThat(restored.playerTrainingGreensideScene().ball()).isEqualTo(scene.ball());
        var intent = new BallStrikeIntent(challenge.suggestedClub(),
                new AimPoint(challenge.target().x(), challenge.target().y()), challenge.suggestedFamily());
        var shot = world.playerTrainingRangeShot(session.id(), greensideNumber, intent);
        var pitch = restored.playerTrainingRangeShot(session.id(), greensideNumber,
                new BallStrikeIntent(challenge.suggestedClub(),
                        new AimPoint(challenge.target().x(), challenge.target().y()),
                        com.progolf.sim.shot.ShotFamily.PITCH));
        assertThat(pitch.rangeShot().outcome().trace()).isNotNull();
        restored = World.restore(611, SMALL, beforeShot);
        var replay = restored.playerTrainingRangeShot(session.id(), greensideNumber, intent);
        assertThat(replay.attempt()).isEqualTo(shot.attempt());
        assertThat(replay.rangeShot().outcome().trace()).isEqualTo(shot.rangeShot().outcome().trace());
        assertThat(shot.rangeShot().outcome().trace().origin()).isEqualTo(challenge.start());
        assertThat(shot.rangeShot().outcome().trace().groundPath()).isNotEmpty();
        assertThat(shot.rangeShot().outcome().trace().groundPath().stream()
                .map(GroundMotionPoint::kind)).contains(GroundMotionPoint.Kind.ROLL_STOP);
        assertThat(shot.attempt().finalPosition()).isEqualTo(shot.rangeShot().outcome().trace().finalPoint());
        assertThat(shot.attempt().score()).isEqualTo(TrainingScoring.range(challenge, shot.rangeShot()));
        assertThat(shot.session().baseProgress()).isEqualTo(session.baseProgress());
    }

    @Test
    void newWedgeSessionsKeepEightVariedRealAndReachableGreensideChallenges() {
        for (long seed : new long[]{610, 611, 612}) {
            World world = playerWorld(seed);
            var session = world.startPlayerTraining(PreparationFocus.WEDGES);
            assertThat(session.challenges()).hasSize(8).allMatch(TrainingChallenge::greenside);
            assertThat(session.challenges().stream()
                    .map(TrainingChallenge::suggestedFamily).toList())
                    .contains(com.progolf.sim.shot.ShotFamily.CHIP,
                            com.progolf.sim.shot.ShotFamily.PITCH,
                            com.progolf.sim.shot.ShotFamily.BUNKER);
            assertThat(World.restore(seed, SMALL, world.snapshot()).playerTrainingSession().challenges())
                    .isEqualTo(session.challenges());
            boolean bunkerFound = false;
            var lies = new java.util.HashSet<Surface>();
            var directions = new java.util.HashSet<Integer>();
            var distances = new java.util.ArrayList<Double>();
            for (TrainingChallenge challenge : session.challenges()) {
                assertThat(world.playerTrainingChallengePlayable()).isTrue();
                var scene = world.playerTrainingGreensideScene();
                assertThat(scene).isNotNull();
                Surface lie = scene.model().geometry().surfaceAt(challenge.start());
                lies.add(lie);
                distances.add(challenge.distance());
                double angle = Math.atan2(challenge.start().y() - scene.model().geometry().greenCenter().y(),
                        challenge.start().x() - scene.model().geometry().greenCenter().x());
                directions.add((int) Math.floor((angle + Math.PI) / (Math.PI / 2)));
                assertThat(lie).isNotEqualTo(Surface.GREEN);
                assertThat(scene.ball().lie()).isEqualTo(lie);
                assertThat(scene.model().greenSurface()).isNotNull();
                assertThat(ShotFamilyEligibility.evaluate(lie, ClubSpec.of(challenge.suggestedClub()),
                        challenge.suggestedFamily()).allowed()).isTrue();
                if (lie == Surface.BUNKER) {
                    bunkerFound = true;
                    assertThat(challenge.suggestedFamily()).isEqualTo(com.progolf.sim.shot.ShotFamily.BUNKER);
                    assertThat(scene.model().geometry().regions().stream()
                            .filter(region -> region.surface() == Surface.BUNKER)
                            .anyMatch(region -> region.boundary().stream().anyMatch(point ->
                                    point.distanceTo(challenge.start()) < 15))).isTrue();
                }
                var result = world.playerTrainingRangeShot(session.id(), challenge.number(),
                        new BallStrikeIntent(challenge.suggestedClub(),
                                new AimPoint(challenge.target().x(), challenge.target().y()),
                                challenge.suggestedFamily()));
                assertThat(result.attempt().finalPosition()).isEqualTo(result.rangeShot().outcome().trace().finalPoint());
                assertThat(result.attempt().score()).isEqualTo(TrainingScoring.range(challenge, result.rangeShot()));
            }
            assertThat(bunkerFound).isTrue();
            assertThat(lies.size()).isGreaterThanOrEqualTo(3);
            assertThat(directions.size()).isGreaterThanOrEqualTo(3);
            assertThat(java.util.Collections.max(distances) - java.util.Collections.min(distances))
                    .isGreaterThanOrEqualTo(12);
            assertThat(world.playerTrainingSession().status()).isEqualTo(PlayableTrainingSession.Status.COMPLETED);
        }
    }

    @Test
    void otherTechnicalTrainingFocusesKeepTheirExistingPracticeEnvironments() {
        for (PreparationFocus focus : new PreparationFocus[]{PreparationFocus.DRIVING,
                PreparationFocus.IRONS, PreparationFocus.PUTTING}) {
            var session = playerWorld(620 + focus.ordinal()).startPlayerTraining(focus);
            assertThat(session.challenges()).hasSize(8).noneMatch(TrainingChallenge::greenside);
            assertThat(session.challenges().stream().allMatch(TrainingChallenge::putting))
                    .isEqualTo(focus == PreparationFocus.PUTTING);
        }
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
