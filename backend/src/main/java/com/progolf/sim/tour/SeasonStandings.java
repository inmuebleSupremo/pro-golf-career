package com.progolf.sim.tour;

import java.util.Comparator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The resetting Season Standings ledger (REQ-130/136): cumulative season points per golfer, awarded from
 * tournament finishing positions and reset each season. Distinct from the rolling World Ranking.
 */
public final class SeasonStandings {

    private final Map<String, Integer> points = new LinkedHashMap<>();

    /** Points for a 1-based finishing position: a decreasing curve, floored at zero. */
    public static int pointsFor(int position) {
        int raw = TourConstants.SEASON_POINTS_BASE - (position - 1) * TourConstants.SEASON_POINTS_STEP;
        return Math.max(0, raw);
    }

    /** Awards season points to a golfer for a finishing position. */
    public void award(String golferId, int position) {
        points.merge(golferId, pointsFor(position), Integer::sum);
    }

    /** Current season points for a golfer (0 if none). */
    public int pointsOf(String golferId) {
        return points.getOrDefault(golferId, 0);
    }

    /** Clears all season points (start of a new season). */
    public void reset() {
        points.clear();
    }

    /**
     * The given members ordered by season points descending, with a deterministic golfer-id tie-break so
     * the ordering is a stable total order.
     */
    public List<String> ranked(Collection<String> members) {
        return members.stream()
                .sorted(Comparator.comparingInt((String id) -> pointsOf(id)).reversed()
                        .thenComparing(Comparator.naturalOrder()))
                .toList();
    }
}
