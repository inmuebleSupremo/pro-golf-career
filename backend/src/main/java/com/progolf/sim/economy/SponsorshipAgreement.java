package com.progolf.sim.economy;

import java.util.List;
import java.util.Objects;

/**
 * A commercial relationship between a golfer and a sponsor (spec: sponsorship, REQ-179/180/188),
 * independent of tournament participation. It pays {@code perSeasonPayment} each active season for
 * {@code durationSeasons} starting at {@code startSeason}, plus a one-off {@code signingBonus}, and may
 * carry performance {@link SponsorshipObjective}s that are evaluated each season. Immutable.
 */
public record SponsorshipAgreement(
        String sponsor,
        double perSeasonPayment,
        double signingBonus,
        int startSeason,
        int durationSeasons,
        List<SponsorshipObjective> objectives) {

    public SponsorshipAgreement {
        Objects.requireNonNull(sponsor, "sponsor");
        Objects.requireNonNull(objectives, "objectives");
        if (perSeasonPayment < 0 || signingBonus < 0) {
            throw new IllegalArgumentException("payment and signing bonus must be >= 0");
        }
        if (durationSeasons < 1) {
            throw new IllegalArgumentException("durationSeasons must be >= 1: " + durationSeasons);
        }
        objectives = List.copyOf(objectives);
    }

    /** The last season (inclusive) this agreement pays out. */
    public int lastActiveSeason() {
        return startSeason + durationSeasons - 1;
    }

    /** Whether the agreement is active (paying) in the given season. */
    public boolean isActiveIn(int season) {
        return season >= startSeason && season <= lastActiveSeason();
    }

    /**
     * Evaluates the agreement's objectives against a season's performance (REQ-180): sums the rewards of
     * met objectives and reports whether enough were met to be renewable.
     */
    public AgreementReview evaluate(PerformanceSnapshot snapshot) {
        double bonus = 0.0;
        int met = 0;
        for (SponsorshipObjective o : objectives) {
            if (o.isMet(snapshot)) {
                met++;
                bonus += o.reward();
            }
        }
        int total = objectives.size();
        boolean renewable = total == 0 || (double) met / total >= EconomyConstants.RENEWAL_MET_FRACTION;
        return new AgreementReview(bonus, met, total, renewable);
    }
}
