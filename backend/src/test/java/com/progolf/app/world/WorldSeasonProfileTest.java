package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.sim.core.SeasonProfile;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class WorldSeasonProfileTest {
    private static final String OWNER = "profile-owner";
    private static final String PROPERTY = "progolf.profile.season";
    private static final WorldConfig TINY = new WorldConfig(12, 2, 1, 6, 3, 0, 0);

    @TempDir Path dir;
    private String originalProperty;

    @BeforeEach
    void rememberProperty() {
        originalProperty = System.getProperty(PROPERTY);
    }

    @AfterEach
    void restoreProperty() {
        if (originalProperty == null) System.clearProperty(PROPERTY);
        else System.setProperty(PROPERTY, originalProperty);
        assertThat(SeasonProfile.current()).isNull();
    }

    @Test
    void profilingIsDisabledByDefault(CapturedOutput output) {
        System.clearProperty(PROPERTY);
        WorldService service = service(dir.resolve("disabled"));
        String id = service.create(OWNER, 11L, TINY).id();

        assertThat(service.advanceSeason(OWNER, id, "disabled", 1).state())
                .isEqualTo(SeasonAdvanceOperation.State.COMPLETED);
        assertThat(output.toString()).doesNotContain("seasonProfile outcome=");
    }

    @Test
    void enabledProfileLogsOneSummaryWithoutChangingWorldOrSave(CapturedOutput output) {
        System.setProperty(PROPERTY, "true");
        WorldService profiled = service(dir.resolve("profiled"));
        String profiledId = profiled.create(OWNER, 12L, TINY).id();
        profiled.assignPlayer(OWNER, profiledId, profiled.get(OWNER, profiledId).world().activeGolferIds().get(0));

        assertThat(profiled.advanceSeason(OWNER, profiledId, "profiled", 1).state())
                .isEqualTo(SeasonAdvanceOperation.State.COMPLETED);
        var result = profiled.get(OWNER, profiledId).world().snapshot();
        assertThat(profiled.load(OWNER, WorldService.AUTOSAVE_ID).world().snapshot()).isEqualTo(result);

        System.setProperty(PROPERTY, "false");
        WorldService plain = service(dir.resolve("plain"));
        String plainId = plain.create(OWNER, 12L, TINY).id();
        plain.assignPlayer(OWNER, plainId, plain.get(OWNER, plainId).world().activeGolferIds().get(0));
        plain.advanceSeason(OWNER, plainId, "plain", 1);
        assertThat(plain.get(OWNER, plainId).world().snapshot()).isEqualTo(result);

        String log = output.toString();
        assertThat(log.split("seasonProfile outcome=", -1)).hasSize(2);
        assertThat(log).contains("seasonProfile outcome=COMPLETED", "backendTotalMs=", "worldMs=",
                "autosaveMs=", "eventSetupMs=", "eventPlayMs=", "greenGenerationMs=",
                "strategicPlanningMs=", "strategicPlanningCalls=", "physicalPuttSolverMs=",
                "physicalPuttSolverCalls=", "eventResultsMs=",
                "seasonEndMs=", "seasonRankingMs=", "developmentMs=", "financeMs=",
                "snapshotMs=", "serializationMs=", "checksumMs=", "fileWriteMs=",
                "events=", "physicalEvents=", "holeModels=", "greenSurfaces=",
                "timing=childrenIncludedInParents", "greenGenerationNestedIn=eventPlayWhereApplicable",
                "hotPathsNestedIn=eventPlayWhereApplicable", "hotPathsDisjointFrom=greenGeneration");
        var greenTiming = Pattern.compile("greenGenerationMs=([0-9]+\\.[0-9]{3})").matcher(log);
        assertThat(greenTiming.find()).isTrue();
        assertThat(Double.parseDouble(greenTiming.group(1))).isPositive();
        assertThat(log).containsPattern("physicalEvents=[1-9][0-9]*")
                .containsPattern("holeModels=[1-9][0-9]*")
                .containsPattern("greenSurfaces=[1-9][0-9]*")
                .containsPattern("strategicPlanningCalls=[1-9][0-9]*")
                .containsPattern("physicalPuttSolverCalls=[1-9][0-9]*");
    }

    @Test
    void failedAutosaveLogsFailureAndClearsRequestScope(CapturedOutput output) {
        System.setProperty(PROPERTY, "true");
        WorldService service = new WorldService(new FilesystemSaveGameStore(dir.resolve("failure").toString()) {
            @Override
            public String save(String ownerId, String saveId, com.progolf.app.persistence.SaveGame game) {
                throw new IllegalStateException("deliberate autosave failure");
            }
        });
        String id = service.create(OWNER, 13L, TINY).id();

        assertThatThrownBy(() -> service.advanceSeason(OWNER, id, "failed", 1))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("deliberate autosave failure");
        assertThat(service.seasonAdvanceOperation(OWNER, id).state()).isEqualTo(SeasonAdvanceOperation.State.FAILED);
        assertThat(SeasonProfile.current()).isNull();
        assertThat(output.toString()).contains("seasonProfile outcome=FAILED", "greenGenerationMs=",
                "strategicPlanningMs=", "physicalPuttSolverMs=", "strategicPlanningCalls=",
                "physicalPuttSolverCalls=");
    }

    private static WorldService service(Path savesDir) {
        return new WorldService(new FilesystemSaveGameStore(savesDir.toString()));
    }
}
