package com.progolf.sim.ranking;

import java.util.List;
import java.util.Optional;

/**
 * Historical derivations over an ordered series of {@link RankingSnapshot}s (REQ-144/145/149). All
 * functions are pure reads; snapshots are the immutable record, and these fold them into career facts.
 */
public final class RankingHistory {

    private RankingHistory() {
    }

    /** Best (numerically lowest) position the golfer ever held across the snapshots, if any. */
    public static Optional<Integer> careerHighPosition(String golferId, List<RankingSnapshot> snapshots) {
        return snapshots.stream()
                .map(s -> s.positionOf(golferId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Integer::compareTo);
    }

    /** Number of snapshots in which the golfer stood at World #1 (a proxy for weeks until the calendar exists). */
    public static long weeksAtNumberOne(String golferId, List<RankingSnapshot> snapshots) {
        return snapshots.stream()
                .filter(s -> s.positionOf(golferId).map(p -> p == 1).orElse(false))
                .count();
    }

    /** The golfer's position in the final snapshot (e.g. season-ending), if present. */
    public static Optional<Integer> positionIn(String golferId, RankingSnapshot snapshot) {
        return snapshot.positionOf(golferId);
    }

    /** Movement between two snapshots, if the golfer appears in both. */
    public static Optional<RankingMovement> movementBetween(String golferId, RankingSnapshot from, RankingSnapshot to) {
        Optional<Integer> a = from.positionOf(golferId);
        Optional<Integer> b = to.positionOf(golferId);
        if (a.isPresent() && b.isPresent()) {
            return Optional.of(new RankingMovement(a.get(), b.get()));
        }
        return Optional.empty();
    }

    /** The most prestigious milestone the golfer ever reached across the snapshots (REQ-149). */
    public static Optional<RankingMilestone> bestMilestoneReached(String golferId, List<RankingSnapshot> snapshots) {
        return careerHighPosition(golferId, snapshots).map(RankingMilestone::forPosition);
    }
}
