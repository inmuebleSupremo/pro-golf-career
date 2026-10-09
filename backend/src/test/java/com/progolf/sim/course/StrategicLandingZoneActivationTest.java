package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.ShotGuidance;
import com.progolf.sim.shot.StrategicTargetPlanner;
import com.progolf.sim.spatial.Surface;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Profile-specific V4 corpus proof that activation consumes real generated plans and canonical guards. */
class StrategicLandingZoneActivationTest {
    private static final Attributes PLAYER = Attributes.uniform(90);
    private static final CourseDesignProfile RISK_REWARD = new CourseDesignProfile(StrategicEmphasis.RISK_REWARD,
            WidthTendency.BALANCED, RecoverySeverity.BALANCED);

    @Test
    void generatedRiskRewardParFivePublishesSafePrimaryAndAggressiveOptionsForReachableElitePlayer() {
        GeneratedHole hole = IntStream.range(0, 500).mapToObj(this::course)
                .flatMap(course -> course.holes().stream()).filter(candidate -> candidate.par() == 5)
                .filter(candidate -> candidate.spatialPlan().landingZones().stream()
                        .anyMatch(zone -> zone.role() == LandingZoneRole.AGGRESSIVE))
                .filter(candidate -> roles(candidate).equals(List.of(LandingZoneRole.SAFE,
                        LandingZoneRole.PRIMARY, LandingZoneRole.AGGRESSIVE))).findFirst()
                .orElseThrow();

        assertThat(hole.hazardPlan().features()).anyMatch(feature -> feature.role() == HazardRole.LANDING_GUARD
                && feature.anchor().landingZoneRole() == LandingZoneRole.AGGRESSIVE);
    }

    private Course course(int id) {
        return CourseGenerator.generateV4(new SeedCoordinate(0x5354524154454749L, 4, id, 0, 0, 0, 0),
                EnvironmentClassification.COASTAL, RISK_REWARD);
    }

    private List<LandingZoneRole> roles(GeneratedHole hole) {
        HoleModel model = hole.forRound(1);
        return StrategicTargetPlanner.options(model, new BallState(model.geometry().tee(), Surface.TEE_BOX),
                model.startDistance(), Surface.TEE_BOX, PLAYER, Environment.calm()).stream()
                .map(ShotGuidance.StrategicOption::role).toList();
    }
}
