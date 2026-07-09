package com.progolf.sim.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * physical-state / availability spec (REQ-225/227): the Health domain is a pure producer of Physical
 * State. It depends only on {@code core} and never on another domain, so it cannot compute or modify
 * scoring, rankings, finances, progression, or shot resolution.
 */
class HealthBoundaryTest {

    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "import com.progolf.sim.tournament",
            "import com.progolf.sim.world",
            "import com.progolf.sim.ranking",
            "import com.progolf.sim.career",
            "import com.progolf.sim.tour",
            "import com.progolf.sim.population",
            "import com.progolf.sim.player",
            "import com.progolf.sim.weather",
            "import com.progolf.sim.economy",
            "import com.progolf.sim.shot",
            "import com.progolf.sim.spatial",
            "import com.progolf.sim.course",
            "import com.progolf.sim.progression");

    @Test
    void healthDependsOnlyOnCore() throws IOException {
        Path root = Path.of("src", "main", "java", "com", "progolf", "sim", "health");
        assertThat(Files.isDirectory(root)).as("health source root exists").isTrue();

        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String content = Files.readString(p);
                    for (String forbidden : FORBIDDEN_IMPORTS) {
                        if (content.contains(forbidden)) {
                            violations.add(p.getFileName() + " -> " + forbidden);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        assertThat(violations).as("health must depend only on core").isEmpty();
    }
}
