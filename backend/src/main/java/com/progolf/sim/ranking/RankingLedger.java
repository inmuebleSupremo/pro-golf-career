package com.progolf.sim.ranking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The append-only source of truth for the World Ranking: a list of dated {@link RankingAward}s
 * (REQ-143/144). Awards are never removed or modified; standings are derived from them.
 *
 * <p>Awards are also indexed by golfer so a golfer's ranking value can be computed from just their own
 * awards rather than scanning the whole ledger. Insertion order is preserved for determinism.
 */
public final class RankingLedger {

    private final List<RankingAward> awards = new ArrayList<>();
    private final Map<String, List<RankingAward>> byGolfer = new LinkedHashMap<>();

    /** Appends an award. */
    public void add(RankingAward award) {
        Objects.requireNonNull(award, "award");
        awards.add(award);
        byGolfer.computeIfAbsent(award.golferId(), k -> new ArrayList<>()).add(award);
    }

    /** An immutable view of all awards. */
    public List<RankingAward> awards() {
        return List.copyOf(awards);
    }

    /** Restores the ledger by replaying captured awards in order (rebuilds the by-golfer index). */
    void restore(List<RankingAward> captured) {
        for (RankingAward award : captured) {
            add(award);
        }
    }

    /** This golfer's awards (empty if none), in insertion order. */
    public List<RankingAward> awardsFor(String golferId) {
        return byGolfer.getOrDefault(golferId, List.of());
    }

    /** Distinct golfer ids that have earned at least one award, in first-seen order. */
    public List<String> golferIds() {
        return new ArrayList<>(byGolfer.keySet());
    }
}
