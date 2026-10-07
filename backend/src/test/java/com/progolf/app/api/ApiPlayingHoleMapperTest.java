package com.progolf.app.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.app.api.dto.PlayingHoleDto;
import com.progolf.app.api.dto.ShotSituationDto;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.CourseSetup;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.course.GeneratedHole;
import com.progolf.sim.course.PinPosition;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.play.ShotSituation;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.FactorBreakdown;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.RecoveryKind;
import com.progolf.sim.shot.ShotContact;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.ShotSettlement;
import com.progolf.sim.shot.ShotTrace;
import com.progolf.sim.shot.ShotTraceTransition;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import org.junit.jupiter.api.Test;

/**
 * graphql-api / web-hole-visualization: {@link ApiMapper} projects a hole's geometry and the active round's pin
 * to {@link PlayingHoleDto}, and the shot situation's reachable surface profile to bands — faithfully mirroring
 * the engine records the resolver uses, with no engine type on the schema (guarded separately by ApiBoundaryTest).
 */
class ApiPlayingHoleMapperTest {

    private static final EnvironmentClassification CLASSIFICATION = EnvironmentClassification.PARKLAND;

    private static Course course(long seed) {
        return CourseGenerator.generate(new SeedCoordinate(seed, 0, 0, 0, 0, 0, 0), CLASSIFICATION);
    }

    @Test
    void playingHoleCarriesGeometryPinAndStableSeed() {
        GeneratedHole hole = course(1234L).holes().get(0);
        PinPosition pin = hole.pinFor(1);

        PlayingHoleDto dto = ApiMapper.playingHole(hole, pin, CLASSIFICATION.name());

        assertThat(dto.holeNumber()).isEqualTo(hole.number());
        assertThat(dto.par()).isEqualTo(hole.par());
        assertThat(dto.length()).isEqualTo(hole.length());
        assertThat(dto.fairwayHalfWidth()).isEqualTo(hole.fairwayHalfWidth());
        assertThat(dto.greenHalfWidth()).isEqualTo(hole.greenHalfWidth());
        assertThat(dto.greenDepth()).isEqualTo(hole.greenDepth());
        assertThat(dto.elevationDelta()).isEqualTo(hole.elevationDelta());
        assertThat(dto.hasGreensideBunker()).isEqualTo(hole.hasGreensideBunker());
        assertThat(dto.hasWater()).isEqualTo(hole.hasWater());
        assertThat(dto.hasTrees()).isEqualTo(hole.hasTrees());
        assertThat(dto.pinLateral()).isEqualTo(pin.lateralOffset());
        assertThat(dto.pinDepth()).isEqualTo(pin.depthOffset());
        assertThat(dto.courseType()).isEqualTo(CLASSIFICATION.name());
        assertThat(dto.layoutSeed()).isEqualTo(Long.toString(hole.holeSeed()));
        assertThat(dto.geometry().tee().x()).isEqualTo(hole.geometry().tee().x());
        assertThat(dto.geometry().tee().y()).isEqualTo(hole.geometry().tee().y());
        assertThat(dto.geometry().cup().x()).isEqualTo(hole.cupFor(pin).x());
        assertThat(dto.geometry().cup().y()).isEqualTo(hole.cupFor(pin).y());
        assertThat(dto.geometry().playableBoundary()).hasSize(hole.geometry().playableBoundary().size());
        assertThat(dto.geometry().regions()).hasSize(hole.geometry().regions().size());
        for (int i = 0; i < hole.geometry().regions().size(); i++) {
            var engine = hole.geometry().regions().get(i);
            var mapped = dto.geometry().regions().get(i);
            assertThat(mapped.surface()).isEqualTo(engine.surface().name());
            assertThat(mapped.boundary()).extracting(p -> p.x(), p -> p.y())
                    .containsExactlyElementsOf(engine.boundary().stream().map(p -> org.assertj.core.groups.Tuple.tuple(p.x(), p.y())).toList());
        }
        assertThat(dto.ball().position().x()).isEqualTo(hole.geometry().tee().x());
        assertThat(dto.ball().lie()).isEqualTo(Surface.TEE_BOX.name());
    }

    @Test
    void pinReflectsTheActiveRoundWhileGeometryIsConstant() {
        GeneratedHole hole = course(1234L).holes().get(3);

        PlayingHoleDto round1 = ApiMapper.playingHole(hole, hole.pinFor(1), CLASSIFICATION.name());
        PlayingHoleDto round2 = ApiMapper.playingHole(hole, hole.pinFor(2), CLASSIFICATION.name());

        // Dimensions and hazard flags are identical across rounds...
        assertThat(round2.length()).isEqualTo(round1.length());
        assertThat(round2.greenHalfWidth()).isEqualTo(round1.greenHalfWidth());
        assertThat(round2.greenDepth()).isEqualTo(round1.greenDepth());
        assertThat(round2.hasWater()).isEqualTo(round1.hasWater());
        // ...while each DTO carries its own round's pin.
        assertThat(round1.pinLateral()).isEqualTo(hole.pinFor(1).lateralOffset());
        assertThat(round2.pinLateral()).isEqualTo(hole.pinFor(2).lateralOffset());
    }

    @Test
    void playingHoleProjectsTheSetupSpecificGeometryUsedByTheHoleModel() {
        GeneratedHole hole = course(4321L).holes().get(0);
        CourseSetup tight = new CourseSetup(1.0, 1.0, 0.60);
        HoleModel model = hole.forRound(1, tight);
        PinPosition pin = hole.pinFor(1, tight);

        PlayingHoleDto dto = ApiMapper.playingHole(hole, pin, CLASSIFICATION.name(),
                new BallState(model.geometry().tee(), Surface.TEE_BOX), model.geometry());

        assertGeometry(dto, model.geometry(), pin);
        assertThat(model.geometry()).isNotEqualTo(hole.geometry());
    }

    @Test
    void layoutSeedIsStableAcrossSessions() {
        // Same world seed -> same generated course -> same hole seed -> same layout seed.
        GeneratedHole first = course(777L).holes().get(5);
        GeneratedHole second = course(777L).holes().get(5);

        assertThat(ApiMapper.playingHole(second, second.pinFor(1), CLASSIFICATION.name()).layoutSeed())
                .isEqualTo(ApiMapper.playingHole(first, first.pinFor(1), CLASSIFICATION.name()).layoutSeed());
    }

    @Test
    void reachableSurfacesMirrorTheResolverProfile() {
        GeneratedHole hole = course(2468L).holes().get(0);
        HoleModel model = hole.forRound(1);
        double remaining = 150.0;
        ShotZoneProfile profile = model.zoneProfileFor(remaining);
        ShotSituation situation = new ShotSituation(hole.number(), hole.par(), 1, 0, remaining,
                Surface.FAIRWAY, model.pinLateral(), profile);

        ShotSituationDto dto = ApiMapper.situation(situation);

        assertThat(dto.reachable()).hasSameSizeAs(profile.bands());
        assertThat(dto.reachable().get(0).startDistance()).isEqualTo(profile.minReach());
        assertThat(dto.reachable().get(dto.reachable().size() - 1).endDistance()).isEqualTo(profile.maxReach());
        for (int i = 0; i < profile.bands().size(); i++) {
            var band = profile.bands().get(i);
            var bandDto = dto.reachable().get(i);
            assertThat(bandDto.startDistance()).isEqualTo(band.startDistance());
            assertThat(bandDto.endDistance()).isEqualTo(band.endDistance());
            assertThat(bandDto.regions()).hasSameSizeAs(band.regions());
            for (int j = 0; j < band.regions().size(); j++) {
                assertThat(bandDto.regions().get(j).surface()).isEqualTo(band.regions().get(j).surface().name());
                assertThat(bandDto.regions().get(j).halfWidth()).isEqualTo(band.regions().get(j).outerHalfWidth());
            }
        }
    }

    @Test
    void settlementProjectionPreservesContactRecoveryAndPlayableBall() {
        Position2d contactPosition = new Position2d(42, 180);
        Position2d recoveryPosition = new Position2d(35, 167);
        BallState ball = new BallState(recoveryPosition, Surface.PRIMARY_ROUGH);
        ShotSettlement settlement = new ShotSettlement(new ShotContact(contactPosition, Surface.WATER), recoveryPosition,
                RecoveryKind.WATER_DROP, ball);
        ShotTrace trace = new ShotTrace(com.progolf.sim.shot.ClubId.SEVEN_IRON, new Position2d(0, 0),
                new com.progolf.sim.shot.AimPoint(40, 180), settlement.contact(),
                new ShotTraceTransition(RecoveryKind.WATER_DROP, contactPosition, recoveryPosition), recoveryPosition);
        ShotOutcome outcome = new ShotOutcome(Surface.WATER, 185, 4, 120, true, 1, 2,
                new FactorBreakdown(0, 0, 0, 0), settlement, false, trace);

        var dto = ApiMapper.shotOutcome(outcome);
        assertThat(dto.settlement().contact().position().x()).isEqualTo(contactPosition.x());
        assertThat(dto.settlement().contact().surface()).isEqualTo(Surface.WATER.name());
        assertThat(dto.settlement().recoveryPosition().y()).isEqualTo(recoveryPosition.y());
        assertThat(dto.settlement().recoveryKind()).isEqualTo(RecoveryKind.WATER_DROP.name());
        assertThat(dto.settlement().ball().position().x()).isEqualTo(recoveryPosition.x());
        assertThat(dto.settlement().ball().lie()).isEqualTo(Surface.PRIMARY_ROUGH.name());
        assertThat(dto.trace().club()).isEqualTo("SEVEN_IRON");
        assertThat(dto.trace().aimPoint().x()).isEqualTo(40);
        assertThat(dto.trace().contact().position().y()).isEqualTo(contactPosition.y());
        assertThat(dto.trace().transition().kind()).isEqualTo(RecoveryKind.WATER_DROP.name());
        assertThat(dto.trace().finalPoint()).isEqualTo(dto.settlement().ball().position());

        ShotContact cupContact = new ShotContact(new Position2d(0, 200), Surface.GREEN);
        BallState holedBall = new BallState(cupContact.position(), Surface.GREEN);
        ShotSettlement holed = new ShotSettlement(cupContact, null, RecoveryKind.NONE, holedBall);
        ShotTrace holeOutTrace = new ShotTrace(com.progolf.sim.shot.ClubId.SEVEN_IRON,
                new Position2d(0, 0), new com.progolf.sim.shot.AimPoint(0, 200), cupContact,
                null, cupContact.position());
        ShotOutcome holeOut = new ShotOutcome(Surface.GREEN, 200, 0, 0, false, 0, 1,
                new FactorBreakdown(0, 0, 0, 0), holed, false, holeOutTrace);
        assertThat(ApiMapper.shotOutcome(holeOut).trace().finalPoint().y()).isEqualTo(200);

        ShotOutcome summaryOnly = new ShotOutcome(Surface.PRIMARY_ROUGH, 120, 4, 80, false, 0, 1,
                new FactorBreakdown(0, 0, 0, 0), settlement, false);
        assertThat(ApiMapper.shotOutcome(summaryOnly).trace()).isNull();
    }

    private static void assertGeometry(PlayingHoleDto dto, CourseGeometry geometry, PinPosition pin) {
        assertThat(dto.geometry().tee().x()).isEqualTo(geometry.tee().x());
        assertThat(dto.geometry().tee().y()).isEqualTo(geometry.tee().y());
        assertThat(dto.geometry().cup().x()).isEqualTo(geometry.greenCenter().x() + pin.lateralOffset());
        assertThat(dto.geometry().cup().y()).isEqualTo(geometry.greenCenter().y() + pin.depthOffset());
        assertThat(dto.geometry().playableBoundary()).extracting(p -> p.x(), p -> p.y())
                .containsExactlyElementsOf(geometry.playableBoundary().stream()
                        .map(p -> org.assertj.core.groups.Tuple.tuple(p.x(), p.y())).toList());
        assertThat(dto.geometry().regions()).hasSameSizeAs(geometry.regions());
        for (int i = 0; i < geometry.regions().size(); i++) {
            var engine = geometry.regions().get(i);
            var mapped = dto.geometry().regions().get(i);
            assertThat(mapped.surface()).isEqualTo(engine.surface().name());
            assertThat(mapped.boundary()).extracting(p -> p.x(), p -> p.y())
                    .containsExactlyElementsOf(engine.boundary().stream()
                            .map(p -> org.assertj.core.groups.Tuple.tuple(p.x(), p.y())).toList());
        }
    }
}
