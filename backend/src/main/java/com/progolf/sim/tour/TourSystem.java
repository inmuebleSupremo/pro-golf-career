package com.progolf.sim.tour;

import com.progolf.sim.tournament.TournamentResult;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The Tour domain engine (REQ-126–140): a tiered ladder of Tours, per-golfer membership, the resetting
 * Season Standings ledger, and a deterministic season-end promotion/relegation review.
 *
 * <p>Analytical and deterministic: it consumes tournament results (reading finishing positions only) and
 * organises competition. It never mutates player attributes or tournament results, computes no rankings
 * or finances (REQ-140), and its movement is a pure function of standings and published counts.
 */
public final class TourSystem {

    private final Map<TourTier, Tour> tours = new EnumMap<>(TourTier.class);
    private final Map<String, TourTier> membership = new LinkedHashMap<>();
    private final SeasonStandings standings = new SeasonStandings();
    private final List<TourMovement> movementHistory = new ArrayList<>();
    private int season = 1;

    public TourSystem() {
        for (TourTier tier : TourTier.values()) {
            tours.put(tier, new Tour("tour-" + tier.name().toLowerCase(), tierName(tier), tier));
        }
    }

    /** The Tour at a tier. */
    public Tour tour(TourTier tier) {
        return tours.get(tier);
    }

    public int currentSeason() {
        return season;
    }

    // --- Membership ---

    /** Registers a golfer's initial (single) Tour membership. Rejects a golfer already registered. */
    public void register(String golferId, TourTier tier) {
        Objects.requireNonNull(golferId, "golferId");
        Objects.requireNonNull(tier, "tier");
        if (membership.containsKey(golferId)) {
            throw new IllegalStateException("Golfer already has a Tour membership: " + golferId);
        }
        membership.put(golferId, tier);
    }

    public boolean isMember(String golferId) {
        return membership.containsKey(golferId);
    }

    /** The golfer's single current Tour tier, if any. */
    public Optional<TourTier> membershipOf(String golferId) {
        return Optional.ofNullable(membership.get(golferId));
    }

    /** Eligibility for a tier's tournaments: membership of that tier, or a defined invitation exception. */
    public boolean isEligible(String golferId, TourTier tier, boolean invited) {
        return tier.equals(membership.get(golferId)) || invited;
    }

    /** Grants membership of a tier via a transparent qualification pathway; recorded as QUALIFICATION. */
    public void grantMembership(String golferId, TourTier tier, String reason) {
        Objects.requireNonNull(golferId, "golferId");
        Objects.requireNonNull(tier, "tier");
        TourTier from = membership.get(golferId);
        membership.put(golferId, tier);
        movementHistory.add(new TourMovement(golferId, from, tier, season, MovementType.QUALIFICATION));
    }

    // --- Results ---

    /**
     * Records a tournament (belonging to exactly one Tour tier) by awarding season points to its finishers
     * by position. Analytical only — the result is never modified.
     */
    public void recordResult(TournamentResult result, TourTier tier) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(tier, "tier");
        for (TournamentResult.Finish finish : result.finishingOrder()) {
            if (!finish.withdrawn()) {
                standings.award(finish.golfer().player().id(), finish.position());
            }
        }
    }

    /** Members of a tier, ordered by season standings (best first). */
    public List<String> standings(TourTier tier) {
        return standings.ranked(membersOf(tier));
    }

    public int seasonPointsOf(String golferId) {
        return standings.pointsOf(golferId);
    }

    // --- Season-end review ---

    /**
     * Reviews membership at season end (REQ-136): per tier, promotes the top season-standings golfers to
     * the tier above and relegates the bottom golfers to the tier below, per published counts. Movements
     * are computed from the current standings, then applied together; the season index advances and
     * standings reset. Deterministic and reproducible.
     */
    public SeasonReviewResult reviewSeasonEnd() {
        int reviewed = season;
        List<TourMovement> movements = new ArrayList<>();

        // Compute all movements from the current snapshot before applying any (so a promoted golfer is
        // never re-evaluated in the tier they move into).
        for (TourTier tier : TourTier.values()) {
            List<String> ranked = standings.ranked(membersOf(tier));
            int size = ranked.size();
            if (size == 0) {
                continue;
            }
            int maxMove = (int) Math.floor(size * TourConstants.MAX_MOVE_FRACTION);
            int promoteN = tier.above().isPresent() ? Math.min(TourConstants.PROMOTE_COUNT, maxMove) : 0;
            int relegateN = tier.below().isPresent() ? Math.min(TourConstants.RELEGATE_COUNT, maxMove) : 0;
            // Guard against overlap: promotions (top) and relegations (bottom) must be disjoint.
            if (promoteN + relegateN > size) {
                relegateN = Math.max(0, size - promoteN);
            }

            for (int i = 0; i < promoteN; i++) {
                String id = ranked.get(i);
                movements.add(new TourMovement(id, tier, tier.above().orElseThrow(), reviewed, MovementType.PROMOTION));
            }
            for (int i = 0; i < relegateN; i++) {
                String id = ranked.get(size - 1 - i);
                movements.add(new TourMovement(id, tier, tier.below().orElseThrow(), reviewed, MovementType.RELEGATION));
            }
        }

        // Apply, record, reset, advance.
        for (TourMovement m : movements) {
            membership.put(m.golferId(), m.toTier());
        }
        movementHistory.addAll(movements);
        standings.reset();
        season++;

        return new SeasonReviewResult(reviewed, movements);
    }

    // --- History ---

    public List<TourMovement> movementHistory() {
        return List.copyOf(movementHistory);
    }

    // --- Helpers ---

    private List<String> membersOf(TourTier tier) {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, TourTier> e : membership.entrySet()) {
            if (e.getValue() == tier) {
                ids.add(e.getKey());
            }
        }
        return ids;
    }

    private static String tierName(TourTier tier) {
        return switch (tier) {
            case DEVELOPMENT -> "Development Tour";
            case SECONDARY -> "Secondary Tour";
            case PRIMARY -> "Primary Tour";
            case ELITE -> "Elite Tour";
        };
    }
}
