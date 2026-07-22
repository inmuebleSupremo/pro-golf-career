package com.progolf.sim.staff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** support-team / staff-relationships spec: composition, roles, history, effects. */
class SupportTeamTest {

    private static StaffMember member(StaffRole role, double quality, double salary) {
        return new StaffMember(role, role.name() + "-x", 45, "USA",
                StaffPersonality.ANALYST, quality, salary * 0.5, salary);
    }

    @Test
    void startsEmpty() {
        SupportTeam team = new SupportTeam();
        assertThat(team.size()).isZero();
        assertThat(team.members()).isEmpty();
        assertThat(team.history()).isEmpty();
        assertThat(team.effects()).isEqualTo(StaffEffects.NONE);
    }

    @Test
    void hiringAddsAMemberAndRecordsAnOngoingRelationship() {
        SupportTeam team = new SupportTeam();
        team.hire(member(StaffRole.COACH, 0.8, 120_000), 1);
        assertThat(team.has(StaffRole.COACH)).isTrue();
        assertThat(team.size()).isEqualTo(1);
        assertThat(team.history()).singleElement().satisfies(r -> {
            assertThat(r.role()).isEqualTo(StaffRole.COACH);
            assertThat(r.isActive()).isTrue();
            assertThat(r.startSeason()).isEqualTo(1);
        });
    }

    @Test
    void releasingKeepsHistoryButClearsTheCurrentRole() {
        SupportTeam team = new SupportTeam();
        team.hire(member(StaffRole.CADDIE, 0.6, 80_000), 1);
        team.release(StaffRole.CADDIE, 3);
        assertThat(team.has(StaffRole.CADDIE)).isFalse();
        assertThat(team.history()).singleElement().satisfies(r -> {
            assertThat(r.isActive()).isFalse();
            assertThat(r.endSeason()).hasValue(3);
            assertThat(r.seasonsSpanned(3)).isEqualTo(3);
        });
    }

    @Test
    void replacingAMemberPreservesTheFormerRelationship() {
        SupportTeam team = new SupportTeam();
        team.hire(member(StaffRole.COACH, 0.5, 120_000), 1);
        team.hire(member(StaffRole.COACH, 0.9, 130_000), 4); // replacement
        assertThat(team.size()).isEqualTo(1);
        assertThat(team.history()).hasSize(2);
        assertThat(team.history().get(0).endSeason()).hasValue(4); // former closed
        assertThat(team.history().get(1).isActive()).isTrue();     // new ongoing
        assertThat(team.members().get(0).quality()).isEqualTo(0.9);
    }

    @Test
    void effectsAggregateByRoleAndScaleWithQuality() {
        SupportTeam team = new SupportTeam();
        team.hire(member(StaffRole.COACH, 0.8, 120_000), 1);
        team.hire(member(StaffRole.PHYSIOTHERAPIST, 0.6, 70_000), 1);

        StaffEffects effects = team.effects();
        assertThat(effects.developmentBonus())
                .isEqualTo(StaffConstants.COACH_DEVELOPMENT_PER_QUALITY * 0.8);
        assertThat(effects.recoveryBonus())
                .isEqualTo(StaffConstants.PHYSIO_RECOVERY_PER_QUALITY * 0.6);
    }

    @Test
    void mostExpensiveRoleIsFoundForReleaseDecisions() {
        SupportTeam team = new SupportTeam();
        team.hire(member(StaffRole.SPORTS_PSYCHOLOGIST, 0.6, 60_000), 1);
        team.hire(member(StaffRole.COACH, 0.7, 120_000), 1);
        assertThat(team.mostExpensiveRole()).isEqualTo(StaffRole.COACH);
    }

    @Test
    void rolesCarryDefinedResponsibilities() {
        assertThat(StaffRole.COACH.responsibility()).isEqualTo("Long-term player development");
        assertThat(StaffRole.PHYSIOTHERAPIST.responsibility()).isEqualTo("Recovery support");
    }
}
