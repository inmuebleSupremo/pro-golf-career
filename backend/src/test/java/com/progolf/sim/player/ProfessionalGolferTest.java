package com.progolf.sim.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.shot.Strategy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Professional-golfer spec: composition, immutable control type, decision seam, no gameplay branch. */
class ProfessionalGolferTest {

    private static Player player() {
        Identity id = new Identity("Rory", "Walsh", Nationality.IRL, LocalDate.of(1994, 4, 4), Archetype.TOP_COLLEGE_GRADUATE);
        return new Player("p-1", id, Attributes.uniform(70));
    }

    @Test
    void humanGolferCarriesNoDecisionPolicy() {
        ProfessionalGolfer g = ProfessionalGolfer.human("g-1", player(), "career-1");
        assertThat(g.controlType()).isEqualTo(ControlType.HUMAN);
        assertThat(g.policy()).isEmpty();
    }

    @Test
    void simulationGolferCarriesADecisionSeam() {
        DecisionPolicy policy = () -> Strategy.CONSERVATIVE;
        ProfessionalGolfer g = ProfessionalGolfer.simulation("g-2", player(), "career-2", policy);
        assertThat(g.controlType()).isEqualTo(ControlType.SIMULATION);
        assertThat(g.policy()).isPresent();
        assertThat(g.policy().get().defaultStrategy()).isEqualTo(Strategy.CONSERVATIVE);
    }

    @Test
    void humanGolferWithAPolicyIsRejected() {
        assertThatThrownBy(() -> new ProfessionalGolfer("g", player(), "c", ControlType.HUMAN, () -> Strategy.BALANCED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shotEngineDoesNotDependOnControlTypeOrThePlayerPackage() throws IOException {
        // The shared gameplay engine must not branch on control type: sim.shot references neither
        // ControlType nor the player package.
        Path shotRoot = Path.of("src", "main", "java", "com", "progolf", "sim", "shot");
        assertThat(Files.isDirectory(shotRoot)).isTrue();
        try (Stream<Path> files = Files.walk(shotRoot)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String content = Files.readString(p);
                    assertThat(content).as("%s must not import the player package", p).doesNotContain("com.progolf.sim.player");
                    assertThat(content).as("%s must not reference ControlType", p).doesNotContain("ControlType");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
