package com.progolf.sim.economy;

/**
 * The kind of a financial {@link Transaction} (spec: financial-identity, REQ-177/184). Each type carries
 * a {@link Category} that determines which career total it folds into. Earnings increase funds; expenses
 * decrease them; the opening balance seeds funds without counting as earnings or expenses.
 */
public enum TransactionType {
    OPENING_BALANCE(Category.OPENING),
    PRIZE_MONEY(Category.TOURNAMENT_EARNINGS),
    SPONSORSHIP_INCOME(Category.SPONSORSHIP_INCOME),
    SPONSORSHIP_BONUS(Category.SPONSORSHIP_INCOME),
    SPONSORSHIP_SIGNING(Category.SPONSORSHIP_INCOME),
    ENTRY_FEE(Category.EXPENSE),
    TRAVEL(Category.EXPENSE),
    ACCOMMODATION(Category.EXPENSE),
    DISCRETIONARY(Category.EXPENSE);

    /** The career total a transaction contributes to. */
    public enum Category {
        OPENING, TOURNAMENT_EARNINGS, SPONSORSHIP_INCOME, EXPENSE
    }

    private final Category category;

    TransactionType(Category category) {
        this.category = category;
    }

    public Category category() {
        return category;
    }

    /** True for money coming in (prize, sponsorship) — credited positively. */
    public boolean isEarning() {
        return category == Category.TOURNAMENT_EARNINGS || category == Category.SPONSORSHIP_INCOME;
    }

    /** True for money going out (entry, travel, discretionary) — debited. */
    public boolean isExpense() {
        return category == Category.EXPENSE;
    }
}
