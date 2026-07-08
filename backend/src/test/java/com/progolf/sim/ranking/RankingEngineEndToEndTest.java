package com.progolf.sim.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.tournament.Tier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** End-to-end & integrity: several events converge to a reproducible ranking; no control-type branch. */
class RankingEngineEndToEndTest {

    private static double meanAttribute(ProfessionalGolfer g) {
        double sum = 0;
        for (Attribute a : Attribute.values()) {
            sum += g.player().attributes().get(a);
        }
        return sum / Attribute.values().length;
    }

    private static WorldRanking runSeason(List<ProfessionalGolfer> order) {
        WorldRanking wr = new WorldRanking();
        // Three events across the year with the same strong-first finishing order.
        wr.record(RankingFixtures.result("Event 1", order), Tier.STANDARD, LocalDate.of(2001, 2, 1), 1);
        wr.record(RankingFixtures.result("Event 2", order), Tier.PREMIER, LocalDate.of(2001, 6, 1), 2);
        wr.record(RankingFixtures.result("Event 3", order), Tier.MAJOR, LocalDate.of(2001, 9, 1), 3);
        return wr;
    }

    @Test
    void multipleEventsConvergeToAReproduciblePlausibleRanking() {
        List<ProfessionalGolfer> field = new ArrayList<>(RankingFixtures.golfers(40));
        // Make the strongest golfers finish first, so the ranking should crown a genuinely strong player.
        field.sort(Comparator.comparingDouble(RankingEngineEndToEndTest::meanAttribute).reversed());

        LocalDate asOf = LocalDate.of(2001, 9, 2);
        WorldRanking a = runSeason(field);
        WorldRanking b = runSeason(field);

        RankingSnapshot ra = a.rankingAsOf(asOf);
        RankingSnapshot rb = b.rankingAsOf(asOf);

        assertThat(ra.size()).isGreaterThan(0);
        // Reproducible.
        assertThat(ra.standings()).isEqualTo(rb.standings());
        // The repeat winner is World #1 with a positive value.
        assertThat(ra.leader().orElseThrow().golferId()).isEqualTo(field.get(0).player().id());
        assertThat(ra.leader().orElseThrow().rankingValue()).isGreaterThan(0);
        // Unique, contiguous positions.
        for (int i = 0; i < ra.standings().size(); i++) {
            assertThat(ra.standings().get(i).position()).isEqualTo(i + 1);
        }
    }

    @Test
    void rankingDoesNotBranchOnControlType() throws IOException {
        Path root = Path.of("src", "main", "java", "com", "progolf", "sim", "ranking");
        assertThat(Files.isDirectory(root)).isTrue();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    assertThat(Files.readString(p))
                            .as("%s must not branch on control type", p)
                            .doesNotContain("ControlType")
                            .doesNotContain("controlType");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
