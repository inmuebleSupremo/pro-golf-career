package com.progolf.sim.ranking;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable capture of the World Ranking as of a date (REQ-148): the ordered standings. Later ranking
 * changes never alter a snapshot.
 */
public record RankingSnapshot(LocalDate asOf, List<RankingStanding> standings) {

    public RankingSnapshot {
        Objects.requireNonNull(asOf, "asOf");
        standings = List.copyOf(standings);
    }

    /** The golfer's position, or empty if they are not in this ranking. */
    public Optional<Integer> positionOf(String golferId) {
        return standings.stream()
                .filter(s -> s.golferId().equals(golferId))
                .map(RankingStanding::position)
                .findFirst();
    }

    /** The World #1, if the ranking is non-empty. */
    public Optional<RankingStanding> leader() {
        return standings.isEmpty() ? Optional.empty() : Optional.of(standings.get(0));
    }

    public int size() {
        return standings.size();
    }
}
