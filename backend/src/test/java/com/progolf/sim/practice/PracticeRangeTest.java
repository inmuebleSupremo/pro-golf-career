package com.progolf.sim.practice;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Handedness;
import com.progolf.sim.shot.AimPoint;
import com.progolf.sim.shot.BallStrikeIntent;
import com.progolf.sim.shot.ClubId;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.ShotFamily;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.spatial.Surface;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PracticeRangeTest {
    @Test
    void widenedFieldAndAimEnvelopeAgreeWithTheSevenTargets() {
        assertThat(PracticeRange.fieldHalfWidth()).isGreaterThanOrEqualTo(2 * 100);
        assertThat(PracticeRange.aimHalfWidth()).isLessThan(PracticeRange.fieldHalfWidth());
        assertThat(PracticeRange.geometryView().surfaceAt(new Position2d(235, 160))).isEqualTo(Surface.FAIRWAY);
        assertThat(PracticeRange.geometryView().surfaceAt(new Position2d(270, 160))).isEqualTo(Surface.OUT_OF_BOUNDS);
        assertThat(PracticeRange.targets()).allSatisfy(target -> {
            double yards = Double.parseDouble(target.label().replace(" yd", ""));
            assertThat(PracticeRange.bay().distanceTo(target.position())).isCloseTo(yards,
                    org.assertj.core.data.Offset.offset(1.0));
            assertThat(PracticeRange.geometryView().surfaceAt(target.position())).isEqualTo(Surface.GREEN);
        });

        PracticeRange range = new PracticeRange(Attributes.uniform(70), GolferState.fresh(), Handedness.RIGHT, 42);
        range.hit(new BallStrikeIntent(ClubId.DRIVER, new AimPoint(210, 160)), "0");
        assertThatThrownBy(() -> range.hit(new BallStrikeIntent(ClubId.DRIVER, new AimPoint(221, 160)), "1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(range.ballsHit()).isEqualTo(1);
    }

    @Test
    void resolvesRepeatableBallsFromTheSameBayAndReportsTargetDistance() {
        PracticeRange range = new PracticeRange(Attributes.uniform(70), GolferState.fresh(), Handedness.RIGHT, 42);
        assertThat(PracticeRange.targets()).hasSize(7);
        assertThat(range.clubs().stream().filter(club -> club.club() == ClubId.SAND_WEDGE).findFirst().orElseThrow()
                .families()).anyMatch(family -> family.family() == ShotFamily.PITCH && family.available());
        assertThat(range.clubs().stream().filter(club -> club.club() == ClubId.DRIVER).findFirst().orElseThrow()
                .families()).noneMatch(family -> family.family() == ShotFamily.PITCH && family.available());
        var target = PracticeRange.targets().get(3).position();
        var intent = new BallStrikeIntent(ClubId.SEVEN_IRON, new AimPoint(target.x(), target.y()));

        var first = range.hit(intent, "0");
        assertThat(first.outcome().trace()).isNotNull();
        assertThat(first.outcome().trace().origin()).isEqualTo(PracticeRange.bay());
        assertThat(first.outcome().trace().airbornePath()).isNotEmpty();
        assertThat(first.totalDistance()).isEqualTo(PracticeRange.bay().distanceTo(first.outcome().trace().finalPoint()));
        assertThat(first.proximity()).isEqualTo(target.distanceTo(first.outcome().trace().finalPoint()));
        assertThat(range.ballsHit()).isEqualTo(1);

        assertThatThrownBy(() -> range.hit(intent, "0")).isInstanceOf(IllegalArgumentException.class);
        assertThat(range.ballsHit()).isEqualTo(1);
        var second = range.hit(intent, "1");
        assertThat(second.ballNumber()).isEqualTo(2);
        assertThat(second.outcome().trace().origin()).isEqualTo(PracticeRange.bay());
    }

    @Test
    void selectedBayChangesTheAuthoritativeOriginWithoutMovingFlags() {
        PracticeRange range = new PracticeRange(Attributes.uniform(70), GolferState.fresh(), Handedness.RIGHT, 42);
        assertThat(PracticeRange.bays()).hasSize(7);
        assertThat(PracticeRange.bays()).allSatisfy(bay ->
                assertThat(PracticeRange.geometryView().surfaceAt(bay)).isEqualTo(Surface.TEE_BOX));
        var target = PracticeRange.targets().get(3).position();
        var intent = new BallStrikeIntent(ClubId.SEVEN_IRON, new AimPoint(target.x(), target.y()));

        var left = range.hit(intent, 1, "0");
        var right = range.hit(intent, 7, "1");
        assertThat(left.outcome().trace().origin()).isEqualTo(PracticeRange.bays().getFirst());
        assertThat(right.outcome().trace().origin()).isEqualTo(PracticeRange.bays().getLast());
        assertThat(left.outcome().trace().intendedAimPoint()).isEqualTo(intent.aimPoint());
        assertThat(right.outcome().trace().intendedAimPoint()).isEqualTo(intent.aimPoint());
        assertThat(left.totalDistance()).isEqualTo(PracticeRange.bays().getFirst()
                .distanceTo(left.outcome().trace().finalPoint()));
        assertThat(right.totalDistance()).isEqualTo(PracticeRange.bays().getLast()
                .distanceTo(right.outcome().trace().finalPoint()));
        assertThat(left.proximity()).isEqualTo(target.distanceTo(left.outcome().trace().finalPoint()));
        assertThat(right.proximity()).isEqualTo(target.distanceTo(right.outcome().trace().finalPoint()));
        assertThatThrownBy(() -> range.hit(intent, 8, "2")).isInstanceOf(IllegalArgumentException.class);
    }
}
