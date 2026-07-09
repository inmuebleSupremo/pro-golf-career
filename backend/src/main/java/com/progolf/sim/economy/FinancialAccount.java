package com.progolf.sim.economy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A Professional Golfer's financial identity (spec: financial-identity, REQ-177): available funds and the
 * career totals (career/tournament/sponsorship earnings, career expenses), backed by an append-only,
 * auditable ledger that is the single source of truth. Every mutation flows through one guarded path that
 * updates funds and the relevant total and appends a {@link Transaction}, so the totals always equal the
 * ledger folded by category and the financial history is fully reconstructable (REQ-184/189).
 *
 * <p>Economic integrity (REQ-184): {@link #award} credits earnings; {@link #charge} applies a mandatory
 * obligation that may drive funds negative (the defined negative-balance rule); {@link #spend} is
 * discretionary and is declined when funds are insufficient. The Economy never modifies attributes,
 * outcomes, or rankings (REQ-185) — this class holds only money.
 */
public final class FinancialAccount {

    private final String golferId;
    private double availableFunds;
    private double tournamentEarnings;
    private double sponsorshipIncome;
    private double careerExpenses;
    private int sequence;

    private final List<Transaction> ledger = new ArrayList<>();
    private final Map<FinancialMilestone, LocalDate> milestones = new LinkedHashMap<>();
    private final List<SponsorshipAgreement> activeAgreements = new ArrayList<>();
    private final List<SponsorshipAgreement> concludedAgreements = new ArrayList<>();
    private double commercialMomentum;

    /** Opens an account with the default starting funds on {@code openedOn}. */
    public FinancialAccount(String golferId, LocalDate openedOn) {
        this(golferId, EconomyConstants.STARTING_FUNDS, openedOn);
    }

    public FinancialAccount(String golferId, double startingFunds, LocalDate openedOn) {
        this.golferId = Objects.requireNonNull(golferId, "golferId");
        Objects.requireNonNull(openedOn, "openedOn");
        if (startingFunds < 0) {
            throw new IllegalArgumentException("startingFunds must be >= 0: " + startingFunds);
        }
        post(TransactionType.OPENING_BALANCE, startingFunds, openedOn, "Opening balance");
    }

    // --- Money in / out (the only mutators of funds) ---

    /** Credits earnings (prize money or sponsorship income) and records the transaction. */
    public void award(TransactionType type, double amount, LocalDate date, String description) {
        requireAmount(amount);
        if (!type.isEarning()) {
            throw new IllegalArgumentException("award requires an earning type: " + type);
        }
        post(type, amount, date, description);
        if (type == TransactionType.PRIZE_MONEY) {
            fire(FinancialMilestone.FIRST_PRIZE, date);
        }
    }

    /**
     * Applies a mandatory expense (entry, travel). It is always recorded and MAY drive funds negative —
     * the defined negative-balance rule (debt), which future earnings recover (REQ-184).
     */
    public void charge(TransactionType type, double amount, LocalDate date, String description) {
        requireAmount(amount);
        if (!type.isExpense()) {
            throw new IllegalArgumentException("charge requires an expense type: " + type);
        }
        post(type, -amount, date, description);
    }

    /**
     * Attempts a discretionary spend. Declined (no record, balance unchanged) when it exceeds available
     * funds — "funds cannot be spent unless available" (REQ-184). Returns whether it was applied.
     */
    public boolean spend(TransactionType type, double amount, LocalDate date, String description) {
        requireAmount(amount);
        if (!type.isExpense()) {
            throw new IllegalArgumentException("spend requires an expense type: " + type);
        }
        if (amount > availableFunds) {
            return false;
        }
        post(type, -amount, date, description);
        return true;
    }

    private void post(TransactionType type, double signedAmount, LocalDate date, String description) {
        availableFunds += signedAmount;
        switch (type.category()) {
            case TOURNAMENT_EARNINGS -> tournamentEarnings += signedAmount;
            case SPONSORSHIP_INCOME -> sponsorshipIncome += signedAmount;
            case EXPENSE -> careerExpenses += -signedAmount; // signedAmount is negative for expenses
            case OPENING -> { /* seeds funds only; not earnings or expenses */ }
        }
        ledger.add(new Transaction(++sequence, date, type, signedAmount, availableFunds, description));
        checkEarningsMilestones(date);
    }

    // --- Sponsorship ---

    /** Signs an agreement: records it as active and credits its signing bonus as sponsorship income. */
    public void signSponsorship(SponsorshipAgreement agreement, LocalDate date) {
        Objects.requireNonNull(agreement, "agreement");
        activeAgreements.add(agreement);
        fire(FinancialMilestone.FIRST_SPONSOR, date);
        if (agreement.signingBonus() > 0) {
            award(TransactionType.SPONSORSHIP_SIGNING, agreement.signingBonus(), date, "Signing bonus: " + agreement.sponsor());
        }
    }

    /** The agreements active (paying) in the given season. */
    public List<SponsorshipAgreement> activeAgreements(int season) {
        List<SponsorshipAgreement> active = new ArrayList<>();
        for (SponsorshipAgreement a : activeAgreements) {
            if (a.isActiveIn(season)) {
                active.add(a);
            }
        }
        return active;
    }

    /** How many agreements are active in the given season (for the concurrent-agreements cap). */
    public int activeAgreementCount(int season) {
        int count = 0;
        for (SponsorshipAgreement a : activeAgreements) {
            if (a.isActiveIn(season)) {
                count++;
            }
        }
        return count;
    }

    /** Moves agreements whose term has ended (by the given season) into concluded history (REQ-188). */
    public void concludeExpiredAgreements(int season) {
        activeAgreements.removeIf(a -> {
            if (a.lastActiveSeason() <= season) {
                concludedAgreements.add(a);
                return true;
            }
            return false;
        });
    }

    /**
     * Nudges the golfer's commercial standing by an objective outcome ([0,1] met fraction): meeting
     * objectives lifts future commercial opportunity, missing them lowers it (REQ-180). Bounded.
     */
    public void recordObjectiveOutcome(double metFraction) {
        double delta = (metFraction - 0.5) * 2.0 * EconomyConstants.MOMENTUM_STEP;
        commercialMomentum = clamp(commercialMomentum + delta,
                -EconomyConstants.MOMENTUM_CAP, EconomyConstants.MOMENTUM_CAP);
    }

    // --- Milestones ---

    private void checkEarningsMilestones(LocalDate date) {
        double earned = careerEarnings();
        if (earned >= EconomyConstants.SIX_FIGURE_EARNINGS) {
            fire(FinancialMilestone.SIX_FIGURE_EARNINGS, date);
        }
        if (earned >= EconomyConstants.MILLION_EARNINGS) {
            fire(FinancialMilestone.MILLION_EARNINGS, date);
        }
        if (earned >= EconomyConstants.MULTI_MILLION_EARNINGS) {
            fire(FinancialMilestone.MULTI_MILLION_EARNINGS, date);
        }
    }

    private void fire(FinancialMilestone milestone, LocalDate date) {
        milestones.putIfAbsent(milestone, date);
    }

    // --- Read-only accessors ---

    public String golferId() {
        return golferId;
    }

    public double availableFunds() {
        return availableFunds;
    }

    /** Total gross income over the career: tournament earnings plus sponsorship income. */
    public double careerEarnings() {
        return tournamentEarnings + sponsorshipIncome;
    }

    public double tournamentEarnings() {
        return tournamentEarnings;
    }

    public double sponsorshipIncome() {
        return sponsorshipIncome;
    }

    public double careerExpenses() {
        return careerExpenses;
    }

    public double commercialMomentum() {
        return commercialMomentum;
    }

    public List<Transaction> ledger() {
        return List.copyOf(ledger);
    }

    public Map<FinancialMilestone, LocalDate> milestones() {
        return Map.copyOf(milestones);
    }

    public boolean hasMilestone(FinancialMilestone milestone) {
        return milestones.containsKey(milestone);
    }

    public List<SponsorshipAgreement> concludedAgreements() {
        return List.copyOf(concludedAgreements);
    }

    /** All agreements a golfer has ever held (active plus concluded) — the sponsorship history. */
    public List<SponsorshipAgreement> sponsorshipHistory() {
        List<SponsorshipAgreement> all = new ArrayList<>(concludedAgreements);
        all.addAll(activeAgreements);
        return List.copyOf(all);
    }

    private static void requireAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("amount must be finite and >= 0: " + amount);
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
