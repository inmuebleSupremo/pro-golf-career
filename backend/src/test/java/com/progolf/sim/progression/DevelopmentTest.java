package com.progolf.sim.progression;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Player-development spec: allocation raises attributes, growth is bounded, AI allocation is deterministic. */
class DevelopmentTest {

    @Test
    void allocationRaisesTheChosenAttributeWithinTheSeasonCap() {
        Attributes before = Attributes.uniform(50);
        Attributes after = ProgressionEngine.applyAllocation(before, Map.of(Attribute.WEDGES, 1000));
        int gain = after.get(Attribute.WEDGES) - before.get(Attribute.WEDGES);
        assertThat(gain).isBetween(1, (int) ProgressionConstants.MAX_DEVELOPMENT_PER_SEASON);
    }

    @Test
    void seasonDevelopmentIsGraduallyBounded() {
        Attributes before = Attributes.uniform(45);
        Attributes after = ProgressionEngine.develop(before, 22); // young: most Development Points
        int totalGain = 0;
        for (Attribute a : Attribute.values()) {
            totalGain += Math.max(0, after.get(a) - before.get(a));
        }
        assertThat(totalGain).isBetween(1, (int) ProgressionConstants.MAX_DEVELOPMENT_PER_SEASON);
    }

    @Test
    void aiAllocationTargetsTheStrongestAttributesAndIsDeterministic() {
        Attributes attrs = Attributes.uniform(50).with(Attribute.DRIVING_DISTANCE, 90);
        Map<Attribute, Integer> a = AllocationPolicy.aiAllocate(attrs, 50);
        Map<Attribute, Integer> b = AllocationPolicy.aiAllocate(attrs, 50);
        assertThat(a).isEqualTo(b); // deterministic
        assertThat(a).containsKey(Attribute.DRIVING_DISTANCE); // specialises on the strongest
        assertThat(a.keySet()).hasSize(ProgressionConstants.AI_FOCUS_ATTRIBUTES);
    }

    @Test
    void developIsDeterministic() {
        Attributes attrs = Attributes.uniform(60);
        assertThat(ProgressionEngine.develop(attrs, 24).get(Attribute.DRIVING_ACCURACY))
                .isEqualTo(ProgressionEngine.develop(attrs, 24).get(Attribute.DRIVING_ACCURACY));
    }
}
