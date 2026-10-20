package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.sim.course.CourseGenConstants;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Generator 8 is opt-in for new careers, persists its exact route strategy, and never reinterprets saved worlds. */
class PrincipledArchitecturePlayableWorldTest {
    private static final String OWNER = "principled-architecture-test";
    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 6);

    @TempDir Path saves;

    @Test
    void optedInCareerPlaysPersistedGeneratorEightGeometryAndRestoresItsPinnedVersion() {
        var store = new FilesystemSaveGameStore(saves.toString());
        WorldService optedIn = new WorldService(store, false, true);
        WorldSession created = optedIn.create(OWNER, 8301L, SMALL);
        assertThat(optedIn.status(OWNER, created.id()).courseGeneratorVersion())
                .isEqualTo(CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        var generated = CourseGenerator.generate(new SeedCoordinate(8301L, 0, 0, 0, 0, 0, 0),
                EnvironmentClassification.PARKLAND, CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        assertThat(generated.generatorVersion()).isEqualTo(CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        assertThat(generated.holes()).allSatisfy(hole -> {
            assertThat(hole.architectureStrategyPlan()).isNotNull();
            assertThat(hole.forRound(1).architectureStrategyPlan()).isEqualTo(hole.architectureStrategyPlan());
        });

        optedIn.createPlayer(OWNER, created.id(), "Test", "Golfer", Nationality.GBR, 19, Archetype.POWER_HITTER);
        optedIn.save(OWNER, created.id(), "principled-career");

        WorldService normal = new WorldService(store, false, false);
        WorldSession loaded = normal.load(OWNER, "principled-career");
        assertThat(normal.status(OWNER, loaded.id()).courseGeneratorVersion())
                .isEqualTo(CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);

        int weeks = 0;
        while (!normal.hasPendingEvent(OWNER, loaded.id()) && weeks++ < 25) {
            normal.advanceWeek(OWNER, loaded.id());
        }
        assertThat(normal.hasPendingEvent(OWNER, loaded.id())).isTrue();
        var hole = normal.currentPlayingHole(OWNER, loaded.id(), null);
        assertThat(hole.courseGeneratorVersion()).isEqualTo(CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        assertThat(hole.canonicalGeometrySource()).isEqualTo("GENERATOR_8_PRINCIPLED_ARCHITECTURE");
        assertThat(hole.canonicalBiome()).isNotBlank();
        assertThat(hole.geometry().regions()).isNotEmpty();
        assertThat(normal.currentSituation(OWNER, loaded.id())).isNotNull();
        assertThat(normal.simShot(OWNER, loaded.id()).trace()).isNotNull();

        WorldSession defaultWorld = normal.create(OWNER, 8302L, SMALL);
        assertThat(normal.status(OWNER, defaultWorld.id()).courseGeneratorVersion())
                .isEqualTo(CourseGenConstants.V5_GENERATOR_VERSION);
    }
}
