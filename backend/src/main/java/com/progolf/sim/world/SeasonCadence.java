package com.progolf.sim.world;

import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EventPrestige;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * The structured season cadence (spec: world-schedule) for the standard 30-week scale. A designed,
 * tier-specific calendar: majors anchor fixed "chapter" weeks on the Elite tour, signature events are
 * spotlighted (including a mid-season Elite+Development collision week), rest weeks bracket the peaks, each
 * tour closes with a Tour Championship (Development a week before Elite), and regular events fill the rest to
 * a moderate density. Purely declarative and seed-independent — the World assigns courses and ids — so
 * schedules stay reproducible. Applies only at the standard profile; other configs use the proportional
 * fallback in {@code World.generateSchedule}.
 */
final class SeasonCadence {

    private SeasonCadence() {
    }

    /** Target events per tier per season (the "moderate" density). */
    static final int TARGET_EVENTS_PER_TIER = 14;
    /** Elite major "chapter" weeks that divide the season. */
    static final int[] MAJOR_WEEKS = {7, 14, 21, 27};
    static final int ELITE_CHAMPIONSHIP_WEEK = 30;
    static final int NON_ELITE_CHAMPIONSHIP_WEEK = 29;

    private static final int[] ELITE_SIGNATURE_WEEKS = {4, 17, 24};
    // Week 17 collides with the Elite signature — a mid-season checkpoint for Development players.
    private static final int[] DEVELOPMENT_SIGNATURE_WEEKS = {5, 17, 25};
    private static final int[] MID_SIGNATURE_WEEKS = {5, 12, 25};
    // Elite rests the week before each major; the other tours rest during the majors themselves.
    private static final int[] ELITE_REST_WEEKS = {6, 13, 20, 26};

    /** Whether the structured cadence applies to this config (the standard 30-week / 4-major profile). */
    static boolean appliesTo(WorldConfig config) {
        return config.weeksPerSeason() == WorldConstants.WEEKS_PER_SEASON
                && config.majorsPerSeason() == WorldConstants.MAJORS_PER_SEASON;
    }

    /** Every (week, tier, prestige) placement for a standard-scale season, tiers in enum order, weeks ascending. */
    static List<Placement> forSeason(int weeks) {
        List<Placement> placements = new ArrayList<>();
        for (TourTier tier : TourTier.values()) {
            placementsForTier(tier, weeks).forEach((week, prestige) ->
                    placements.add(new Placement(week, tier, prestige)));
        }
        return placements;
    }

    private static TreeMap<Integer, EventPrestige> placementsForTier(TourTier tier, int weeks) {
        boolean elite = tier == TourTier.ELITE;
        TreeMap<Integer, EventPrestige> byWeek = new TreeMap<>();

        // Anchors: majors (Elite only; field is cross-tour via majorField), the tour championship, signatures.
        if (elite) {
            for (int week : MAJOR_WEEKS) {
                byWeek.put(week, EventPrestige.MAJOR);
            }
        }
        byWeek.put(elite ? ELITE_CHAMPIONSHIP_WEEK : NON_ELITE_CHAMPIONSHIP_WEEK, EventPrestige.TOUR_CHAMPIONSHIP);
        for (int week : signatureWeeks(tier)) {
            byWeek.putIfAbsent(week, EventPrestige.SIGNATURE);
        }

        // Fill regulars on remaining eligible weeks (excluding anchors and rest weeks) to the target density.
        int lastWeek = elite ? weeks : weeks - 1; // non-Elite tours finish a week early — Elite plays the finale
        List<Integer> eligible = new ArrayList<>();
        for (int week = 1; week <= lastWeek; week++) {
            if (!byWeek.containsKey(week) && !isRestWeek(tier, week)) {
                eligible.add(week);
            }
        }
        int regulars = Math.max(0, TARGET_EVENTS_PER_TIER - byWeek.size());
        for (int i = 0; i < regulars && !eligible.isEmpty(); i++) {
            int index = (int) ((long) i * eligible.size() / regulars);
            byWeek.putIfAbsent(eligible.get(index), EventPrestige.REGULAR);
        }
        return byWeek;
    }

    private static int[] signatureWeeks(TourTier tier) {
        return switch (tier) {
            case ELITE -> ELITE_SIGNATURE_WEEKS;
            case DEVELOPMENT -> DEVELOPMENT_SIGNATURE_WEEKS;
            case SECONDARY, PRIMARY -> MID_SIGNATURE_WEEKS;
        };
    }

    private static boolean isRestWeek(TourTier tier, int week) {
        int[] rest = tier == TourTier.ELITE ? ELITE_REST_WEEKS : MAJOR_WEEKS;
        for (int r : rest) {
            if (r == week) {
                return true;
            }
        }
        return false;
    }

    /** One placed event before the World assigns it a course and id. */
    record Placement(int week, TourTier tier, EventPrestige prestige) {
    }
}
