package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.course.TerrainRegion;
import com.progolf.sim.spatial.LateralRegion;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.spatial.ZoneBand;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Focused mechanical acceptance tests for the first surface-aware technique slice. */
class ShotFamilyResolutionTest {
    private static final Position2d TEE = new Position2d(0, 0);
    private static final Position2d CUP = new Position2d(0, 120);

    @Test
    void eligibilitySeparatesInvalidTechniqueFromDifficultButLegalLie() {
        assertThat(ShotFamilyEligibility.evaluate(Surface.BUNKER, ClubSpec.of(ClubId.SAND_WEDGE), ShotFamily.BUNKER).allowed())
                .isTrue();
        assertThat(ShotFamilyEligibility.evaluate(Surface.BUNKER, ClubSpec.of(ClubId.GAP_WEDGE), ShotFamily.FULL).allowed())
                .isFalse();
        assertThat(ShotFamilyEligibility.evaluate(Surface.FAIRWAY, ClubSpec.of(ClubId.GAP_WEDGE), ShotFamily.BUNKER).allowed())
                .isFalse();
        assertThat(ShotFamilyEligibility.evaluate(Surface.DEEP_ROUGH, ClubSpec.of(ClubId.SIX_IRON), ShotFamily.FULL).allowed())
                .isTrue();
        assertThat(ShotFamilyEligibility.evaluate(Surface.TREES, ClubSpec.of(ClubId.DRIVER), ShotFamily.FULL).allowed())
                .isFalse();
    }

    @Test
    void controlledTradesReachForLowerVarianceAndPitchChipHaveDifferentRelease() {
        Attributes attributes = Attributes.uniform(60);
        ShotExecutionProfile full = ShotExecutionProfile.derive(Surface.FAIRWAY, ClubSpec.of(ClubId.SEVEN_IRON),
                ShotFamily.FULL, attributes, Environment.calm());
        ShotExecutionProfile controlled = ShotExecutionProfile.derive(Surface.FAIRWAY, ClubSpec.of(ClubId.SEVEN_IRON),
                ShotFamily.CONTROLLED, attributes, Environment.calm());
        ShotExecutionProfile pitch = ShotExecutionProfile.derive(Surface.FAIRWAY, ClubSpec.of(ClubId.GAP_WEDGE),
                ShotFamily.PITCH, attributes, Environment.calm());
        ShotExecutionProfile chip = ShotExecutionProfile.derive(Surface.FAIRWAY, ClubSpec.of(ClubId.GAP_WEDGE),
                ShotFamily.CHIP, attributes, Environment.calm());

        assertThat(controlled.effectiveCarryCapMultiplier()).isLessThan(full.effectiveCarryCapMultiplier());
        assertThat(controlled.lateralDispersionMultiplier()).isLessThan(full.lateralDispersionMultiplier());
        assertThat(chip.rollYardsOn(Surface.FAIRWAY)).isGreaterThan(pitch.rollYardsOn(Surface.FAIRWAY));
    }

    @Test
    void chipTraceContainsBoundedAuthoritativeRollAndPitchReleasesLess() {
        ShotOutcome pitch = resolve(ShotFamily.PITCH);
        ShotOutcome chip = resolve(ShotFamily.CHIP);

        assertThat(pitch.trace().roll()).isNotNull();
        assertThat(chip.trace().roll()).isNotNull();
        assertThat(chip.trace().roll().to().distanceTo(chip.trace().roll().from()))
                .isGreaterThan(pitch.trace().roll().to().distanceTo(pitch.trace().roll().from()));
        assertThat(chip.finalSurface()).isEqualTo(Surface.FAIRWAY);
        assertThat(chip.settlement().contact().surface()).isEqualTo(Surface.FAIRWAY);
        assertThat(chip.trace().transition()).isNull();
    }

    @Test
    void bunkerProfileHasNoGroundResponse() {
        ShotExecutionProfile bunker = ShotExecutionProfile.derive(Surface.BUNKER, ClubSpec.of(ClubId.SAND_WEDGE),
                ShotFamily.BUNKER, Attributes.uniform(60), Environment.calm());
        assertThat(bunker.rollYardsOn(Surface.BUNKER)).isZero();
        assertThat(bunker.lateralDispersionMultiplier()).isGreaterThan(1.0);
    }

    @Test
    void fullControlledAndBunkerUseTheirApprovedGroundResponseProfiles() {
        Attributes attributes = Attributes.uniform(60);
        ShotExecutionProfile full = ShotExecutionProfile.derive(Surface.FAIRWAY, ClubSpec.of(ClubId.SEVEN_IRON),
                ShotFamily.FULL, attributes, Environment.calm());
        ShotExecutionProfile controlled = ShotExecutionProfile.derive(Surface.FAIRWAY, ClubSpec.of(ClubId.SEVEN_IRON),
                ShotFamily.CONTROLLED, attributes, Environment.calm());
        ShotExecutionProfile bunker = ShotExecutionProfile.derive(Surface.BUNKER, ClubSpec.of(ClubId.SAND_WEDGE),
                ShotFamily.BUNKER, attributes, Environment.calm());

        assertThat(full.rollYardsOn(Surface.FAIRWAY)).isGreaterThan(controlled.rollYardsOn(Surface.FAIRWAY));
        assertThat(controlled.rollYardsOn(Surface.FAIRWAY)).isPositive();
        assertThat(bunker.rollYardsOn(Surface.BUNKER)).isZero();
        assertThat(resolve(ShotFamily.FULL).trace().roll()).isNotNull();
        assertThat(resolve(ShotFamily.CONTROLLED).trace().roll()).isNotNull();
    }

    @Test
    void aiKeepsTheEstablishedCloseFringePuttPathAndUsesFamilyBearingStrikesElsewhere() {
        CourseGeometry geometry = new CourseGeometry(TEE, CUP, square(-150, -10, 150, 200),
                List.of(new TerrainRegion(Surface.FAIRWAY, square(-150, -10, 150, 200))));
        HoleModel hole = new HoleModel() {
            @Override public double startDistance() { return 120; }
            @Override public ShotZoneProfile zoneProfileFor(double remainingDistance) {
                return new ShotZoneProfile(List.of(new ZoneBand(0, 500, List.of(new LateralRegion(500, Surface.FAIRWAY)))));
            }
            @Override public CourseGeometry geometry() { return geometry; }
            @Override public Position2d cupPosition() { return CUP; }
        };
        StrategyPolicy policy = new StrategyPolicy(Strategy.BALANCED);

        assertThat(policy.decideShotIntent(hole, new BallState(new Position2d(0, 7), Surface.FRINGE), 7,
                Surface.FRINGE, Attributes.uniform(60), 4)).isInstanceOf(PuttIntent.class);
        assertThat(policy.decideShotIntent(hole, new BallState(new Position2d(0, 25), Surface.FAIRWAY), 25,
                Surface.FAIRWAY, Attributes.uniform(60), 4)).isInstanceOf(BallStrikeIntent.class);

        BallStrikeIntent bunker = (BallStrikeIntent) policy.decideShotIntent(hole,
                new BallState(new Position2d(0, 200), Surface.BUNKER), 180, Surface.BUNKER,
                Attributes.uniform(60), 4);
        assertThat(bunker.shotFamily()).isEqualTo(ShotFamily.BUNKER);
        assertThat(new Position2d(bunker.aimPoint().x(), bunker.aimPoint().y()).distanceTo(new Position2d(0, 200)))
                .isLessThanOrEqualTo(ClubSpec.of(ClubId.SAND_WEDGE).baseCarry()
                        * (SimConstants.REACH_FLOOR + SimConstants.REACH_SPAN * Attributes.uniform(60)
                        .norm(ClubSpec.of(ClubId.SAND_WEDGE).distanceAttribute())));
    }

    private static ShotOutcome resolve(ShotFamily family) {
        CourseGeometry geometry = new CourseGeometry(TEE, CUP, square(-150, -10, 150, 200),
                List.of(new TerrainRegion(Surface.FAIRWAY, square(-150, -10, 150, 200))));
        ClubSpec club = ClubSpec.of(ClubId.GAP_WEDGE);
        ShotDecision decision = new ShotDecision(club.family(), 25, 0, Strategy.BALANCED, club, family);
        ShotZoneProfile zones = new ShotZoneProfile(List.of(new ZoneBand(0, 500, List.of(new LateralRegion(500, Surface.FAIRWAY)))));
        return ShotResolver.resolveShotWithTrace(new ShotContext(Attributes.uniform(60), GolferState.fresh(),
                Environment.calm(), 120, zones, decision, new SeedCoordinate(9, 1, 1, 1, 1, 1, 0), Surface.FAIRWAY,
                0, new BallState(TEE, Surface.FAIRWAY), geometry, CUP, new Position2d(0, 25)));
    }

    private static List<Position2d> square(double minX, double minY, double maxX, double maxY) {
        return List.of(new Position2d(minX, minY), new Position2d(maxX, minY),
                new Position2d(maxX, maxY), new Position2d(minX, maxY));
    }
}
