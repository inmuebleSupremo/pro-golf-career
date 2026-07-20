package com.progolf.sim.tour;

/**
 * The single tunables surface for the Tour domain (mirrors {@code SimConstants}). Movement counts and
 * the season-points curve are placeholder calibration — structural behaviour (top rises, bottom falls,
 * deterministic) is what matters now; magnitudes are tuned with the World/economy later.
 */
public final class TourConstants {

    private TourConstants() {
    }

    /** Number of bottom-standings golfers relegated from a tier at season end — the ladder's churn rate. */
    public static final int RELEGATE_COUNT = 10;

    /**
     * Never move more than this fraction of a tier's members in one review, so a small tier cannot be
     * emptied or hollowed out by a single season.
     */
    public static final double MAX_MOVE_FRACTION = 0.34;

    // --- Tier sizes (fraction of total membership; the Development tour holds the remainder) ---
    /**
     * The Pro tour carries a fixed number of cards, as a share of the golfers in the system — the top tour
     * is selective, so a card is worth earning. It is refilled to this size every season from the Development
     * tour, so retirement thinning it pulls the best developmental golfers up behind it instead of leaving it
     * hollow. The Development tour is the reservoir and has no target — it holds everyone else.
     */
    public static final double PRO_FRACTION = 0.16;

    /** The number of cards a tier carries, given the total membership. Zero for the Development tour. */
    public static int targetSize(TourTier tier, int totalMembers) {
        double fraction = switch (tier) {
            case PRO -> PRO_FRACTION;
            case DEVELOPMENT -> 0.0;
        };
        return (int) Math.round(totalMembers * fraction);
    }

    // --- Season points curve: points = max(0, BASE - (position-1)*STEP) ---
    public static final int SEASON_POINTS_BASE = 100;
    public static final int SEASON_POINTS_STEP = 2;
}
