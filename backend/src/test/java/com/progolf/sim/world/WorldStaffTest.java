package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.economy.Transaction;
import com.progolf.sim.economy.TransactionType;
import com.progolf.sim.staff.SupportTeam;
import java.util.List;
import org.junit.jupiter.api.Test;

/** world-progression (modified): golfers acquire staff, salaries flow through the Economy; reproducible. */
class WorldStaffTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void everyGolferStartsWithAnEmptyTeam() {
        World world = World.create(11L, small());
        for (String id : world.activeGolferIds()) {
            SupportTeam team = world.supportTeamOf(id);
            assertThat(team).isNotNull();
            assertThat(team.size()).isZero();
        }
    }

    @Test
    void golfersAcquireStaffPaidThroughTheEconomy() {
        World world = World.create(22L, small());
        world.advanceSeason();
        world.advanceSeason();
        world.advanceSeason();

        boolean anyStaff = world.activeGolferIds().stream()
                .map(world::supportTeamOf)
                .anyMatch(t -> t.size() > 0);
        assertThat(anyStaff).as("some golfer employs staff").isTrue();

        boolean anyStaffCharge = world.activeGolferIds().stream()
                .map(world::financialAccountOf)
                .flatMap(a -> a.ledger().stream())
                .map(Transaction::type)
                .anyMatch(t -> t == TransactionType.STAFF_HIRING || t == TransactionType.STAFF_SALARY);
        assertThat(anyStaffCharge).as("staff costs are charged through the account").isTrue();
    }

    @Test
    void staffRelationshipHistoryIsPreserved() {
        World world = World.create(33L, small());
        world.advanceSeason();
        world.advanceSeason();
        world.advanceSeason();

        boolean anyHistory = world.activeGolferIds().stream()
                .map(world::supportTeamOf)
                .anyMatch(t -> !t.history().isEmpty());
        assertThat(anyHistory).isTrue();
    }

    @Test
    void staffStateIsReproducibleFromTheSeed() {
        World a = World.create(44L, small());
        World b = World.create(44L, small());
        a.advanceSeason();
        a.advanceSeason();
        b.advanceSeason();
        b.advanceSeason();

        assertThat(staffSummary(a)).isEqualTo(staffSummary(b));
    }

    private static List<String> staffSummary(World w) {
        return w.activeGolferIds().stream().sorted()
                .map(id -> {
                    SupportTeam t = w.supportTeamOf(id);
                    List<String> roles = t.members().stream().map(m -> m.role().name()).sorted().toList();
                    return id + "|" + roles + "|" + t.history().size();
                })
                .toList();
    }
}
