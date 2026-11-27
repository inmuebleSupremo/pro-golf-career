package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * World coordination-boundary spec (REQ-112): the World composes domains and does not reimplement their
 * work. It may carry shot intent and guidance for playable training, but must not reach into shot resolution
 * or branch on control type — it drives public domain operations.
 */
class WorldBoundaryTest {
    private static final Pattern SHOT_TYPE_REFERENCE = Pattern.compile("com\\.progolf\\.sim\\.shot\\.([A-Za-z][A-Za-z0-9]*)");
    private static final Set<String> TRAINING_COORDINATION_TYPES = Set.of(
            "BallStrikeIntent", "Club", "ClubSpec", "GolferState", "ShotGuidance");

    @Test
    void worldDelegatesAndDoesNotReimplementDomainWork() throws IOException {
        Path root = Path.of("src", "main", "java", "com", "progolf", "sim", "world");
        assertThat(Files.isDirectory(root)).isTrue();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String content = Files.readString(p);
                    // Training carries intent/club metadata and public guidance; shot resolution stays in its domain.
                    assertThat(content).as("%s must not import the entire shot package", p)
                            .doesNotContain("com.progolf.sim.shot.*");
                    Matcher references = SHOT_TYPE_REFERENCE.matcher(content);
                    while (references.find()) {
                        assertThat(TRAINING_COORDINATION_TYPES)
                                .as("%s must not depend on shot resolution type %s", p, references.group(1))
                                .contains(references.group(1));
                    }
                    // Identical rules for all: no control-type branching in world progression.
                    assertThat(content).as("%s must not branch on control type", p)
                            .doesNotContain("ControlType").doesNotContain("controlType");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
