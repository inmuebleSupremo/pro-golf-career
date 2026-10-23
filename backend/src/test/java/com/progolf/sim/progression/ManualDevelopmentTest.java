package com.progolf.sim.progression;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.player.AttributeChange;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ManualDevelopmentTest {

    @Test
    void purchaseOptionsExplainTheActualBlocker() {
        Attribute target = Attribute.PUTTING_ACCURACY;
        var noFunds = ManualDevelopment.options(Attributes.uniform(63), Attributes.uniform(97),
                0, List.of(), 2).stream().filter(o -> o.attribute() == target).findFirst().orElseThrow();
        assertThat(noFunds.nextCost()).isEqualTo(11);
        assertThat(noFunds.remainingSeasonal()).isEqualTo(6);
        assertThat(noFunds.blockingReason()).isEqualTo(ManualDevelopment.BlockingReason.INSUFFICIENT_POINTS);

        var atPotential = ManualDevelopment.options(Attributes.uniform(63), Attributes.uniform(63),
                100, List.of(), 2).stream().filter(o -> o.attribute() == target).findFirst().orElseThrow();
        assertThat(atPotential.potentialHeadroom()).isZero();
        assertThat(atPotential.nextCost()).isNull();
        assertThat(atPotential.blockingReason()).isEqualTo(ManualDevelopment.BlockingReason.AT_POTENTIAL);

        var atLimit = ManualDevelopment.options(Attributes.uniform(69), Attributes.uniform(97),
                100, List.of(new AttributeChange(target, 6, AttributeChange.Reason.DEVELOPMENT, 2),
                        new AttributeChange(target, 2, AttributeChange.Reason.AGING, 2),
                        new AttributeChange(target, 4, AttributeChange.Reason.DEVELOPMENT, 1)),
                2).stream().filter(o -> o.attribute() == target).findFirst().orElseThrow();
        assertThat(atLimit.remainingSeasonal()).isZero();
        assertThat(atLimit.blockingReason()).isEqualTo(ManualDevelopment.BlockingReason.SEASON_LIMIT);
    }

    @Test
    void missingLegacyPurchaseHistoryLeavesExistingRatingsAndBankAvailable() {
        Attribute target = Attribute.PUTTING_ACCURACY;
        Attributes existing = Attributes.uniform(75);
        Attributes potential = Attributes.uniform(90);
        var legacy = ManualDevelopment.options(existing, potential, 300, List.of(), 2);
        var option = legacy.stream().filter(o -> o.attribute() == target).findFirst().orElseThrow();
        assertThat(option.rating()).isEqualTo(75);
        assertThat(option.remainingSeasonal()).isEqualTo(6);
        assertThat(option.purchasable()).isTrue();
        assertThat(ManualDevelopment.costOfPurchase(Map.of(target, 6), legacy, 300)).isPositive();

        var afterPurchase = ManualDevelopment.options(existing.with(target, 81), potential, 300,
                List.of(new AttributeChange(target, 6, AttributeChange.Reason.DEVELOPMENT, 2)), 2);
        assertThat(afterPurchase.stream().filter(o -> o.attribute() == target).findFirst()
                .orElseThrow().remainingSeasonal()).isZero();
    }
}
