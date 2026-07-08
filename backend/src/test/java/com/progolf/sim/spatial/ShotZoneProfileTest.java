package com.progolf.sim.spatial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Hole-spatial-model spec: partition validity, surface-solely-from-bands, across-line selection. */
class ShotZoneProfileTest {

    private static ZoneBand fairwayBand(double start, double end) {
        return new ZoneBand(start, end, List.of(
                new LateralRegion(15.0, Surface.FAIRWAY),
                new LateralRegion(30.0, Surface.PRIMARY_ROUGH)));
    }

    @Test
    void contiguousBandsFormValidProfile() {
        ShotZoneProfile profile = new ShotZoneProfile(List.of(fairwayBand(0, 150), fairwayBand(150, 320)));
        assertThat(profile.minReach()).isEqualTo(0.0);
        assertThat(profile.maxReach()).isEqualTo(320.0);
    }

    @Test
    void gapBetweenBandsIsRejected() {
        assertThatThrownBy(() -> new ShotZoneProfile(List.of(fairwayBand(0, 150), fairwayBand(160, 320))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lateralRegionsMustStrictlyIncrease() {
        assertThatThrownBy(() -> new ZoneBand(0, 100, List.of(
                new LateralRegion(20.0, Surface.FAIRWAY),
                new LateralRegion(20.0, Surface.PRIMARY_ROUGH))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void surfaceIsReadSolelyFromMatchingBandAndLateralOffset() {
        ShotZoneProfile profile = new ShotZoneProfile(List.of(fairwayBand(0, 300)));
        assertThat(profile.surfaceAt(250, 0)).isEqualTo(Surface.FAIRWAY);
        assertThat(profile.surfaceAt(250, 15)).isEqualTo(Surface.FAIRWAY);
        assertThat(profile.surfaceAt(250, 25)).isEqualTo(Surface.PRIMARY_ROUGH);
        // Beyond the widest lateral region -> OUT_OF_BOUNDS.
        assertThat(profile.surfaceAt(250, 40)).isEqualTo(Surface.OUT_OF_BOUNDS);
        // Sign of lateral does not matter (symmetric).
        assertThat(profile.surfaceAt(250, -25)).isEqualTo(Surface.PRIMARY_ROUGH);
    }

    @Test
    void overshootBeyondMaxReachIsOutOfBounds() {
        ShotZoneProfile profile = new ShotZoneProfile(List.of(fairwayBand(0, 300)));
        assertThat(profile.surfaceAt(350, 0)).isEqualTo(Surface.OUT_OF_BOUNDS);
    }
}
