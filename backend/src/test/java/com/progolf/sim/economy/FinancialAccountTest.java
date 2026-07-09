package com.progolf.sim.economy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** financial-identity spec: identity, auditable ledger, economic integrity, milestones, history. */
class FinancialAccountTest {

    private static final LocalDate DAY = LocalDate.of(2000, 1, 1);

    private static FinancialAccount account() {
        return new FinancialAccount("g1", 50_000.0, DAY);
    }

    @Test
    void opensWithStartingFundsAndAnOpeningTransaction() {
        FinancialAccount a = account();
        assertThat(a.availableFunds()).isEqualTo(50_000.0);
        assertThat(a.careerEarnings()).isZero();
        assertThat(a.careerExpenses()).isZero();
        assertThat(a.ledger()).singleElement()
                .satisfies(t -> assertThat(t.type()).isEqualTo(TransactionType.OPENING_BALANCE));
    }

    @Test
    void awardCreditsEarningsAndRecords() {
        FinancialAccount a = account();
        a.award(TransactionType.PRIZE_MONEY, 200_000.0, DAY, "Prize");
        assertThat(a.availableFunds()).isEqualTo(250_000.0);
        assertThat(a.tournamentEarnings()).isEqualTo(200_000.0);
        assertThat(a.careerEarnings()).isEqualTo(200_000.0);
        assertThat(a.hasMilestone(FinancialMilestone.FIRST_PRIZE)).isTrue();
        assertThat(a.ledger()).hasSize(2);
    }

    @Test
    void chargeIsMandatoryAndMayGoNegative() {
        FinancialAccount a = new FinancialAccount("g", 10_000.0, DAY);
        a.charge(TransactionType.ENTRY_FEE, 40_000.0, DAY, "Entry");
        assertThat(a.availableFunds()).isEqualTo(-30_000.0); // defined negative-balance rule (debt)
        assertThat(a.careerExpenses()).isEqualTo(40_000.0);
    }

    @Test
    void discretionarySpendIsDeclinedWhenUnaffordable() {
        FinancialAccount a = new FinancialAccount("g", 10_000.0, DAY);
        boolean applied = a.spend(TransactionType.DISCRETIONARY, 40_000.0, DAY, "Gadget");
        assertThat(applied).isFalse();
        assertThat(a.availableFunds()).isEqualTo(10_000.0); // unchanged
        assertThat(a.ledger()).hasSize(1); // nothing recorded beyond opening balance

        assertThat(a.spend(TransactionType.DISCRETIONARY, 4_000.0, DAY, "Small")).isTrue();
        assertThat(a.availableFunds()).isEqualTo(6_000.0);
    }

    @Test
    void categoryTotalsAlwaysEqualTheLedgerFoldedByType() {
        FinancialAccount a = account();
        a.award(TransactionType.PRIZE_MONEY, 300_000.0, DAY, "Prize");
        a.award(TransactionType.SPONSORSHIP_INCOME, 80_000.0, DAY, "Sponsor");
        a.award(TransactionType.SPONSORSHIP_BONUS, 20_000.0, DAY, "Bonus");
        a.charge(TransactionType.ENTRY_FEE, 40_000.0, DAY, "Entry");
        a.charge(TransactionType.TRAVEL, 12_000.0, DAY, "Travel");

        double funds = a.ledger().stream().mapToDouble(Transaction::amount).sum();
        double tournament = a.ledger().stream()
                .filter(t -> t.type().category() == TransactionType.Category.TOURNAMENT_EARNINGS)
                .mapToDouble(Transaction::amount).sum();
        double sponsorship = a.ledger().stream()
                .filter(t -> t.type().category() == TransactionType.Category.SPONSORSHIP_INCOME)
                .mapToDouble(Transaction::amount).sum();
        double expenses = -a.ledger().stream()
                .filter(t -> t.type().category() == TransactionType.Category.EXPENSE)
                .mapToDouble(Transaction::amount).sum();

        assertThat(a.availableFunds()).isEqualTo(funds);
        assertThat(a.tournamentEarnings()).isEqualTo(tournament);
        assertThat(a.sponsorshipIncome()).isEqualTo(sponsorship);
        assertThat(a.careerExpenses()).isEqualTo(expenses);
    }

    @Test
    void earningsMilestonesFireOnceWhenCrossed() {
        FinancialAccount a = account();
        assertThat(a.hasMilestone(FinancialMilestone.SIX_FIGURE_EARNINGS)).isFalse();
        a.award(TransactionType.PRIZE_MONEY, 150_000.0, DAY, "Prize");
        assertThat(a.hasMilestone(FinancialMilestone.SIX_FIGURE_EARNINGS)).isTrue();
        assertThat(a.hasMilestone(FinancialMilestone.MILLION_EARNINGS)).isFalse();
        a.award(TransactionType.PRIZE_MONEY, 1_000_000.0, DAY, "Prize");
        assertThat(a.hasMilestone(FinancialMilestone.MILLION_EARNINGS)).isTrue();
    }

    @Test
    void rejectsBadAmountsAndTypeMismatches() {
        FinancialAccount a = account();
        assertThatThrownBy(() -> a.award(TransactionType.PRIZE_MONEY, -1.0, DAY, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> a.award(TransactionType.ENTRY_FEE, 100.0, DAY, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> a.charge(TransactionType.PRIZE_MONEY, 100.0, DAY, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
