package com.progolf.sim.tour;

/**
 * The single tunables surface for the Tour domain (mirrors {@code SimConstants}). Movement counts and
 * the season-points curve are placeholder calibration — structural behaviour (top rises, bottom falls,
 * deterministic) is what matters now; magnitudes are tuned with the World/economy later.
 */
public final class TourConstants {

    private TourConstants() {
    }

    /** Number of top-standings golfers promoted from a tier at season end. */
    public static final int PROMOTE_COUNT = 10;
    /** Number of bottom-standings golfers relegated from a tier at season end. */
    public static final int RELEGATE_COUNT = 10;

    /**
     * Never move more than this fraction of a tier's members in one review, so a small tier cannot be
     * emptied or hollowed out by a single season.
     */
    public static final double MAX_MOVE_FRACTION = 0.34;

    // --- Season points curve: points = max(0, BASE - (position-1)*STEP) ---
    public static final int SEASON_POINTS_BASE = 100;
    public static final int SEASON_POINTS_STEP = 2;
}
