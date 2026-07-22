package com.progolf.sim.staff;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SplitMix64Rng;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** support-team spec: the persistent hire pool generates named profiles, offers a variety-weighted subset. */
class StaffPoolTest {

    @Test
    void generatesNamedProfilesForEveryRole() {
        StaffPool pool = StaffPool.generate(1L, new StaffMarket(), 4);
        assertThat(pool.available()).hasSize(4 * StaffRole.values().length);
        // Real names (a first + last), not the old "ROLE-12345" serials.
        assertThat(pool.available()).allSatisfy(m -> {
            assertThat(m.name()).contains(" ").doesNotContain("-");
            assertThat(m.age()).isPositive();
        });
    }

    @Test
    void offerIsVarietyWeightedAndBounded() {
        StaffPool pool = StaffPool.generate(2L, new StaffMarket(), 10);
        List<StaffMember> offered = pool.offer(EnumSet.allOf(StaffRole.class), 6, new SplitMix64Rng(99L));
        assertThat(offered).hasSize(6);
        // Round-robin across the 5 roles spreads the offer — most roles appear.
        assertThat(offered.stream().map(StaffMember::role).distinct().count()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void offerRespectsAllowedRoles() {
        StaffPool pool = StaffPool.generate(3L, new StaffMarket(), 10);
        List<StaffMember> offered = pool.offer(EnumSet.of(StaffRole.COACH), 6, new SplitMix64Rng(1L));
        assertThat(offered).isNotEmpty().allSatisfy(m -> assertThat(m.role()).isEqualTo(StaffRole.COACH));
    }

    @Test
    void hiringRemovesFromThePoolAndItRestoresRoundTrip() {
        StaffPool pool = StaffPool.generate(4L, new StaffMarket(), 3);
        StaffMember hired = pool.available().get(0);
        pool.remove(hired);
        assertThat(pool.available()).doesNotContain(hired);
        assertThat(StaffPool.restore(pool.available()).available()).isEqualTo(pool.available());
    }

    @Test
    void offerIsDeterministic() {
        StaffPool a = StaffPool.generate(5L, new StaffMarket(), 10);
        StaffPool b = StaffPool.generate(5L, new StaffMarket(), 10);
        assertThat(a.offer(EnumSet.allOf(StaffRole.class), 6, new SplitMix64Rng(7L)))
                .isEqualTo(b.offer(EnumSet.allOf(StaffRole.class), 6, new SplitMix64Rng(7L)));
    }
}
