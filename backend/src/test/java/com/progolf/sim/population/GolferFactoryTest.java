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
        var attrs = create(Archetype.POWER_HITTER).player().attributes();
        assertThat(attrs.get(Attribute.DRIVING_DISTANCE))
                .isGreaterThan(attrs.get(Attribute.PUTTING_ACCURACY));
        assertThat(attrs.get(Attribute.DRIVING_DISTANCE))
                .isGreaterThan(PopulationConstants.CREATION_BASELINE);
        assertThat(attrs.get(Attribute.WEDGES)).isLessThan(PopulationConstants.CREATION_BASELINE);
    }

    @Test
    void anAllRounderStartsBalanced() {
        var attrs = create(Archetype.ALL_ROUNDER).player().attributes();
        for (Attribute a : Attribute.values()) {
            assertThat(attrs.get(a)).as("attribute %s", a).isEqualTo(PopulationConstants.CREATION_BASELINE);
        }
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
