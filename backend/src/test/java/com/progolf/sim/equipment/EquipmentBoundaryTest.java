package com.progolf.sim.equipment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * equipment-influence spec (REQ-214): the Equipment domain owns ownership + characteristics only. It
 * depends solely on {@code core} and never on another domain, so it computes no shots and writes no
 * attributes, scores, rankings, or finances — the World applies its characteristics through the shot engine.
 */
class EquipmentBoundaryTest {

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
            "import com.progolf.sim.health",
            "import com.progolf.sim.staff",
            "import com.progolf.sim.shot",
            "import com.progolf.sim.spatial",
            "import com.progolf.sim.course",
            "import com.progolf.sim.progression");

    @Test
    void equipmentDependsOnlyOnCore() throws IOException {
        Path root = Path.of("src", "main", "java", "com", "progolf", "sim", "equipment");
        assertThat(Files.isDirectory(root)).as("equipment source root exists").isTrue();

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
        assertThat(violations).as("equipment must depend only on core").isEmpty();
    }
}
