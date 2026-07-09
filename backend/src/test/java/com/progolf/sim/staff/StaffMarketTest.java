package com.progolf.sim.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.progolf.sim.core.SplitMix64Rng;
import org.junit.jupiter.api.Test;

/** staff-influence spec: deterministic staff generation with quality-scaled costs (REQ-195). */
class StaffMarketTest {

    private final StaffMarket market = new StaffMarket();

    @Test
    void generationIsDeterministic() {
        assertThat(market.generate(StaffRole.COACH, new SplitMix64Rng(9L)))
                .isEqualTo(market.generate(StaffRole.COACH, new SplitMix64Rng(9L)));
    }

    @Test
    void candidateQualityIsInRangeAndCostsScaleWithQuality() {
        StaffMember m = market.generate(StaffRole.COACH, new SplitMix64Rng(3L));
        assertThat(m.quality()).isBetween(StaffConstants.QUALITY_MIN, StaffConstants.QUALITY_MAX);
        double expectedSalary = StaffRole.COACH.baseSalary()
                * (StaffConstants.SALARY_QUALITY_FLOOR + m.quality() * StaffConstants.SALARY_QUALITY_SPAN);
        assertThat(m.seasonalSalary()).isCloseTo(expectedSalary, within(1e-6));
        assertThat(m.hiringCost()).isCloseTo(expectedSalary * StaffConstants.HIRING_COST_FRACTION, within(1e-6));
    }

    @Test
    void higherQualityCandidatesCostMore() {
        // Scan seeds to find a clearly low- and high-quality candidate, then compare their salaries.
        StaffMember low = market.generate(StaffRole.CADDIE, new SplitMix64Rng(0L));
        StaffMember high = market.generate(StaffRole.CADDIE, new SplitMix64Rng(0L));
        for (long s = 0; s < 200; s++) {
            StaffMember m = market.generate(StaffRole.CADDIE, new SplitMix64Rng(s));
            if (m.quality() < low.quality()) {
                low = m;
            }
            if (m.quality() > high.quality()) {
                high = m;
            }
        }
        assertThat(high.quality()).isGreaterThan(low.quality());
        assertThat(high.seasonalSalary()).isGreaterThan(low.seasonalSalary());
    }
}
