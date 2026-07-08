package com.progolf.sim.population;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.player.CareerStatus;
import com.progolf.sim.player.ControlType;
import com.progolf.sim.player.Player;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.RoundOutcome;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.Strategy;
import java.lang.reflect.RecordComponent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Golfer-population spec: reproducible, diverse, replenished, control-free rules, no scripted rivals. */
class PopulationGeneratorTest {

    private static final SeedCoordinate BASE = new SeedCoordinate(0xABCDEF12L, 1, 1, 0, 0, 0, 0);

    private static double meanAttribute(Player p) {
        double sum = 0;
        for (Attribute a : Attribute.values()) {
            sum += p.attributes().get(a);
        }
        return sum / Attribute.values().length;
    }

    private static Attribute strongest(Player p) {
        Attribute best = Attribute.values()[0];
        for (Attribute a : Attribute.values()) {
            if (p.attributes().get(a) > p.attributes().get(best)) {
                best = a;
            }
        }
        return best;
    }

    @Test
    void generationIsReproducibleFromSeed() {
        List<ProfessionalGolfer> a = PopulationGenerator.generate(BASE, 25);
        List<ProfessionalGolfer> b = PopulationGenerator.generate(BASE, 25);
        assertThat(a).hasSize(25);
        for (int i = 0; i < 25; i++) {
            assertThat(a.get(i).player().id()).isEqualTo(b.get(i).player().id());
            assertThat(a.get(i).player().identity()).isEqualTo(b.get(i).player().identity());
            for (Attribute attr : Attribute.values()) {
                assertThat(a.get(i).player().attributes().get(attr))
                        .isEqualTo(b.get(i).player().attributes().get(attr));
            }
        }
    }

    @Test
    void populationIsDiverseInSkillAndStrengths() {
        List<ProfessionalGolfer> field = PopulationGenerator.generate(BASE, 60);
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        Set<Attribute> strengths = new HashSet<>();
        for (ProfessionalGolfer g : field) {
            double mean = meanAttribute(g.player());
            min = Math.min(min, mean);
            max = Math.max(max, mean);
            strengths.add(strongest(g.player()));
        }
        // Meaningful spread of overall skill, and golfers specialise in different attributes.
        assertThat(max - min).isGreaterThan(5.0);
        assertThat(strengths.size()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void generatedGolfersAreSimulationControlledAndActive() {
        for (ProfessionalGolfer g : PopulationGenerator.generate(BASE, 10)) {
            assertThat(g.controlType()).isEqualTo(ControlType.SIMULATION);
            assertThat(g.policy()).isPresent();
            assertThat(g.player().status()).isEqualTo(CareerStatus.ACTIVE);
        }
    }

    @Test
    void replenishmentAddsFreshDistinctGolfersAtNewIndices() {
        List<ProfessionalGolfer> initial = PopulationGenerator.generate(BASE, 20);
        List<ProfessionalGolfer> added = PopulationGenerator.replenish(BASE, 20, 5);
        assertThat(added).hasSize(5);
        Set<String> initialIds = new HashSet<>();
        initial.forEach(g -> initialIds.add(g.player().id()));
        for (int i = 0; i < 5; i++) {
            // Fresh index -> distinct golfer, and deterministic (matches generateOne at that index).
            assertThat(initialIds).doesNotContain(added.get(i).player().id());
            assertThat(added.get(i).player().id())
                    .isEqualTo(PopulationGenerator.generateOne(BASE, 20 + i).player().id());
        }
    }

    @Test
    void noScriptedRivalFields() {
        for (RecordComponent rc : ProfessionalGolfer.class.getRecordComponents()) {
            assertThat(rc.getName().toLowerCase()).doesNotContain("rival");
        }
    }

    @Test
    void generatedPlayerFeedsTheShotEngineEndToEnd() {
        // A generated Player's attributes + toGolferState drive the existing engine with no changes.
        ProfessionalGolfer g = PopulationGenerator.generateOne(BASE, 3);
        Course course = CourseGenerator.generate(new SeedCoordinate(1, 1, 1, 0, 0, 0, 0), EnvironmentClassification.PARKLAND);
        HoleModel hole = course.holeModel(1, 1);

        RoundOutcome round = RoundResolver.resolveHole(
                hole,
                g.player().attributes(),
                g.player().toGolferState(0.0),
                Environment.calm(),
                Strategy.BALANCED,
                new SeedCoordinate(0xABCDEF12L, 1, 1, 1, 3, 1, 0));

        assertThat(round.shots()).isNotEmpty();
    }
}
