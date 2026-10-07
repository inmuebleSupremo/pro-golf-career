package com.progolf.sim.play;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.SimConstants;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.LateralRegion;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.spatial.ZoneBand;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * shot-resolution penalty-hazard recovery: water is recovered by a drop near the hazard (played forward
 * from a rough lie), while out-of-bounds keeps stroke-and-distance. The rule is applied identically by the
 * automatic {@link RoundResolver} and the interactive {@link PlayableHole}/{@link PlayableRound}.
 */
class WaterDropTest {

    private static final long WORLD = 0xF1005DL;
    private static final SeedCoordinate COORD = new SeedCoordinate(WORLD, 1, 3, 5, 7, 1, 0);
    private static final double TEE_DISTANCE = 200.0;

    /** A hazard is fatal off the tee (the whole landing line is the hazard) but a short recovery finds the green. */
    private static HoleModel hazardHole(Surface teeHazard) {
        return new HoleModel() {
            @Override
            public double startDistance() {
                return TEE_DISTANCE;
            }

            @Override
            public int par() {
                return 3;
            }

            @Override
            public ShotZoneProfile zoneProfileFor(double remainingDistance) {
                Surface fill = remainingDistance > 100.0 ? teeHazard : Surface.GREEN;
                ZoneBand band = new ZoneBand(0.0, 600.0, List.of(new LateralRegion(200.0, fill)));
                return new ShotZoneProfile(List.of(band));
            }
        };
    }

    private static PlayableHole playable(HoleModel model) {
        return new PlayableHole(1, model.par(), Attributes.uniform(60), GolferState.fresh(), model,
                Environment.calm(), COORD, Strategy.BALANCED);
    }

    @Test
    void waterIsDroppedNearerTheHoleFromARoughLie() {
        PlayableHole hole = playable(hazardHole(Surface.WATER));

        assertThat(hole.situation().distanceToPin()).isEqualTo(TEE_DISTANCE);
        assertThat(hole.simShot().finalSurface()).isEqualTo(Surface.PRIMARY_ROUGH);

        // The drop advances the ball toward the hole (not a replay from the tee) and lands in rough.
        ShotSituation afterDrop = hole.situation();
        assertThat(afterDrop.distanceToPin()).isLessThan(TEE_DISTANCE);
        assertThat(afterDrop.lie()).isEqualTo(Surface.PRIMARY_ROUGH);
    }

    @Test
    void outOfBoundsReplaysFromThePreviousSpot() {
        PlayableHole hole = playable(hazardHole(Surface.OUT_OF_BOUNDS));

        assertThat(hole.simShot().finalSurface()).isEqualTo(Surface.TEE_BOX);

        // Stroke-and-distance: the next shot is played from the same spot, losing the distance.
        ShotSituation afterOob = hole.situation();
        assertThat(afterOob.distanceToPin()).isEqualTo(TEE_DISTANCE);
        assertThat(afterOob.lie()).isEqualTo(Surface.TEE_BOX);
    }

    @Test
    void theDropNeverExceedsTheEntryPlusSetback() {
        PlayableHole hole = playable(hazardHole(Surface.WATER));
        double entryRemaining = hole.simShot().distanceRemaining();
        // remaining == min(preShot, entry + setback); the ball clearly advanced, so it is the drop term.
        assertThat(hole.situation().distanceToPin())
                .isEqualTo(entryRemaining + SimConstants.WATER_DROP_SETBACK);
    }

    @Test
    void aSimmedWaterHoleMatchesAutomaticResolution() {
        HoleModel model = hazardHole(Surface.WATER);

        int automatic = RoundResolver.resolveHole(model, Attributes.uniform(60), GolferState.fresh(),
                Environment.calm(), Strategy.BALANCED, COORD).totalStrokes();

        PlayableHole hole = playable(model);
        hole.simHole();

        assertThat(hole.isComplete()).isTrue();
        assertThat(hole.strokes()).isEqualTo(automatic);
    }
}
