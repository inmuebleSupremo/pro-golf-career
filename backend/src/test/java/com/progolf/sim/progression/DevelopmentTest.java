package com.progolf.sim.progression;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Player-development spec: allocation raises attributes, growth is bounded, AI allocation is deterministic. */
class DevelopmentTest {

    /** Ample headroom, so these tests exercise allocation rather than the potential ceiling. */
    private static final Attributes UNCAPPED = Attributes.uniform(100);

    @Test
    void allocationRaisesTheChosenAttributeWithinTheSeasonCap() {
        Attributes before = Attributes.uniform(50);
        Attributes after = ProgressionEngine.applyAllocation(before, UNCAPPED, Map.of(Attribute.WEDGES, 1000));
        int gain = after.get(Attribute.WEDGES) - before.get(Attribute.WEDGES);
        assertThat(gain).isBetween(1, (int) ProgressionConstants.MAX_DEVELOPMENT_PER_SEASON);
    }

    @Test
    void seasonDevelopmentIsGraduallyBounded() {
        Attributes before = Attributes.uniform(45);
        Attributes after = ProgressionEngine.develop(before, UNCAPPED, 22); // young: most Development Points
        int totalGain = 0;
        for (Attribute a : Attribute.values()) {
            totalGain += Math.max(0, after.get(a) - before.get(a));
        }
        assertThat(totalGain).isBetween(1, (int) ProgressionConstants.MAX_DEVELOPMENT_PER_SEASON);
    }

    @Test
    void aiAllocationWorksTheWholeGameAndIsDeterministic() {
        Attributes attrs = Attributes.uniform(50).with(Attribute.DRIVING_DISTANCE, 90);
        Map<Attribute, Integer> a = AllocationPolicy.aiAllocate(attrs, UNCAPPED, 500);
        Map<Attribute, Integer> b = AllocationPolicy.aiAllocate(attrs, UNCAPPED, 500);
        assertThat(a).isEqualTo(b); // deterministic
        // Every attribute with headroom is developed: a golfer works on their whole game, not three of it.
        assertThat(a.keySet()).containsExactlyInAnyOrder(Attribute.values());
    }

    @Test
    void aiAllocationSkipsAttributesAlreadyAtTheirCeiling() {
        // The golfer's best attribute is maxed out; points must go to the ones with headroom instead.
        Attributes potential = Attributes.uniform(80).with(Attribute.DRIVING_DISTANCE, 90);
        Attributes attrs = Attributes.uniform(50).with(Attribute.DRIVING_DISTANCE, 90);

        Map<Attribute, Integer> allocation = AllocationPolicy.aiAllocate(attrs, potential, 500);

        assertThat(allocation).doesNotContainKey(Attribute.DRIVING_DISTANCE);
        assertThat(allocation.keySet()).hasSize(Attribute.values().length - 1);
    }

    @Test
    void aFullyRealisedGolferHasNothingLeftToBuy() {
        Attributes maxed = Attributes.uniform(70);
        assertThat(AllocationPolicy.aiAllocate(maxed, maxed, 500)).isEmpty();
    }

    @Test
    void developIsDeterministic() {
        Attributes attrs = Attributes.uniform(60);
        assertThat(ProgressionEngine.develop(attrs, UNCAPPED, 24).get(Attribute.DRIVING_ACCURACY))
                .isEqualTo(ProgressionEngine.develop(attrs, UNCAPPED, 24).get(Attribute.DRIVING_ACCURACY));
    }
}
