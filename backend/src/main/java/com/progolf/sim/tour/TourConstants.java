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

    // --- Tier sizes (fractions of total membership; the entry tier holds the remainder) ---
    /**
     * Each tour above the entry tier carries a fixed number of cards, as a share of the golfers in the
     * system. A tour is refilled to its size every season from the tour below, so retirement thinning the
     * upper tours pulls golfers up behind it instead of leaving the ladder hollow and the entry tier
     * swollen. The entry tier is the reservoir and has no target — it holds whatever is left.
     */
    public static final double ELITE_FRACTION = 0.08;
    public static final double PRIMARY_FRACTION = 0.17;
    public static final double SECONDARY_FRACTION = 0.30;

    /** The number of cards a tier carries, given the total membership. Zero for the entry tier. */
    public static int targetSize(TourTier tier, int totalMembers) {
        double fraction = switch (tier) {
            case ELITE -> ELITE_FRACTION;
            case PRIMARY -> PRIMARY_FRACTION;
            case SECONDARY -> SECONDARY_FRACTION;
            case DEVELOPMENT -> 0.0;
        };
        return (int) Math.round(totalMembers * fraction);
    }

    // --- Season points curve: points = max(0, BASE - (position-1)*STEP) ---
    public static final int SEASON_POINTS_BASE = 100;
    public static final int SEASON_POINTS_STEP = 2;
}
