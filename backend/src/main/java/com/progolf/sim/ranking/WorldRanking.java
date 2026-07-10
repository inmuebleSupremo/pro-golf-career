package com.progolf.sim.ranking;

import com.progolf.sim.tournament.EventPrestige;
import com.progolf.sim.tournament.Tier;
import com.progolf.sim.tournament.TournamentResult;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The World Ranking engine (REQ-141–150). It records ranking points from completed tournaments into an
 * append-only {@link RankingLedger} and computes the ranking as of any date by decaying those points.
 *
 * <p>Analytical and deterministic: points are a pure function of results, there is no randomness, and the
 * same results plus as-of date always yield the same ranking. It never modifies gameplay.
 */
public final class WorldRanking {

    private final RankingLedger ledger = new RankingLedger();
    private final Set<String> ineligible = new HashSet<>();

    /** The append-only ledger of awards (read access). */
    public RankingLedger ledger() {
        return ledger;
    }

    /**
     * Records ranking points from a completed tournament. Each non-withdrawn finisher is awarded points
     * by finishing position, the tournament tier, and the field strength (bootstrapped from competitors'
     * ranking values as of the event date). Awards are appended; nothing else is mutated.
     */
    public void record(TournamentResult result, Tier tier, LocalDate date, long tournamentId) {
        record(result, tier, EventPrestige.REGULAR, date, tournamentId);
    }

    /**
     * Records ranking points weighted by event prestige (spec: event-prestige). Points scale by
     * {@code prestige.rankingWeight()} on top of tier, position, and field strength, so a major moves the
     * ranking far more than a regular event. Regular prestige reproduces the un-weighted award exactly.
     */
    public void record(TournamentResult result, Tier tier, EventPrestige prestige, LocalDate date, long tournamentId) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(prestige, "prestige");
        Objects.requireNonNull(date, "date");

        double fieldStrength = averageFieldStrength(result, date);
        double factor = RankingPoints.fieldStrengthFactor(fieldStrength);

        for (TournamentResult.Finish finish : result.finishingOrder()) {
            if (finish.withdrawn()) {
                continue;
            }
            double points = RankingPoints.award(tier, prestige, finish.position(), factor);
            if (points > 0) {
                ledger.add(new RankingAward(finish.golfer().player().id(), date, points, tournamentId));
            }
        }
    }

    /** Average ranking value of the field as of {@code date}; bootstraps to the reference when unconverged. */
    private double averageFieldStrength(TournamentResult result, LocalDate date) {
        List<TournamentResult.Finish> finishers = result.finishingOrder().stream()
                .filter(f -> !f.withdrawn())
                .toList();
        if (finishers.isEmpty()) {
            return RankingConstants.REFERENCE_FIELD_STRENGTH;
        }
        double total = 0;
        for (TournamentResult.Finish f : finishers) {
            total += rankingValue(f.golfer().player().id(), date);
        }
        double average = total / finishers.size();
        // Bootstrap: before rankings converge the average is ~0, so fall back to the reference strength.
        return average < 1e-9 ? RankingConstants.REFERENCE_FIELD_STRENGTH : average;
    }

    /** A golfer's ranking value as of {@code asOf}: the sum of their decayed award points within the window. */
    public double rankingValue(String golferId, LocalDate asOf) {
        double value = 0;
        for (RankingAward award : ledger.awardsFor(golferId)) {
            if (award.date().isAfter(asOf)) {
                continue; // awards from the future do not count toward an earlier ranking
            }
            long ageDays = ChronoUnit.DAYS.between(award.date(), asOf);
            value += award.points() * RankingPoints.decay(ageDays);
        }
        return value;
    }

    /** Marks a golfer ineligible (e.g. retired): removed from the active ranking; history is preserved. */
    public void markIneligible(String golferId) {
        ineligible.add(Objects.requireNonNull(golferId, "golferId"));
    }

    public boolean isEligible(String golferId) {
        return !ineligible.contains(golferId);
    }

    /**
     * The World Ranking as of {@code asOf}: eligible golfers ordered by ranking value (descending), with a
     * deterministic tie-break on golfer id so the result is a stable total order. Positions are 1..n.
     */
    public RankingSnapshot rankingAsOf(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        record Scored(String golferId, double value) {
        }
        List<Scored> scored = new ArrayList<>();
        for (String golferId : ledger.golferIds()) {
            if (!isEligible(golferId)) {
                continue;
            }
            double value = rankingValue(golferId, asOf);
            if (value > 0) {
                scored.add(new Scored(golferId, value));
            }
        }
        scored.sort(Comparator.comparingDouble(Scored::value).reversed()
                .thenComparing(Scored::golferId));

        List<RankingStanding> standings = new ArrayList<>(scored.size());
        for (int i = 0; i < scored.size(); i++) {
            standings.add(new RankingStanding(i + 1, scored.get(i).golferId(), scored.get(i).value()));
        }
        return new RankingSnapshot(asOf, standings);
    }
}
