package com.progolf.sim.control;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.RiskApproach;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The human player's control of a single designated golfer (spec: player-control): the golfer's id plus the
 * player's standing decisions applied at the World's seams during a normal advance (configure-then-advance).
 * Covers a development focus (attribute priority), event entry (a blanket resting choice plus per-event
 * skips — the player is entered in eligible events by default and skips specific ones), while pending
 * sponsorship/staff/equipment offers are generated state the World holds separately. Framework-free —
 * depends only on {@code core}.
 */
public final class PlayerControl {

    private final String golferId;
    private List<Attribute> developmentFocus = List.of();
    private boolean resting;
    /**
     * Whether the player has chosen to compete through a recovering injury (spec: injury-recovery
     * play-through). Off by default: grinding costs real shot impairment, and every AI golfer sits a
     * recovering injury out, so defaulting it on silently handicapped the one golfer the player controls
     * against the entire field for their whole career.
     */
    private boolean playingThroughInjury;
    /**
     * The player's chosen risk approach for their golfer's rounds (spec: player-control), or empty to let the
     * golfer play the disposition their build implies. Aggressive play is higher-variance — more birdies and
     * more blow-ups — which is how the field's winners post low scores; letting the player choose it is how
     * they chase a win rather than grind out steady mid-pack finishes.
     */
    private RiskApproach riskApproach;
    private final Set<Long> skippedEvents = new LinkedHashSet<>();
    private List<CareerGoal> careerGoals = List.of();
    private int developmentPoints; // banked Development Points the player spends to raise attributes

    public PlayerControl(String golferId) {
        this.golferId = Objects.requireNonNull(golferId, "golferId");
    }

    /** The player's banked Development Points, earned from play and spent to raise attributes. */
    public int developmentPoints() {
        return developmentPoints;
    }

    /** Credits Development Points earned from a season's play. */
    public void addDevelopmentPoints(int amount) {
        developmentPoints += Math.max(0, amount);
    }

    /** Debits Development Points spent on attribute raises; rejects spending more than are banked. */
    public void spendDevelopmentPoints(int amount) {
        if (amount < 0 || amount > developmentPoints) {
            throw new IllegalArgumentException("Cannot spend " + amount + " Development Points (have "
                    + developmentPoints + ")");
        }
        developmentPoints -= amount;
    }

    /** The designated player-controlled golfer. */
    public String golferId() {
        return golferId;
    }

    /** The attributes, in priority order, the player wants development directed to (empty = automatic). */
    public List<Attribute> developmentFocus() {
        return developmentFocus;
    }

    public void setDevelopmentFocus(List<Attribute> focus) {
        this.developmentFocus = focus == null ? List.of() : List.copyOf(focus);
    }

    /** Whether the player's golfer is resting (sitting out event entry to recover). */
    public boolean isResting() {
        return resting;
    }

    /** The player's chosen risk approach, or empty to play the disposition their build implies. */
    public Optional<RiskApproach> riskApproach() {
        return Optional.ofNullable(riskApproach);
    }

    /** Sets the player's chosen risk approach; null clears it back to the build-implied disposition. */
    public void setRiskApproach(RiskApproach riskApproach) {
        this.riskApproach = riskApproach;
    }

    /** Whether the player's golfer competes through a recovering injury rather than sitting it out. */
    public boolean isPlayingThroughInjury() {
        return playingThroughInjury;
    }

    public void setPlayingThroughInjury(boolean playingThroughInjury) {
        this.playingThroughInjury = playingThroughInjury;
    }

    public void setResting(boolean resting) {
        this.resting = resting;
    }

    /** Marks an event (by tournament id) to be sat out; the player is otherwise entered by default. */
    public void skipEvent(long tournamentId) {
        skippedEvents.add(tournamentId);
    }

    /** Re-enters a previously skipped event (restoring default entry). */
    public void enterEvent(long tournamentId) {
        skippedEvents.remove(tournamentId);
    }

    /** Whether the player has chosen to skip the given event. */
    public boolean isSkipped(long tournamentId) {
        return skippedEvents.contains(tournamentId);
    }

    /** The events (by tournament id) the player has chosen to skip. */
    public Set<Long> skippedEvents() {
        return Set.copyOf(skippedEvents);
    }

    /** The player's self-chosen career goals (empty = none). */
    public List<CareerGoal> careerGoals() {
        return careerGoals;
    }

    public void setCareerGoals(List<CareerGoal> goals) {
        this.careerGoals = goals == null ? List.of() : List.copyOf(goals);
    }

    /** An immutable capture of the player's control state (spec: world-snapshot). */
    public record Snapshot(String golferId, List<Attribute> developmentFocus, boolean resting,
                           boolean playingThroughInjury, RiskApproach riskApproach, Set<Long> skippedEvents,
                           List<CareerGoal> careerGoals, int developmentPoints) {
        public Snapshot {
            developmentFocus = List.copyOf(developmentFocus);
            skippedEvents = new LinkedHashSet<>(skippedEvents); // preserve order
            careerGoals = List.copyOf(careerGoals);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(golferId, developmentFocus, resting, playingThroughInjury, riskApproach,
                skippedEvents, careerGoals, developmentPoints);
    }

    /** Rebuilds a player control from a snapshot via the existing setters (no new mutation surface). */
    public static PlayerControl restore(Snapshot s) {
        PlayerControl c = new PlayerControl(s.golferId());
        c.setDevelopmentFocus(s.developmentFocus());
        c.setResting(s.resting());
        c.setPlayingThroughInjury(s.playingThroughInjury());
        c.setRiskApproach(s.riskApproach());
        s.skippedEvents().forEach(c::skipEvent);
        c.setCareerGoals(s.careerGoals());
        c.addDevelopmentPoints(s.developmentPoints());
        return c;
    }
}
