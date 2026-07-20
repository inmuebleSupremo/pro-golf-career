package com.progolf.sim.tour;

import com.progolf.sim.tournament.TournamentResult;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
    /** Cache of {@link #isExempt} for {@code exemptSeason}; derived from movementHistory, never persisted. */
    private final Set<String> exempt = new HashSet<>();
    private int exemptSeason = -1;

    public TourSystem() {
        for (TourTier tier : TourTier.values()) {
            tours.put(tier, new Tour("tour-" + tier.name().toLowerCase(), tierName(tier), tier));
        }
    }

    /**
     * An immutable capture of the tour system (spec: world-snapshot). The Tour objects themselves are
     * deterministic (rebuilt by the constructor), so only membership, season standings, movement history,
     * and the season are captured.
     */
    public record Snapshot(Map<String, TourTier> membership, Map<String, Integer> standings,
                           List<TourMovement> movementHistory, int season) {
        public Snapshot {
            membership = Map.copyOf(membership);
            standings = Map.copyOf(standings);
            movementHistory = List.copyOf(movementHistory);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(new LinkedHashMap<>(membership), standings.snapshot(),
                new ArrayList<>(movementHistory), season);
    }

    public static TourSystem restore(Snapshot s) {
        TourSystem t = new TourSystem();
        t.membership.putAll(s.membership());
        t.standings.restoreFrom(s.standings());
        t.movementHistory.addAll(s.movementHistory());
        t.season = s.season();
        return t;
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

    /**
     * Removes a golfer from Tour membership when they leave the active world (e.g. retirement). The golfer
     * no longer holds a membership and is excluded from future standings and reviews; their recorded
     * movement history is preserved.
     */
    public void deregister(String golferId) {
        membership.remove(Objects.requireNonNull(golferId, "golferId"));
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

    // --- Exemptions ---

    /**
     * Whether a golfer is exempt for the current season: they were promoted into the tier they now hold at
     * the end of the previous season, so they hold a card for it. A promoted golfer is the weakest member of
     * the tier they arrive in, and standings reset to zero every season — without an exemption, merit-ordered
     * entry would leave them outside every field, earning no points, and relegate them straight back with no
     * chance to compete (REQ-136/137: the ladder must be a pathway, not a revolving door).
     *
     * <p>Derived from the recorded movement history rather than stored, so it survives snapshot/restore and
     * never disagrees with the movements it is computed from. The tier check keeps the exemption tied to the
     * tier that was actually earned: a golfer moved on again since (e.g. by a qualification pathway) is no
     * longer exempt on the strength of the old promotion.
     */
    public boolean isExempt(String golferId) {
        if (exemptSeason != season) {
            exempt.clear();
            for (TourMovement m : movementHistory) {
                if (m.season() == season - 1 && m.type() == MovementType.PROMOTION
                        && m.toTier() == membership.get(m.golferId())) {
                    exempt.add(m.golferId());
                }
            }
            exemptSeason = season;
        }
        return exempt.contains(golferId);
    }

    // --- Season-end review ---

    /**
     * Reviews membership at season end (REQ-136): each tour above the entry tier sheds its bottom golfers,
     * then refills its cards from the top of the tour below. Deterministic and reproducible — movement is a
     * pure function of the standings and the published tier sizes.
     *
     * <p>Worked from the top tour down, so vacancies cascade: a tour thinned by retirement pulls golfers up
     * behind it in the same review, and the ladder holds its shape instead of hollowing out at the top while
     * the entry tier swells with everyone who could not be promoted fast enough. Promotion is therefore
     * demand-driven rather than a fixed count — at rest a tour promotes exactly as many as it relegated.
     */
    public SeasonReviewResult reviewSeasonEnd() {
        int reviewed = season;
        List<TourMovement> movements = new ArrayList<>();
        int totalMembers = membership.size();

        Map<TourTier, List<String>> ranked = new EnumMap<>(TourTier.class);
        for (TourTier tier : TourTier.values()) {
            ranked.put(tier, new ArrayList<>(standings.ranked(membersOf(tier))));
        }

        TourTier[] tiers = TourTier.values();
        for (int i = tiers.length - 1; i >= 1; i--) { // the entry tier is the reservoir; it is never reviewed
            TourTier tier = tiers[i];
            TourTier below = tiers[i - 1];
            List<String> here = ranked.get(tier);
            List<String> lower = ranked.get(below);

            // Shed the bottom: these golfers lose their cards.
            List<String> relegated = new ArrayList<>();
            int relegateN = Math.min(TourConstants.RELEGATE_COUNT, maxMove(here.size()));
            for (int k = 0; k < relegateN; k++) {
                String id = here.remove(here.size() - 1);
                relegated.add(id);
                movements.add(new TourMovement(id, tier, below, reviewed, MovementType.RELEGATION));
            }

            // Refill the vacated cards from the top of the tour below.
            int vacancies = Math.max(0, TourConstants.targetSize(tier, totalMembers) - here.size());
            int promoteN = Math.min(vacancies, Math.min(lower.size(), maxMove(lower.size())));
            for (int k = 0; k < promoteN; k++) {
                String id = lower.remove(0);
                here.add(id);
                movements.add(new TourMovement(id, below, tier, reviewed, MovementType.PROMOTION));
            }

            // The relegated join the tour below only once its promotions are drawn, so a golfer dropped from
            // above can never be promoted straight back in the same review.
            lower.addAll(relegated);
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

    /** The most golfers that may leave a tier in one review, so a tier is never emptied by a single season. */
    private static int maxMove(int size) {
        return (int) Math.floor(size * TourConstants.MAX_MOVE_FRACTION);
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
            case PRO -> "Pro Tour";
        };
    }
}
