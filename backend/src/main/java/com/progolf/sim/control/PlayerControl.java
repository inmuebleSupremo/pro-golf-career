package com.progolf.sim.control;

import com.progolf.sim.core.Attribute;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
    private final Set<Long> skippedEvents = new LinkedHashSet<>();

    public PlayerControl(String golferId) {
        this.golferId = Objects.requireNonNull(golferId, "golferId");
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
}
