package com.progolf.sim.population;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.ControlType;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.player.ProfessionalGolfer;
import org.junit.jupiter.api.Test;

/** golfer-creation spec: an archetype-shaped, human-controlled created golfer, deterministic from inputs. */
class GolferFactoryTest {

    private static ProfessionalGolfer create(Archetype archetype) {
        return GolferFactory.createHuman("player-x", "Ana", "Rivera", Nationality.ESP, 20, archetype, 2000);
    }

    @Test
    void aCreatedGolferIsHumanWithTheChosenIdentity() {
        ProfessionalGolfer g = create(Archetype.ALL_ROUNDER);
        assertThat(g.controlType()).isEqualTo(ControlType.HUMAN);
        assertThat(g.decisionPolicy()).isNull();
        assertThat(g.player().identity().fullName()).isEqualTo("Ana Rivera");
        assertThat(g.player().identity().nationality()).isEqualTo(Nationality.ESP);
        assertThat(g.player().identity().archetype()).isEqualTo(Archetype.ALL_ROUNDER);
        assertThat(g.player().identity().dateOfBirth().getYear()).isEqualTo(1980); // 2000 - 20
    }

    @Test
    void aPowerHitterStartsStrongerOffTheTeeThanOnTheGreen() {
        var golfer = create(Archetype.POWER_HITTER);
        var attrs = golfer.player().attributes();
        var potential = golfer.player().potential();
        assertThat(attrs.get(Attribute.DRIVING_DISTANCE))
                .isGreaterThan(attrs.get(Attribute.PUTTING_ACCURACY));
        // The archetype shapes the ceiling, and the starting build inherits that shape.
        assertThat(potential.get(Attribute.DRIVING_DISTANCE))
                .isGreaterThan(PopulationConstants.CREATION_POTENTIAL_BASELINE);
        assertThat(potential.get(Attribute.WEDGES))
                .isLessThan(PopulationConstants.CREATION_POTENTIAL_BASELINE);
    }

    @Test
    void anAllRounderStartsBalanced() {
        var golfer = create(Archetype.ALL_ROUNDER);
        for (Attribute a : Attribute.values()) {
            assertThat(golfer.player().potential().get(a)).as("potential %s", a)
                    .isEqualTo(PopulationConstants.CREATION_POTENTIAL_BASELINE);
        }
        var attrs = golfer.player().attributes();
        assertThat(Attribute.values()).allSatisfy(a ->
                assertThat(attrs.get(a)).as("attribute %s", a).isEqualTo(attrs.get(Attribute.DRIVING_DISTANCE)));
    }

    @Test
    void aCreatedGolferStartsWellShortOfTheirCeilingAndHasRoomToGrow() {
        var golfer = create(Archetype.ALL_ROUNDER); // created at 20: a prospect, not a finished article
        for (Attribute a : Attribute.values()) {
            assertThat(golfer.player().attributes().get(a)).as("attribute %s", a)
                    .isLessThan(golfer.player().potential().get(a));
        }
    }

    @Test
    void aCreatedGolfersCeilingIsHighEnoughToReachTheTop() {
        // The player is a genuine prospect: developed well, they can compete with the best golfers the
        // population generates. A ceiling below the population's top would make the career unwinnable.
        var potential = create(Archetype.ALL_ROUNDER).player().potential();
        assertThat(potential.get(Attribute.PUTTING_ACCURACY))
                .isGreaterThan((int) PopulationConstants.SKILL_MIN);
    }

    @Test
    void creationIsDeterministic() {
        var a = create(Archetype.PRECISION_PLAYER).player().attributes();
        var b = create(Archetype.PRECISION_PLAYER).player().attributes();
        for (Attribute at : Attribute.values()) {
            assertThat(a.get(at)).isEqualTo(b.get(at));
        }
    }

    @Test
    void startAgeIsValidated() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> GolferFactory.createHuman("p", "A", "B", Nationality.USA, 12, Archetype.ALL_ROUNDER, 2000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
