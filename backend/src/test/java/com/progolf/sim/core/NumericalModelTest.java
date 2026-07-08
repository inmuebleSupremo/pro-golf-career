package com.progolf.sim.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Numerical-model spec: value categories, pipeline order, bounds, and guarded operations. */
class NumericalModelTest {

    @Test
    void pipelineRandomnessRunsAfterShapingAndBeforeSafetyNet() {
        assertThat(PipelineStep.ENVIRONMENTAL_EFFECTS.isBefore(PipelineStep.CONTROLLED_RANDOMNESS)).isTrue();
        assertThat(PipelineStep.CONTROLLED_RANDOMNESS.isBefore(PipelineStep.SAFETY_NET)).isTrue();
        assertThat(PipelineStep.SAFETY_NET.isBefore(PipelineStep.FINAL_OUTCOME)).isTrue();
    }

    @Test
    void pipelineDeclaresExactlySevenStepsInCanonicalOrder() {
        assertThat(PipelineStep.values()).containsExactly(
                PipelineStep.BASE_ATTRIBUTE,
                PipelineStep.PERMANENT_CAREER_EFFECTS,
                PipelineStep.TEMPORARY_MODIFIERS,
                PipelineStep.ENVIRONMENTAL_EFFECTS,
                PipelineStep.CONTROLLED_RANDOMNESS,
                PipelineStep.SAFETY_NET,
                PipelineStep.FINAL_OUTCOME);
    }

    @Test
    void onlyAttributeCategoryIsTrainable() {
        assertThat(ValueCategory.ATTRIBUTE.isTrainable()).isTrue();
        assertThat(ValueCategory.MODIFIER.isTrainable()).isFalse();
        assertThat(ValueCategory.RATING.isTrainable()).isFalse();
        assertThat(ValueCategory.STATE.isTrainable()).isFalse();
        assertThat(ValueCategory.OUTCOME.isTrainable()).isFalse();
    }

    @Test
    void developmentRejectsNonTrainableCategory() {
        assertThatThrownBy(() -> Development.assertTrainable(ValueCategory.MODIFIER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void developmentAppliesToAttributesAndClampsToRange() {
        Attributes base = Attributes.uniform(50);
        Attributes improved = Development.applyPoints(base, Attribute.PUTTING_ACCURACY, 10);
        assertThat(improved.get(Attribute.PUTTING_ACCURACY)).isEqualTo(60);

        Attributes capped = Development.applyPoints(base, Attribute.WEDGES, 1000);
        assertThat(capped.get(Attribute.WEDGES)).isEqualTo(Attributes.MAX);
    }

    @Test
    void outcomeCannotBeUsedAsCalculationInput() {
        assertThatThrownBy(() -> CalculationInputs.require(ValueCategory.OUTCOME))
                .isInstanceOf(IllegalArgumentException.class);
        // Other categories are valid inputs.
        CalculationInputs.require(ValueCategory.STATE);
        CalculationInputs.require(ValueCategory.MODIFIER);
    }

    @Test
    void constructingAttributesOutOfRangeIsRejected() {
        assertThatThrownBy(() -> Attributes.uniform(150)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Attributes.uniform(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withClampsToStoredRange() {
        // with() clamps rather than throwing, keeping stored values inside 0-100.
        assertThat(Attributes.uniform(50).with(Attribute.COMPOSURE, 150).get(Attribute.COMPOSURE)).isEqualTo(100);
        assertThat(Attributes.uniform(50).with(Attribute.COMPOSURE, -20).get(Attribute.COMPOSURE)).isEqualTo(0);
    }
}
