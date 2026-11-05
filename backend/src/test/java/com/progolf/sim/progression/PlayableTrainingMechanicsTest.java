package com.progolf.sim.progression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Handedness;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.practice.PracticePuttingGreen;
import com.progolf.sim.practice.PracticeRange;
import com.progolf.sim.shot.AimPoint;
import com.progolf.sim.shot.BallStrikeIntent;
import com.progolf.sim.shot.Club;
import com.progolf.sim.shot.ClubId;
import com.progolf.sim.shot.ClubSpec;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.ShotExecutionProfile;
import com.progolf.sim.shot.ShotFamily;
import com.progolf.sim.shot.ShotGuidance;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.putt.PhysicalPuttIntent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayableTrainingMechanicsTest {
    @Test
    void calibratedClickAimReachesRepresentativeFinalZonesWithTheRealResolver() {
        Attributes skills = Attributes.uniform(55);
        for (PreparationFocus focus : List.of(PreparationFocus.DRIVING, PreparationFocus.IRONS,
                PreparationFocus.WEDGES)) {
            var challenges = TrainingChallengeGenerator.generate(focus, skills, 12045L);
            assertThat(challenges).hasSize(8);
            for (TrainingChallenge challenge : challenges) {
                var aim = TrainingChallengeGenerator.assessRangeAim(challenge, skills,
                        GolferState.fresh(), Handedness.RIGHT, 12045L).orElseThrow();
                assertThat(aim.intendedCarry()).isBetween(15.0, 320.0);
                assertThat(Math.abs(aim.medianRestingDistance() - challenge.distance()))
                        .isLessThanOrEqualTo(Math.max(2.0, challenge.tolerance() * 0.2));
                assertThat(aim.medianProximity()).isLessThanOrEqualTo(challenge.tolerance());
                assertThat(aim.meanScore()).isGreaterThanOrEqualTo(0.5);
                assertThat(aim.medianActualCarry()).isLessThan(aim.medianRestingDistance());
                // Independent resolver seeds catch a lucky calibration set hiding systematic rollout.
                List<Double> independentRests = new ArrayList<>();
                double independentScore = 0;
                double factor = aim.intendedCarry() / challenge.distance();
                for (int sample = 0; sample < 64; sample++) {
                    PracticeRange range = new PracticeRange(skills, GolferState.fresh(), Handedness.RIGHT,
                            1000L + sample);
                    var shot = range.hit(new BallStrikeIntent(challenge.suggestedClub(),
                            new AimPoint(challenge.start().x() + (challenge.target().x() - challenge.start().x()) * factor,
                                    challenge.start().y() + (challenge.target().y() - challenge.start().y()) * factor),
                            challenge.suggestedFamily()), PracticeRange.bays().indexOf(challenge.start()) + 1, "0");
                    independentRests.add(shot.totalDistance());
                    independentScore += TrainingScoring.range(challenge, shot);
                }
                Collections.sort(independentRests);
                assertThat(Math.abs(independentRests.get(32) - challenge.distance()))
                        .as("independent resting bias for %s challenge %s", focus, challenge.number())
                        .isLessThanOrEqualTo(Math.max(3.0, challenge.tolerance() * 0.5));
                assertThat(independentScore / 64).isGreaterThanOrEqualTo(0.45);
            }
        }
    }

    @Test
    void resolverProbeRejectsReachableGeometryWithNoRealisticRestingOutcome() {
        Attributes skills = Attributes.uniform(55);
        TrainingChallenge ordinary = TrainingChallengeGenerator.generate(PreparationFocus.DRIVING,
                skills, 12045L).get(0);
        TrainingChallenge unreasonablyTight = new TrainingChallenge(1, ordinary.start(), ordinary.target(), 0,
                ordinary.suggestedClub(), ordinary.suggestedFamily(), 0.25, 0);
        assertThat(TrainingChallengeGenerator.compatible(PreparationFocus.DRIVING, unreasonablyTight,
                skills, GolferState.fresh())).isTrue();
        assertThat(TrainingChallengeGenerator.assessRangeAim(unreasonablyTight, skills,
                GolferState.fresh(), Handedness.RIGHT, 12045L)).isEmpty();
    }

    @Test
    void sequenceVariesRealClubsDistancesDirectionsAndPuttingCups() {
        for (int skill : List.of(55, 85)) {
            for (PreparationFocus focus : List.of(PreparationFocus.DRIVING, PreparationFocus.IRONS,
                    PreparationFocus.WEDGES, PreparationFocus.PUTTING)) {
                var sequence = TrainingChallengeGenerator.generate(focus, Attributes.uniform(skill), 12045L);
                assertThat(sequence).hasSize(8);
                for (int index = 1; index < sequence.size(); index++)
                    assertThat(sequence.get(index).start().equals(sequence.get(index - 1).start())
                            && sequence.get(index).target().equals(sequence.get(index - 1).target())
                            && sequence.get(index).suggestedClub() == sequence.get(index - 1).suggestedClub()).isFalse();
                if (focus == PreparationFocus.PUTTING) {
                    assertThat(sequence.stream().map(TrainingChallenge::cupNumber).distinct().count()).isGreaterThanOrEqualTo(4);
                    assertThat(sequence.stream().map(TrainingChallenge::start).distinct().count()).isEqualTo(8);
                    assertThat(sequence.stream().map(c -> Math.round(c.distance() * 3)).distinct().count())
                            .isGreaterThanOrEqualTo(4);
                } else {
                    assertThat(sequence.stream().map(TrainingChallenge::target).distinct().count()).isGreaterThanOrEqualTo(2);
                    assertThat(sequence.stream().map(TrainingChallenge::start).distinct().count()).isGreaterThanOrEqualTo(2);
                    assertThat(sequence.stream().map(c -> List.of(c.start(), c.target(), c.suggestedClub(), c.suggestedFamily())).distinct().count()).isEqualTo(8);
                    assertThat(sequence.stream().map(c -> Math.round(c.distance() / 10)).distinct().count())
                            .isGreaterThanOrEqualTo(3);
                    assertThat(sequence.stream().map(c -> Math.signum(c.target().x())).distinct().count())
                            .isGreaterThanOrEqualTo(2);
                    if (focus != PreparationFocus.DRIVING)
                        assertThat(sequence.stream().map(TrainingChallenge::suggestedClub).distinct().count())
                                .isGreaterThanOrEqualTo(2);
                }
            }
        }
    }

    @Test
    void variedSessionsRemainAvailableAcrossDeterministicSeeds() {
        for (long seed = 0; seed < 12; seed++) {
            for (PreparationFocus focus : List.of(PreparationFocus.DRIVING, PreparationFocus.IRONS,
                    PreparationFocus.WEDGES, PreparationFocus.PUTTING)) {
                var sequence = TrainingChallengeGenerator.generate(focus, Attributes.uniform(55), seed);
                assertThat(sequence).hasSize(8);
                assertThat(sequence.stream().map(TrainingChallenge::target).distinct().count())
                        .isGreaterThanOrEqualTo(focus == PreparationFocus.PUTTING ? 5 : 2);
            }
        }
    }

    @Test
    void rangeReachReadoutUsesTheResolversEquipmentAndTechniqueCap() {
        Attributes skills = Attributes.uniform(55);
        GolferState equipment = new GolferState(0, 0, 0, 0.12);
        var reaches = new PracticeRange(skills, equipment, Handedness.RIGHT, 44L).expectedReaches();
        var driver = reaches.stream().filter(r -> r.club() == ClubId.DRIVER && r.family() == ShotFamily.FULL)
                .findFirst().orElseThrow();
        var controlled = reaches.stream().filter(r -> r.club() == ClubId.DRIVER && r.family() == ShotFamily.CONTROLLED)
                .findFirst().orElseThrow();
        double normal = ShotGuidance.clubReaches(skills, Surface.TEE_BOX).stream()
                .filter(r -> r.club() == ClubId.DRIVER).findFirst().orElseThrow().normalReach();
        assertThat(driver.yards()).isEqualTo(normal * 1.12);
        assertThat(controlled.yards()).isEqualTo(normal * 1.12 * 0.82);
        assertThat(reaches.stream().filter(r -> r.club() == ClubId.NINE_IRON && r.family() == ShotFamily.FULL))
                .singleElement().satisfies(r -> assertThat(r.yards()).isPositive());
    }

    @Test
    void eightChallengesPerFocusStayDeterministicAndInsideExistingPracticeGeometry() {
        for (PreparationFocus focus : List.of(PreparationFocus.DRIVING, PreparationFocus.IRONS,
                PreparationFocus.WEDGES, PreparationFocus.PUTTING)) {
            for (int skill : List.of(55, 85)) {
                var first = TrainingChallengeGenerator.generate(focus, Attributes.uniform(skill), 12045L);
                assertThat(first).hasSize(8).isEqualTo(
                        TrainingChallengeGenerator.generate(focus, Attributes.uniform(skill), 12045L));
                assertThat(first).extracting(TrainingChallenge::number).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
                assertThat(first.stream().map(TrainingChallenge::target).distinct().count()).isGreaterThan(1);
                assertThat(first).allSatisfy(challenge -> {
                    assertThat(challenge.tolerance()).isPositive();
                    assertThat(TrainingChallengeGenerator.compatible(focus, challenge,
                            Attributes.uniform(skill), GolferState.fresh())).isTrue();
                    if (focus == PreparationFocus.PUTTING) {
                        assertThat(challenge.putting()).isTrue();
                        assertThat(PracticePuttingGreen.surfaceView().contains(challenge.start())).isTrue();
                        assertThat(challenge.target()).isEqualTo(
                                PracticePuttingGreen.cups().get(challenge.cupNumber() - 1).position());
                        assertThat(challenge.distance()).isBetween(1.8, 6.5);
                        assertThat(PracticePuttingGreen.surfaceView().gradientAt(challenge.start()).magnitude())
                                .isLessThanOrEqualTo(0.15);
                        assertThat(challenge.slope()).isEqualTo(
                                PracticePuttingGreen.surfaceView().gradientAt(challenge.start()).magnitude());
                    } else {
                        assertThat(challenge.putting()).isFalse();
                        assertThat(PracticeRange.bays()).contains(challenge.start());
                        assertThat(PracticeRange.geometryView().surfaceAt(challenge.target()))
                                .isEqualTo(Surface.GREEN);
                        assertThat(TrainingChallengeGenerator.landingZone(challenge)).isFalse();
                        assertThat(PracticeRange.targets().stream().map(PracticeRange.Target::position)).contains(challenge.target());
                        assertThat(TrainingChallengeGenerator.targetLabel(challenge)).endsWith(" flag");
                        assertThat(challenge.target().y()).isBetween(15.0, 320.0);
                        assertThat(Math.abs(challenge.target().x())).isLessThanOrEqualTo(PracticeRange.aimHalfWidth());
                        assertThat(challenge.slope()).isZero();
                        double reach = ShotGuidance.clubReaches(Attributes.uniform(skill), Surface.TEE_BOX).stream()
                                .filter(club -> club.club() == challenge.suggestedClub())
                                .findFirst().orElseThrow().normalReach();
                        assertThat(challenge.distance()).isLessThanOrEqualTo(reach);
                        assertThat(challenge.distance()).isBetween(15.0, 320.0);
                        Club required = focus == PreparationFocus.DRIVING ? Club.DRIVER
                                : focus == PreparationFocus.IRONS ? Club.IRON : Club.WEDGE;
                        assertThat(ClubSpec.of(challenge.suggestedClub()).family()).isEqualTo(required);
                        double effectiveReach = reach * ShotExecutionProfile.derive(Surface.TEE_BOX,
                                ClubSpec.of(challenge.suggestedClub()), challenge.suggestedFamily(),
                                Attributes.uniform(skill), Environment.calm()).effectiveCarryCapMultiplier();
                        assertThat(challenge.distance()).isBetween(effectiveReach * 0.68 - 1e-8,
                                effectiveReach * 0.99 + 1e-8);
                    }
                });
            }
            assertThat(TrainingChallengeGenerator.generate(focus, Attributes.uniform(55), 12045L))
                    .isNotEqualTo(TrainingChallengeGenerator.generate(focus, Attributes.uniform(55), 12046L));
        }
    }

    @Test
    void rangeChallengesRespectRealEquipmentReachAndOldIncompatibleObjectivesAreDetected() {
        var state = new GolferState(0, 0, 0, 0.12);
        for (int skill : List.of(55, 85)) {
            Attributes attributes = Attributes.uniform(skill);
            for (PreparationFocus focus : List.of(PreparationFocus.DRIVING, PreparationFocus.IRONS,
                    PreparationFocus.WEDGES)) {
                var challenges = TrainingChallengeGenerator.generate(focus, attributes, state, null, 491L);
                assertThat(challenges).hasSize(8).allSatisfy(challenge ->
                        assertThat(TrainingChallengeGenerator.compatible(focus, challenge, attributes, state)).isTrue());
            }
        }
        TrainingChallenge oldIron = new TrainingChallenge(1, PracticeRange.bay(),
                PracticeRange.targets().get(2).position(), 0, ClubId.NINE_IRON, ShotFamily.CONTROLLED, 20, 0);
        assertThat(TrainingChallengeGenerator.compatible(PreparationFocus.IRONS, oldIron,
                Attributes.uniform(55), GolferState.fresh())).isTrue();
        TrainingChallenge oldDriver = new TrainingChallenge(1, PracticeRange.bay(),
                PracticeRange.targets().get(6).position(), 0, ClubId.DRIVER, ShotFamily.FULL, 44, 0);
        assertThat(TrainingChallengeGenerator.compatible(PreparationFocus.DRIVING, oldDriver,
                Attributes.uniform(85), GolferState.fresh())).isTrue();
        assertThat(TrainingChallengeGenerator.landingZone(oldDriver)).isFalse();
        assertThat(TrainingChallengeGenerator.targetLabel(oldDriver)).isEqualTo("290 yd flag");
        TrainingChallenge savedZone = new TrainingChallenge(1, PracticeRange.bay(),
                new Position2d(0, 207), 0, ClubId.DRIVER, ShotFamily.FULL, 42, 0);
        assertThat(TrainingChallengeGenerator.compatible(PreparationFocus.DRIVING, savedZone,
                Attributes.uniform(55), GolferState.fresh())).isTrue();
        assertThat(TrainingChallengeGenerator.landingZone(savedZone)).isTrue();
    }

    @Test
    void noAchievableDriverChallengeFailsBeforeAPlayableCommitCanBeMade() {
        assertThatThrownBy(() -> TrainingChallengeGenerator.generate(PreparationFocus.DRIVING,
                Attributes.uniform(55), new GolferState(0, 0, 0, 1), Handedness.RIGHT, 919L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no achievable");
        assertThatThrownBy(() -> TrainingChallengeGenerator.generate(PreparationFocus.WEDGES,
                Attributes.uniform(20), GolferState.fresh(), Handedness.RIGHT, 7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no achievable varied wedges training session");
    }

    @Test
    void scoresOnlyResolvedShotsAndCapsTheEightAttemptBonus() {
        Attributes skills = Attributes.uniform(65);
        TrainingChallenge rangeChallenge = TrainingChallengeGenerator.generate(PreparationFocus.WEDGES, skills, 7).get(0);
        var range = new PracticeRange(skills, GolferState.fresh(), null, 90L);
        var intent = new BallStrikeIntent(rangeChallenge.suggestedClub(),
                new AimPoint(rangeChallenge.target().x(), rangeChallenge.target().y()),
                rangeChallenge.suggestedFamily());
        var rangeResult = range.hit(intent, 4, "0");
        double rangeScore = TrainingScoring.range(rangeChallenge, rangeResult);
        assertThat(rangeScore).isBetween(0.0, 1.0);
        assertThat(TrainingScoring.range(rangeChallenge, rangeResult)).isEqualTo(rangeScore);

        TrainingChallenge puttChallenge = TrainingChallengeGenerator.generate(PreparationFocus.PUTTING, skills, 7).get(0);
        var green = new PracticePuttingGreen(skills, GolferState.fresh(), 90L);
        green.placeBall(puttChallenge.start(), "0");
        Position2d direction = new Position2d(puttChallenge.target().x() - puttChallenge.start().x(),
                puttChallenge.target().y() - puttChallenge.start().y());
        var puttResult = green.putt(puttChallenge.cupNumber(),
                new PhysicalPuttIntent(direction, puttChallenge.distance() * 3), "1");
        double puttScore = TrainingScoring.putt(puttChallenge, puttResult);
        assertThat(puttScore).isBetween(0.0, 1.0);
        assertThat(TrainingScoring.putt(puttChallenge, puttResult)).isEqualTo(puttScore);
        assertThatThrownBy(() -> TrainingScoring.putt(rangeChallenge, puttResult))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(TrainingScoring.bonusFraction(0.0)).isZero();
        assertThat(TrainingScoring.bonusFraction(0.35)).isZero();
        assertThat(TrainingScoring.bonusFraction(0.675)).isCloseTo(0.075,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(TrainingScoring.bonusFraction(1.0)).isEqualTo(0.15);
        assertThatThrownBy(() -> TrainingScoring.bonusFraction(1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void puttingPlacementRemainsValidAcrossManyDeterministicSessions() {
        for (long seed = 0; seed < 40; seed++) {
            for (int skill : List.of(20, 85)) {
                assertThat(TrainingChallengeGenerator.generate(PreparationFocus.PUTTING,
                        Attributes.uniform(skill), seed)).allSatisfy(challenge -> {
                    assertThat(PracticePuttingGreen.surfaceView().contains(challenge.start())).isTrue();
                    assertThat(challenge.distance()).isBetween(1.8, 6.5);
                    assertThat(challenge.slope()).isLessThanOrEqualTo(0.15);
                });
            }
        }
    }

    @Test
    void suggestedRangeShotsCanScoreOnTheGeneratedObjectives() {
        for (int skill : List.of(55, 85)) {
            for (PreparationFocus focus : List.of(PreparationFocus.DRIVING, PreparationFocus.IRONS,
                    PreparationFocus.WEDGES)) {
                Attributes skills = Attributes.uniform(skill);
                double total = 0;
                var challenges = TrainingChallengeGenerator.generate(focus, skills, 881L);
                for (TrainingChallenge challenge : challenges) {
                    PracticeRange range = new PracticeRange(skills, GolferState.fresh(), null, 700L + challenge.number());
                    var feasible = TrainingChallengeGenerator.assessRangeAim(challenge, skills,
                            GolferState.fresh(), Handedness.RIGHT, 881L).orElseThrow();
                    double fraction = feasible.intendedCarry() / challenge.distance();
                    var shot = range.hit(new BallStrikeIntent(challenge.suggestedClub(),
                            new AimPoint(challenge.start().x() + (challenge.target().x() - challenge.start().x()) * fraction,
                                    challenge.start().y() + (challenge.target().y() - challenge.start().y()) * fraction),
                            challenge.suggestedFamily()), PracticeRange.bays().indexOf(challenge.start()) + 1, "0");
                    total += TrainingScoring.range(challenge, shot);
                }
                assertThat(total / 8.0).as("suggested %s challenges at skill %s", focus, skill)
                        .isGreaterThanOrEqualTo(0.4);
            }
        }
    }
}
