package com.progolf.sim;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.*;
import com.progolf.sim.play.HoleToPlay;
import com.progolf.sim.play.PlayableRound;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.population.PopulationGenerator;
import com.progolf.sim.putt.PuttingPolicy;
import com.progolf.sim.shot.*;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.tournament.*;
import com.progolf.sim.weather.PlayingConditions;
import com.progolf.sim.weather.TournamentWeather;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Fixed-seed Generator 8 checks for route decisions, round parity, skill separation and event totals. */
class TournamentScoringConsistencyTest {
    private static final long SEED = 0xCA11B4A7EL;
    private static final CourseSetup SETUP = SetupDifficulty.forEvent(Tier.STANDARD, EventPrestige.REGULAR);

    @Test
    void parThreeTeeAttacksTheReachableGreenInsteadOfItsFairwayLandingZone() {
        Course course = course();
        Attributes attributes = Attributes.uniform(70);
        int parThreesWithPublishedLandingOptions = 0;
        for (int hole = 1; hole <= 18; hole++) {
            if (course.holes().get(hole - 1).par() != 3) continue;
            HoleModel model = model(course, hole);
            BallState tee = new BallState(model.geometry().tee(), Surface.TEE_BOX);
            if (!StrategicTargetPlanner.options(model, tee, model.startDistance(), Surface.TEE_BOX,
                    attributes, Environment.calm()).isEmpty()) parThreesWithPublishedLandingOptions++;
            for (Strategy strategy : Strategy.values()) {
                BallStrikeIntent intent = new StrategyPolicy(strategy).decideIntent(model, tee,
                        model.startDistance(), Surface.TEE_BOX, attributes, 3, Environment.calm());
                Position2d target = new Position2d(intent.aimPoint().x(), intent.aimPoint().y());
                assertThat(target.distanceTo(model.geometry().greenCenter()))
                        .as("hole %d, %s", hole, strategy).isLessThan(10.0);
            }
        }
        assertThat(parThreesWithPublishedLandingOptions).isPositive();
    }

    @Test
    void reachableParFourApproachIgnoresAStillFutureFairwayDetour() {
        Course course = CourseGenerator.generate(new SeedCoordinate(7L, 0, 1, 0, 0, 0, 0),
                EnvironmentClassification.PARKLAND, CourseGenConstants.CURRENT_GENERATOR_VERSION);
        Attributes attributes = Attributes.uniform(70);
        int detoursAvoided = 0;
        for (int golfer = 0; golfer < 40; golfer++) {
            for (int hole = 1; hole <= 18; hole++) {
                if (course.holes().get(hole - 1).par() != 4) continue;
                HoleModel model = model(course, hole);
                RoundOutcome round = RoundResolver.resolveHole(model, attributes, GolferState.fresh(),
                        Environment.calm(), Strategy.BALANCED, coordinate(golfer, hole));
                ShotOutcome teeShot = round.shots().getFirst();
                if (teeShot.hazardEntered() || teeShot.settlement() == null) continue;
                BallState ball = teeShot.settlement().ball();
                double remaining = ball.position().distanceTo(model.cupPosition());
                if (remaining > 200 || ball.lie().recoveryDifficulty() > 0) continue;
                var option = StrategicTargetPlanner.preferred(StrategicTargetPlanner.options(model, ball,
                        remaining, ball.lie(), attributes, Environment.calm()), Strategy.BALANCED);
                if (option == null) continue;
                Position2d detour = new Position2d(option.aimPoint().x(), option.aimPoint().y());
                if (detour.distanceTo(model.geometry().greenCenter()) <= 40) continue;
                BallStrikeIntent intent = new StrategyPolicy(Strategy.BALANCED).decideIntent(model, ball,
                        remaining, ball.lie(), attributes, 4, Environment.calm());
                Position2d target = new Position2d(intent.aimPoint().x(), intent.aimPoint().y());
                assertThat(target.distanceTo(model.geometry().greenCenter()))
                        .as("hole %d, golfer %d", hole, golfer).isLessThan(25.0);
                detoursAvoided++;
            }
        }
        assertThat(detoursAvoided).isPositive();
    }

    @Test
    void automaticAndInteractiveSimulationUseTheSameStrokesAndPenalties() {
        Course course = course();
        ProfessionalGolfer golfer = field().get(0);
        List<HoleToPlay> holes = holes(course);
        List<Integer> automaticHoleScores = new ArrayList<>();
        for (int hole = 1; hole <= 18; hole++) {
            HoleToPlay toPlay = holes.get(hole - 1);
            RoundOutcome outcome = RoundResolver.resolveHole(toPlay.model(), golfer.player().attributes(),
                    golfer.player().toGolferState(0), toPlay.environment(), Strategy.BALANCED, coordinate(0, hole));
            automaticHoleScores.add(outcome.totalStrokes());
            assertThat(outcome.totalStrokes()).isEqualTo(outcome.shots().stream().mapToInt(s -> s.strokes()).sum());
            assertThat(outcome.shots()).allSatisfy(s -> assertThat(s.strokes()).isEqualTo(1 + s.penaltyStrokes()));
        }
        PlayableRound played = new PlayableRound(golfer.player().attributes(), golfer.player().toGolferState(0),
                holes, coordinate(0, 0), Strategy.BALANCED);
        played.simRound();
        assertThat(played.holeScores()).containsExactlyElementsOf(automaticHoleScores);
        assertThat(played.totalStrokes()).isEqualTo(automaticHoleScores.stream().mapToInt(Integer::intValue).sum());
        assertThat(played.scoreVsPar()).isEqualTo(-1);
    }

    @Test
    void fixedFieldRetainsSkillSeparationAndCredibleFourRoundStandings() {
        Course course = course();
        double weakMean = meanRound(course, Attributes.uniform(45));
        double strongMean = meanRound(course, Attributes.uniform(80));
        assertThat(weakMean - strongMean).isGreaterThan(5.0);

        List<ProfessionalGolfer> field = field();
        TournamentDefinition definition = new TournamentDefinition("Fixed Standard Open", course, Tier.STANDARD,
                EventPrestige.REGULAR, new EntryRequirements(field.size(), true), PrizeStructure.standard(),
                new TournamentFormat(4, true, 11), LocalDate.of(2000, 6, 1), SEED, 1, 1,
                PinPlacementVersion.V5_EFFECTIVE_GREEN, PuttingPolicy.LEGACY);
        Tournament tournament = new Tournament(definition, TournamentWeather.calm(), SETUP);
        tournament.openRegistration();
        field.forEach(tournament::register);
        tournament.confirmField();
        TournamentResult result = tournament.playToCompletion();
        assertThat(result.cutResult().madeCutCount()).isEqualTo(11);
        assertThat(result.finishingOrder().get(0).score()).isEqualTo(-7);
        assertThat(result.finishingOrder().get(0).golfer()).isEqualTo(result.winner());
        assertThat(result.finishingOrder()).extracting(TournamentResult.Finish::score).startsWith(-7, 0, 0);
        result.finishingOrder().forEach(finish -> {
            List<Integer> rounds = tournament.roundScoresOf(finish.golfer().player().id());
            assertThat(finish.score()).isEqualTo(rounds.stream().mapToInt(Integer::intValue).sum());
            assertThat(rounds).hasSize(finish.madeCut() ? 4 : 2);
        });
    }

    private static double meanRound(Course course, Attributes attributes) {
        int strokes = 0;
        for (int golfer = 0; golfer < 24; golfer++) {
            for (int hole = 1; hole <= 18; hole++) {
                strokes += RoundResolver.resolveHole(model(course, hole), attributes, GolferState.fresh(),
                        Environment.calm(), Strategy.BALANCED, coordinate(golfer, hole)).totalStrokes();
            }
        }
        return strokes / 24.0 - course.totalPar();
    }

    private static List<HoleToPlay> holes(Course course) {
        List<HoleToPlay> holes = new ArrayList<>(18);
        for (int hole = 1; hole <= 18; hole++) {
            holes.add(new HoleToPlay(model(course, hole), course.holes().get(hole - 1).par(),
                    PlayingConditions.calm().environmentForHole(hole,
                            course.identity().classification().exposure() * SETUP.windScale())));
        }
        return holes;
    }

    private static HoleModel model(Course course, int hole) {
        return course.holeModel(hole, 1, SETUP, PinPlacementVersion.V5_EFFECTIVE_GREEN, PuttingPolicy.LEGACY);
    }

    private static Course course() {
        return CourseGenerator.generate(new SeedCoordinate(SEED, 1, 1, 0, 0, 0, 0),
                EnvironmentClassification.PARKLAND, CourseGenConstants.CURRENT_GENERATOR_VERSION);
    }

    private static List<ProfessionalGolfer> field() {
        return PopulationGenerator.generate(new SeedCoordinate(SEED, 2, 1, 0, 0, 0, 0), 24);
    }

    private static SeedCoordinate coordinate(int golfer, int hole) {
        return new SeedCoordinate(SEED, 1, 1, 1, golfer, hole, 0);
    }
}
