package com.progolf.sim;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Protects the purity of the simulation core (design D1): the sim.* packages must not depend on Spring,
 * persistence, or I/O. This test itself may use I/O; the constraint applies only to production sources.
 */
class ArchitecturePurityTest {

    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "import org.springframework",
            "import jakarta.persistence",
            "import javax.persistence",
            "import java.io.",
            "import java.nio.",
            "import java.sql.",
            "import java.net.",
            "import java.util.Random");

    @Test
    void coreSourcesHaveNoFrameworkOrIoDependencies() throws IOException {
        Path root = Path.of("src", "main", "java", "com", "progolf", "sim");
        assertThat(Files.isDirectory(root)).as("core source root exists").isTrue();

        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String content = Files.readString(p);
                    for (String forbidden : FORBIDDEN_IMPORTS) {
                        if (content.contains(forbidden)) {
                            violations.add(p + " -> " + forbidden);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        assertThat(violations).as("core must stay framework/IO-free").isEmpty();
    }
}
