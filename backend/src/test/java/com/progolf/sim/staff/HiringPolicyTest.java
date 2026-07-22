package com.progolf.sim.staff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** staff-influence spec: deterministic hiring gated by career stage and affordability (REQ-195/199). */
class HiringPolicyTest {

    private static StaffMember member(StaffRole role, double salary) {
        return new StaffMember(role, role.name(), 45, "USA",
                StaffPersonality.ANALYST, 0.6, salary * 0.5, salary);
    }

    @Test
    void targetTeamSizeGrowsWithCareerStage() {
        assertThat(HiringPolicy.targetTeamSize(20)).isEqualTo(StaffConstants.TARGET_TEAM_DEVELOPMENT);
        assertThat(HiringPolicy.targetTeamSize(28)).isEqualTo(StaffConstants.TARGET_TEAM_PRIME);
        assertThat(HiringPolicy.targetTeamSize(40)).isEqualTo(StaffConstants.TARGET_TEAM_LATE);
    }

    @Test
    void fillsRolesInPriorityOrderUpToTheStageTarget() {
        SupportTeam team = new SupportTeam();
        // Prime golfer, target 3.
        assertThat(HiringPolicy.chooseRole(team, 28)).contains(StaffRole.COACH);
        team.hire(member(StaffRole.COACH, 120_000), 1);
        assertThat(HiringPolicy.chooseRole(team, 28)).contains(StaffRole.PHYSIOTHERAPIST);
        team.hire(member(StaffRole.PHYSIOTHERAPIST, 70_000), 1);
        assertThat(HiringPolicy.chooseRole(team, 28)).contains(StaffRole.FITNESS_COACH);
        team.hire(member(StaffRole.FITNESS_COACH, 70_000), 1);
        assertThat(HiringPolicy.chooseRole(team, 28)).isEmpty(); // at target
    }

    @Test
    void developingGolferPrioritisesASingleCoach() {
        SupportTeam team = new SupportTeam();
        assertThat(HiringPolicy.chooseRole(team, 20)).contains(StaffRole.COACH);
        team.hire(member(StaffRole.COACH, 120_000), 1);
        assertThat(HiringPolicy.chooseRole(team, 20)).isEmpty(); // target 1 reached
    }

    @Test
    void affordabilityRequiresHiringCostPlusSalary() {
        StaffMember candidate = member(StaffRole.COACH, 120_000); // hiring 60k + salary 120k = 180k
        assertThat(HiringPolicy.canAfford(200_000, candidate)).isTrue();
        assertThat(HiringPolicy.canAfford(150_000, candidate)).isFalse();
    }

    @Test
    void choiceIsDeterministic() {
        SupportTeam team = new SupportTeam();
        Optional<StaffRole> a = HiringPolicy.chooseRole(team, 28);
        Optional<StaffRole> b = HiringPolicy.chooseRole(team, 28);
        assertThat(a).isEqualTo(b);
    }
}
