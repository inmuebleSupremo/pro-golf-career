package com.progolf.sim.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import org.junit.jupiter.api.Test;

/**
 * equipment-influence: brands make gear a real trade-off (not a strictly-ordered quality ladder), and fit
 * measures whether a piece complements a golfer's weakness.
 */
class EquipmentBrandFitTest {

    @Test
    void brandsTradeOffCharacteristicsWithoutChangingTheBudget() {
        double base = 0.6;
        EquipmentCharacteristics apex = EquipmentBrand.APEX.characteristics(base);
        // A power brand is stronger in power than forgiveness — a genuine trade-off.
        assertThat(apex.power()).isGreaterThan(apex.forgiveness());
        // The overall budget (mean) is preserved, so same-tier gear from any brand costs the same.
        double mean = (apex.forgiveness() + apex.power() + apex.workability() + apex.feel()) / 4.0;
        assertThat(mean).isCloseTo(base, within(1e-9));

        // The forgiveness brand has the opposite bias.
        EquipmentCharacteristics everman = EquipmentBrand.EVERMAN.characteristics(base);
        assertThat(everman.forgiveness()).isGreaterThan(everman.power());
    }

    @Test
    void fitRewardsGearThatComplementsAWeakness() {
        // A wayward golfer: weak in accuracy, strong elsewhere — wants forgiveness, not power.
        Attributes wayward = Attributes.uniform(85)
                .with(Attribute.DRIVING_ACCURACY, 35)
                .with(Attribute.IRONS_ACCURACY, 35);
        double base = 0.6;

        double forgivingFit = EquipmentFit.fit(EquipmentBrand.EVERMAN.characteristics(base), wayward);
        double poweredFit = EquipmentFit.fit(EquipmentBrand.APEX.characteristics(base), wayward);
        double balancedFit = EquipmentFit.fit(EquipmentBrand.MERIDIAN.characteristics(base), wayward);

        assertThat(balancedFit).as("a balanced item is neutral").isCloseTo(0.5, within(1e-9));
        assertThat(forgivingFit).as("forgiveness complements the weakness").isGreaterThan(balancedFit);
        assertThat(poweredFit).as("power emphasises the wrong thing").isLessThan(balancedFit);
    }
}
