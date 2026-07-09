package com.progolf.sim.player;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Player-entity (modified): attributes evolve up/down, clamp, are recorded, and never change via the shot engine. */
class PlayerEvolutionTest {

    private static Player player() {
        Identity id = new Identity("Evo", "Player", Nationality.USA, LocalDate.of(1990, 1, 1), Archetype.TOP_COLLEGE_GRADUATE);
        return new Player("p", id, Attributes.uniform(50));
    }

    @Test
    void attributesEvolveUpAndDownClampedAndRecorded() {
        Player p = player();
        p.evolveAttributes(p.attributes().with(Attribute.WEDGES, 55), AttributeChange.Reason.DEVELOPMENT, 1);
        assertThat(p.attributes().get(Attribute.WEDGES)).isEqualTo(55);

        p.evolveAttributes(p.attributes().with(Attribute.WEDGES, 52), AttributeChange.Reason.AGING, 2);
        assertThat(p.attributes().get(Attribute.WEDGES)).isEqualTo(52);

        // Clamping: an out-of-range target is clamped by Attributes.with before it reaches storage.
        p.evolveAttributes(p.attributes().with(Attribute.WEDGES, 999), AttributeChange.Reason.DEVELOPMENT, 3);
        assertThat(p.attributes().get(Attribute.WEDGES)).isEqualTo(100);

        // Every change recorded with its reason.
        assertThat(p.attributeChanges()).extracting(AttributeChange::reason)
                .containsExactly(AttributeChange.Reason.DEVELOPMENT, AttributeChange.Reason.AGING, AttributeChange.Reason.DEVELOPMENT);
    }

    @Test
    void theShotEngineNeverEvolvesAttributes() throws IOException {
        // REQ-049: only development/aging change attributes. The shot engine must never call the evolution
        // path — it reads attributes only.
        Path shotRoot = Path.of("src", "main", "java", "com", "progolf", "sim", "shot");
        assertThat(Files.isDirectory(shotRoot)).isTrue();
        try (Stream<Path> files = Files.walk(shotRoot)) {
            files.filter(f -> f.toString().endsWith(".java")).forEach(f -> {
                try {
                    assertThat(Files.readString(f)).as("%s must not evolve attributes", f)
                            .doesNotContain("evolveAttributes");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
