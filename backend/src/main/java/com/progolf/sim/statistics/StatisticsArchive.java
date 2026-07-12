package com.progolf.sim.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The authoritative historical archive of competitive information (spec: competitive-statistics /
 * records-archive / historical-queries, REQ-251..262). A pure observer: the World feeds it real outcomes
 * via {@link #observeEvent}, and it accumulates per-season and career statistics, registers champions, and
 * updates the {@link RecordBook}. It answers queries and comparisons but mutates nothing outside itself —
 * every entry corresponds to an observed gameplay event, and nothing is ever removed.
 */
public final class StatisticsArchive {

    private final Map<String, StatLine> career = new HashMap<>();
    private final Map<String, Map<Integer, StatLine>> seasonal = new HashMap<>();
    private final List<Championship> championships = new ArrayList<>();
    private final RecordBook records = new RecordBook();
    private final Map<String, Integer> consecutiveCuts = new HashMap<>();
    private final Map<String, Set<Integer>> seasonsAppeared = new HashMap<>();
    private final Map<String, Integer> majorWins = new HashMap<>();

    /** An immutable capture of the statistics archive (spec: world-snapshot). Sub-types are records. */
    public record Snapshot(Map<String, StatLine> career, Map<String, Map<Integer, StatLine>> seasonal,
                           List<Championship> championships, RecordBook.Snapshot records,
                           Map<String, Integer> consecutiveCuts, Map<String, Set<Integer>> seasonsAppeared,
                           Map<String, Integer> majorWins) {
    }

    public Snapshot snapshot() {
        Map<String, Map<Integer, StatLine>> seasonalCopy = new HashMap<>();
        seasonal.forEach((k, v) -> seasonalCopy.put(k, new HashMap<>(v)));
        Map<String, Set<Integer>> seasonsCopy = new HashMap<>();
        seasonsAppeared.forEach((k, v) -> seasonsCopy.put(k, new HashSet<>(v)));
        return new Snapshot(new HashMap<>(career), seasonalCopy, new ArrayList<>(championships),
                records.snapshot(), new HashMap<>(consecutiveCuts), seasonsCopy, new HashMap<>(majorWins));
    }

    public static StatisticsArchive restore(Snapshot s) {
        StatisticsArchive a = new StatisticsArchive();
        a.career.putAll(s.career());
        s.seasonal().forEach((k, v) -> a.seasonal.put(k, new HashMap<>(v)));
        a.championships.addAll(s.championships());
        a.records.restoreFrom(s.records()); // RecordBook is a final field: replace its contents in place
        a.consecutiveCuts.putAll(s.consecutiveCuts());
        s.seasonsAppeared().forEach((k, v) -> a.seasonsAppeared.put(k, new HashSet<>(v)));
        a.majorWins.putAll(s.majorWins());
        return a;
    }

    /**
     * Observes one golfer's outcome in a tournament (Regular prestige), accumulating statistics,
     * registering a champion when this golfer won, and updating records — all from real gameplay data.
     */
    public void observeEvent(EventOutcome outcome, String tournamentName, String tier, boolean isWinner) {
        observeEvent(outcome, tournamentName, tier, "REGULAR", isWinner);
    }

    /**
     * Observes one golfer's outcome, recording the event's prestige (spec: event-prestige): a major victory
     * is preserved distinctly and drives the most-major-championships record. Otherwise identical.
     */
    public void observeEvent(EventOutcome outcome, String tournamentName, String tier, String prestige,
                             boolean isWinner) {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(prestige, "prestige");
        String id = outcome.golferId();

        // Accumulate career and per-season statistics.
        career.merge(id, StatLine.of(outcome), StatLine::plus);
        seasonal.computeIfAbsent(id, k -> new HashMap<>())
                .merge(outcome.season(), StatLine.of(outcome), StatLine::plus);

        // Register the champion (REQ-256), preserving the event's prestige so majors are distinguishable.
        if (isWinner) {
            championships.add(new Championship(outcome.season(), tournamentName, tier, prestige, id));
            if (Championship.MAJOR_PRESTIGE.equals(prestige) && outcome.counts()) {
                int majors = majorWins.merge(id, 1, Integer::sum);
                records.challenge(RecordType.MOST_MAJOR_WINS, id, majors, outcome.season());
            }
        }

        // Career longevity (distinct seasons appeared) — a record even for retirees who played this season.
        Set<Integer> seasons = seasonsAppeared.computeIfAbsent(id, k -> new HashSet<>());
        if (seasons.add(outcome.season())) {
            records.challenge(RecordType.LONGEST_CAREER, id, seasons.size(), outcome.season());
        }

        // Records that emerge from completed play only.
        if (outcome.counts()) {
            records.challenge(RecordType.LOWEST_TOURNAMENT_SCORE, id, outcome.scoreVsPar(), outcome.season());
            if (isWinner) {
                records.challenge(RecordType.MOST_CAREER_WINS, id, career.get(id).wins(), outcome.season());
            }
            int streak = outcome.madeCut() ? consecutiveCuts.getOrDefault(id, 0) + 1 : 0;
            consecutiveCuts.put(id, streak);
            if (streak > 0) {
                records.challenge(RecordType.MOST_CONSECUTIVE_CUTS, id, streak, outcome.season());
            }
        }
    }

    // --- Queries (REQ-258) ---

    /** A golfer's complete career statistics, available permanently including after retirement (REQ-253). */
    public StatLine careerStatistics(String golferId) {
        return career.getOrDefault(golferId, StatLine.empty());
    }

    /** A golfer's statistics for a specific season, preserved and never overwritten (REQ-252). */
    public StatLine seasonStatistics(String golferId, int season) {
        return seasonal.getOrDefault(golferId, Map.of()).getOrDefault(season, StatLine.empty());
    }

    /** The champions of a given season. */
    public List<Championship> championsOfSeason(int season) {
        List<Championship> out = new ArrayList<>();
        for (Championship c : championships) {
            if (c.season() == season) {
                out.add(c);
            }
        }
        return out;
    }

    /** Every championship a golfer has won. */
    public List<Championship> championshipsOf(String golferId) {
        List<Championship> out = new ArrayList<>();
        for (Championship c : championships) {
            if (c.winnerId().equals(golferId)) {
                out.add(c);
            }
        }
        return out;
    }

    /** All recorded championships, oldest first. */
    public List<Championship> allChampionships() {
        return Collections.unmodifiableList(new ArrayList<>(championships));
    }

    /** Every major a golfer has won (spec: event-prestige). */
    public List<Championship> majorsWonBy(String golferId) {
        List<Championship> out = new ArrayList<>();
        for (Championship c : championships) {
            if (c.isMajor() && c.winnerId().equals(golferId)) {
                out.add(c);
            }
        }
        return out;
    }

    /** A snapshot of every record's current holder. */
    public Map<RecordType, RecordHolder> records() {
        return records.currentRecords();
    }

    /** The full progression of a record (each successive holder), oldest first (REQ-255). */
    public List<RecordHolder> recordProgression(RecordType type) {
        return records.progression(type);
    }

    /** A read-only comparison of two golfers' careers (REQ-259). */
    public CareerComparison compareCareers(String golferA, String golferB) {
        return new CareerComparison(golferA, careerStatistics(golferA), golferB, careerStatistics(golferB));
    }
}
