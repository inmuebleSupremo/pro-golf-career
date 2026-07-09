package com.progolf.sim.weather;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * weather-generation / playing-conditions spec (REQ-236/239): the Weather domain is a low-level producer
 * of environmental state. It depends only on core, course, and shot (the Environment interchange), and
 * never on higher domains — so it cannot become an authoritative source for, or mutate, their state.
 */
class WeatherBoundaryTest {

    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "import com.progolf.sim.tournament",
            "import com.progolf.sim.world",
            "import com.progolf.sim.ranking",
            "import com.progolf.sim.career",
            "import com.progolf.sim.tour",
            "import com.progolf.sim.population",
            "import com.progolf.sim.player");

    @Test
    void weatherDependsOnNoHigherDomain() throws IOException {
        Path root = Path.of("src", "main", "java", "com", "progolf", "sim", "weather");
        assertThat(Files.isDirectory(root)).as("weather source root exists").isTrue();

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
        assertThat(violations).as("weather must not import higher domains").isEmpty();
    }
}
