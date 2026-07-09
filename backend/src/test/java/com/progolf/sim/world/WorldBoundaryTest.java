package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * World coordination-boundary spec (REQ-112): the World composes domains and does not reimplement their
 * work. It must not reach into shot resolution nor branch on control type — it drives public operations.
 */
class WorldBoundaryTest {

    @Test
    void worldDelegatesAndDoesNotReimplementDomainWork() throws IOException {
        Path root = Path.of("src", "main", "java", "com", "progolf", "sim", "world");
        assertThat(Files.isDirectory(root)).isTrue();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String content = Files.readString(p);
                    // Shot resolution is the tournament engine's job; the World composes it, never calls it.
                    assertThat(content).as("%s must not reach into the shot engine", p)
                            .doesNotContain("com.progolf.sim.shot");
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
