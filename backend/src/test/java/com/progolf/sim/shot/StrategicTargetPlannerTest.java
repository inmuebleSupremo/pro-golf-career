package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.CourseGenConstants;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.GreenComplexPlan;
import com.progolf.sim.course.GreenSide;
import com.progolf.sim.course.GreenSurroundRole;
import com.progolf.sim.course.HoleRoute;
import com.progolf.sim.course.HoleSpatialPlan;
import com.progolf.sim.course.LandingZone;
import com.progolf.sim.course.LandingZoneRole;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.course.PreferredApproachSide;
import com.progolf.sim.course.ReferenceCarryBand;
import com.progolf.sim.course.TerrainRegion;
import com.progolf.sim.spatial.LateralRegion;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.spatial.ZoneBand;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Small deterministic V4-shaped fixtures for truthful, shared strategic landing guidance. */
class StrategicTargetPlannerTest {
    private static final Attributes PLAYER = Attributes.uniform(72);

    @Test
    void strategicParFivePublishesDistinctSafePrimaryAndAggressiveTradeOffs() {
        HoleModel model = strategicParFive();
        BallState tee = new BallState(model.geometry().tee(), Surface.TEE_BOX);
        List<ShotGuidance.StrategicOption> options = StrategicTargetPlanner.options(model, tee, model.startDistance(),
                Surface.TEE_BOX, PLAYER, Environment.calm());

        assertThat(options).extracting(ShotGuidance.StrategicOption::role)
                .containsExactly(LandingZoneRole.SAFE, LandingZoneRole.PRIMARY, LandingZoneRole.AGGRESSIVE);
        assertThat(options).extracting(ShotGuidance.StrategicOption::aimPoint).doesNotHaveDuplicates();
        ShotGuidance.StrategicOption safe = option(options, LandingZoneRole.SAFE);
        ShotGuidance.StrategicOption primary = option(options, LandingZoneRole.PRIMARY);
        ShotGuidance.StrategicOption aggressive = option(options, LandingZoneRole.AGGRESSIVE);
        assertThat(safe.routeSummary()).contains("Longer next shot");
        assertThat(aggressive.routeSummary()).contains("Shorter next shot");
        assertThat(safe.suggestedClub()).isEqualTo(ClubId.THREE_HYBRID);
        assertThat(primary.suggestedClub()).isEqualTo(ClubId.DRIVER);
        assertThat(aggressive.suggestedClub()).isEqualTo(ClubId.DRIVER);
        assertThat(aggressive.exposureSummary()).isEqualTo("Higher landing exposure");
        assertThat(aggressive.suggestedFamily()).isEqualTo(ShotFamily.FULL);
        assertThat(model.geometry().surfaceAt(position(aggressive.aimPoint()))).isNotIn(Surface.WATER,
                Surface.OUT_OF_BOUNDS, Surface.BUNKER, Surface.TREES, Surface.RECOVERY_AREA);
    }

    @Test
    void positionalParFourUsesItsLandingRouteAndMayHonestlyOfferOnlyPrimary() {
        HoleModel model = positionalParFour();
        BallState tee = new BallState(model.geometry().tee(), Surface.TEE_BOX);
        List<ShotGuidance.StrategicOption> options = StrategicTargetPlanner.options(model, tee, model.startDistance(),
                Surface.TEE_BOX, PLAYER, Environment.calm());

        assertThat(options).extracting(ShotGuidance.StrategicOption::role).containsExactly(LandingZoneRole.PRIMARY);
        ShotGuidance.StrategicOption primary = option(options, LandingZoneRole.PRIMARY);
        assertThat(primary.aimPoint().x()).isPositive();
        assertThat(position(primary.aimPoint()).distanceTo(model.geometry().greenCenter())).isGreaterThan(40.0);
    }

    @Test
    void aiChoosesTheSamePublishedCandidateAndLegacyModelsRemainUnchanged() {
        HoleModel model = strategicParFive();
        BallState tee = new BallState(model.geometry().tee(), Surface.TEE_BOX);
        List<ShotGuidance.StrategicOption> options = StrategicTargetPlanner.options(model, tee, model.startDistance(),
                Surface.TEE_BOX, PLAYER, Environment.calm());

        for (Strategy strategy : Strategy.values()) {
            BallStrikeIntent intent = (BallStrikeIntent) new StrategyPolicy(strategy).decideShotIntent(model, tee,
                    model.startDistance(), Surface.TEE_BOX, PLAYER, model.par(), Environment.calm());
            ShotGuidance.StrategicOption expected = StrategicTargetPlanner.preferred(options, strategy);
            assertThat(intent.aimPoint()).isEqualTo(expected.aimPoint());
            assertThat(intent.club()).isEqualTo(expected.suggestedClub());
        }

        HoleModel legacy = new FixtureHole(4, null, geometry(false));
        assertThat(StrategicTargetPlanner.options(legacy, new BallState(legacy.geometry().tee(), Surface.TEE_BOX),
                legacy.startDistance(), Surface.TEE_BOX, PLAYER, Environment.calm())).isEmpty();
    }

    @Test
    void generatedV4ModelsActivateTheirExistingPrimaryLandingZones() {
        var course = CourseGenerator.generate(new SeedCoordinate(0x5354524154454749L, 4, 7, 0, 0, 0, 0),
                EnvironmentClassification.COASTAL, CourseGenConstants.V4_GENERATOR_VERSION);
        course.holes().stream().filter(hole -> hole.par() >= 4).forEach(hole -> {
            HoleModel model = hole.forRound(1);
            List<ShotGuidance.StrategicOption> options = StrategicTargetPlanner.options(model,
                    new BallState(model.geometry().tee(), Surface.TEE_BOX), model.startDistance(), Surface.TEE_BOX,
                    PLAYER, Environment.calm());
            assertThat(model.strategicLandingPlan()).isNotNull();
            assertThat(options).extracting(ShotGuidance.StrategicOption::role).contains(LandingZoneRole.PRIMARY);
        });
    }

    @Test
    void planningDoesNotChangeTheSharedSeededResolverResult() {
        HoleModel model = strategicParFive();
        BallState tee = new BallState(model.geometry().tee(), Surface.TEE_BOX);
        BallStrikeIntent strike = (BallStrikeIntent) new StrategyPolicy(Strategy.BALANCED).decideShotIntent(model, tee,
                model.startDistance(), Surface.TEE_BOX, PLAYER, model.par(), Environment.calm());
        Position2d aim = position(strike.aimPoint());
        ShotContext context = new ShotContext(PLAYER, GolferState.fresh(), Environment.calm(), model.startDistance(),
                model.zoneProfileFor(model.startDistance()), ShotDecision.fromIntent(strike, tee.position().distanceTo(aim),
                Strategy.BALANCED), new SeedCoordinate(4, 4, 4, 4, 4, 4, 4), Surface.TEE_BOX, 0.0, tee,
                model.geometry(), model.cupPosition(), aim);

        ShotOutcome beforePlanning = ShotResolver.resolveShot(context);
        StrategicTargetPlanner.options(model, tee, model.startDistance(), Surface.TEE_BOX, PLAYER, Environment.calm());
        assertThat(ShotResolver.resolveShot(context)).isEqualTo(beforePlanning);
    }

    private static ShotGuidance.StrategicOption option(List<ShotGuidance.StrategicOption> options, LandingZoneRole role) {
        return options.stream().filter(option -> option.role() == role).findFirst().orElseThrow();
    }

    private static HoleModel strategicParFive() { return new FixtureHole(5, plan(true), geometry(true)); }
    private static HoleModel positionalParFour() { return new FixtureHole(4, plan(false), geometry(false)); }

    private static HoleSpatialPlan plan(boolean riskReward) {
        HoleRoute route = new HoleRoute(new Position2d(0, 0), List.of(), new Position2d(0, 470));
        List<LandingZone> zones = riskReward
                ? List.of(zone(LandingZoneRole.SAFE, 230), zone(LandingZoneRole.PRIMARY, 270), zone(LandingZoneRole.AGGRESSIVE, 315))
                : List.of(zone(LandingZoneRole.PRIMARY, 270, 18), zone(LandingZoneRole.SAFE, 230, -6));
        return new HoleSpatialPlan(route, zones, new GreenComplexPlan(new Position2d(0, 30), 18, 14, 0,
                new Position2d(0, 1), GreenSide.FRONT, GreenSide.RIGHT, GreenSide.LEFT, true,
                EnumSet.allOf(GreenSurroundRole.class)));
    }

    private static LandingZone zone(LandingZoneRole role, double distance) {
        return zone(role, distance, 0);
    }

    private static LandingZone zone(LandingZoneRole role, double distance, double offset) {
        return new LandingZone(role, distance, offset, 40, 24, PreferredApproachSide.NEUTRAL,
                role == LandingZoneRole.AGGRESSIVE ? ReferenceCarryBand.LONG_REACHABLE : ReferenceCarryBand.STANDARD);
    }

    private static CourseGeometry geometry(boolean aggressiveGuard) {
        List<Position2d> boundary = rectangle(-60, 0, 60, 540);
        List<TerrainRegion> regions = new java.util.ArrayList<>();
        regions.add(new TerrainRegion(Surface.FAIRWAY, boundary));
        regions.add(new TerrainRegion(Surface.GREEN, rectangle(-18, 482, 18, 518)));
        if (aggressiveGuard) {
            // PRIMARY is guarded enough that the wider SAFE landing is genuinely lower exposure; the
            // AGGRESSIVE guard remains the highest-exposure, shorter-route choice.
            regions.add(new TerrainRegion(Surface.BUNKER, rectangle(12, 262, 28, 278)));
            regions.add(new TerrainRegion(Surface.BUNKER, rectangle(12, 285, 50, 345)));
        }
        return new CourseGeometry(new Position2d(0, 0), new Position2d(0, 500), boundary, regions);
    }

    private static List<Position2d> rectangle(double minX, double minY, double maxX, double maxY) {
        return List.of(new Position2d(minX, minY), new Position2d(maxX, minY), new Position2d(maxX, maxY),
                new Position2d(minX, maxY));
    }

    private static Position2d position(AimPoint point) { return new Position2d(point.x(), point.y()); }

    private record FixtureHole(int par, HoleSpatialPlan strategicLandingPlan, CourseGeometry geometry) implements HoleModel {
        @Override public double startDistance() { return 500; }
        @Override public ShotZoneProfile zoneProfileFor(double remainingDistance) {
            return new ShotZoneProfile(List.of(new ZoneBand(0, 600, List.of(new LateralRegion(60, Surface.FAIRWAY)))));
        }
        @Override public Position2d cupPosition() { return new Position2d(0, 500); }
    }
}
