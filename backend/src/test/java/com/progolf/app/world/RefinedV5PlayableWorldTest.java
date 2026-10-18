package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.sim.course.CourseGenConstants;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The fresh-world opt-in reaches normal play and survives a save/load without changing default worlds. */
class RefinedV5PlayableWorldTest {
    private static final String OWNER = "refined-v5-test";
    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 6);

    @TempDir Path saves;

    @Test
    void optedInCareerPlaysRefinedCanonicalGeometryAndRestoresItsPinnedVersion() {
        var store = new FilesystemSaveGameStore(saves.toString());
        WorldService refined = new WorldService(store, true);
        WorldSession created = refined.create(OWNER, 4242L, SMALL);
        assertThat(refined.status(OWNER, created.id()).courseGeneratorVersion())
                .isEqualTo(CourseGenConstants.V5_REFINED_GENERATOR_VERSION);

        refined.createPlayer(OWNER, created.id(), "Test", "Golfer", Nationality.GBR, 19, Archetype.POWER_HITTER);
        refined.save(OWNER, created.id(), "refined-career");

        // Loading does not consult the current opt-in; the saved version remains authoritative.
        WorldService normal = new WorldService(store, false);
        WorldSession loaded = normal.load(OWNER, "refined-career");
        assertThat(normal.status(OWNER, loaded.id()).courseGeneratorVersion())
                .isEqualTo(CourseGenConstants.V5_REFINED_GENERATOR_VERSION);

        int weeks = 0;
        while (!normal.hasPendingEvent(OWNER, loaded.id()) && weeks++ < 25) {
            normal.advanceWeek(OWNER, loaded.id());
        }
        assertThat(normal.hasPendingEvent(OWNER, loaded.id())).isTrue();
        var hole = normal.currentPlayingHole(OWNER, loaded.id(), null);
        assertThat(hole.geometry().playableBoundary()).hasSizeGreaterThan(10);
        assertThat(hole.geometry().regions()).isNotEmpty();
        assertThat(normal.currentSituation(OWNER, loaded.id())).isNotNull();
        var shot = normal.simShot(OWNER, loaded.id());
        assertThat(shot.trace()).isNotNull();
        assertThat(shot.finalSurface()).isNotNull();

        WorldSession defaultWorld = normal.create(OWNER, 4243L, SMALL);
        assertThat(normal.status(OWNER, defaultWorld.id()).courseGeneratorVersion())
                .isEqualTo(CourseGenConstants.V5_GENERATOR_VERSION);
        normal.save(OWNER, defaultWorld.id(), "retained-v5-career");
        WorldSession retained = refined.load(OWNER, "retained-v5-career");
        assertThat(refined.status(OWNER, retained.id()).courseGeneratorVersion())
                .isEqualTo(CourseGenConstants.V5_GENERATOR_VERSION);
    }
}
