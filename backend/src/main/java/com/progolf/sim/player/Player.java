package com.progolf.sim.player;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Handedness;
import com.progolf.sim.shot.GolferState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The canonical golfer entity (REQ-014). It is the single authoritative owner of a golfer's permanent
 * data: an immutable {@link Identity} and a reference to {@link Attributes} (reused from the numerical
 * model — never duplicated), plus mutable {@link PlayerState} and a {@link CareerStatus}.
 *
 * <p>Attributes evolve only through the guarded {@link #evolveAttributes} path (development and aging);
 * state lives in {@link PlayerState} which has no handle to attributes, so state changes and tournament
 * randomness can never alter permanent skill (REQ-019/020/049).
 */
public final class Player {

    private final String id;
    private final Identity identity;
    private final Handedness handedness;
    private Attributes attributes;
    private final Attributes potential;
    private final PlayerState state;
    private CareerStatus status;
    private final List<AttributeChange> attributeChanges = new ArrayList<>();

    public Player(String id, Identity identity, Attributes attributes, Attributes potential) {
        this(id, identity, attributes, potential, Handedness.RIGHT);
    }

    public Player(String id, Identity identity, Attributes attributes, Attributes potential, Handedness handedness) {
        this.id = Objects.requireNonNull(id, "id");
        this.identity = Objects.requireNonNull(identity, "identity");
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.potential = Objects.requireNonNull(potential, "potential");
        this.handedness = handedness == null ? Handedness.RIGHT : handedness;
        this.state = PlayerState.fresh();
        this.status = CareerStatus.CREATED;
    }

    /** A golfer with no headroom left — potential equals current ability. For fixtures and tests. */
    public Player(String id, Identity identity, Attributes attributes) {
        this(id, identity, attributes, attributes);
    }

    public String id() {
        return id;
    }

    /** The permanent, immutable identity. */
    public Identity identity() {
        return identity;
    }
    public Handedness handedness() { return handedness; }

    /** The referenced permanent attributes (single source of truth). */
    public Attributes attributes() {
        return attributes;
    }

    /**
     * The golfer's innate ceiling, per attribute: how good they could become if they develop perfectly. Drawn
     * once at generation and immutable for life — talent is not earned. Development converges current
     * attributes toward it and can never pass it, so a golfer's career is the story of how much of their
     * potential they actually realise (spec: player-development).
     */
    public Attributes potential() {
        return potential;
    }

    /**
     * Evolves the permanent attributes through progression (development raises, aging lowers), the only
     * sanctioned path that changes them (REQ-049/153). The new attributes must already be within 0–100;
     * every non-zero per-attribute change is recorded with its reason and season. Tournament resolution
     * never calls this — it reads attributes only.
     */
    public void evolveAttributes(Attributes next, AttributeChange.Reason reason, int season) {
        Objects.requireNonNull(next, "next");
        Objects.requireNonNull(reason, "reason");
        for (Attribute a : Attribute.values()) {
            int delta = next.get(a) - attributes.get(a);
            if (delta != 0) {
                attributeChanges.add(new AttributeChange(a, delta, reason, season));
            }
        }
        this.attributes = next;
    }

    /** The append-only history of permanent attribute changes (development and aging). */
    public List<AttributeChange> attributeChanges() {
        return List.copyOf(attributeChanges);
    }

    /** The mutable temporary state. */
    public PlayerState state() {
        return state;
    }

    /** An immutable capture of a Player's full state (spec: world-snapshot). */
    public record Snapshot(String id, Identity identity, Attributes attributes, Attributes potential, Handedness handedness,
                           CareerStatus status, List<AttributeChange> attributeChanges,
                           PlayerState.Snapshot state) {
        public Snapshot {
            attributeChanges = List.copyOf(attributeChanges);
            handedness = handedness == null ? Handedness.RIGHT : handedness;
        }
    }

    /** Captures this Player. */
    public Snapshot snapshot() {
        return new Snapshot(id, identity, attributes, potential, handedness, status, attributeChanges, state.snapshot());
    }

    /** Rebuilds a Player from a snapshot, bypassing the guarded transition machine (net-new reconstruction). */
    public static Player restore(Snapshot s) {
        Player p = new Player(s.id(), s.identity(), s.attributes(), s.potential(), s.handedness());
        p.status = s.status();
        p.attributeChanges.addAll(s.attributeChanges());
        p.restoreState(s.state());
        return p;
    }

    private void restoreState(PlayerState.Snapshot s) {
        this.state.setFatigue(s.fatigue());
        this.state.restoreRating(s.rating());
    }

    public CareerStatus status() {
        return status;
    }

    /** Performs a guarded career-status transition; rejects any transition not permitted (REQ-022). */
    public void transitionTo(CareerStatus target) {
        Objects.requireNonNull(target, "target");
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Illegal career-status transition: " + status + " -> " + target);
        }
        this.status = target;
    }

    /** Convenience: activate a CREATED player. */
    public void activate() {
        transitionTo(CareerStatus.ACTIVE);
    }

    /**
     * Produces the shot engine's per-shot {@link GolferState} from current state plus a situational
     * pressure value (pressure is contextual, not owned by the player). No attribute/state duplication.
     */
    public GolferState toGolferState(double pressure) {
        return new GolferState(state.fatigue(), pressure, state.equipmentForgiveness(), state.equipmentPower(),
                state.mentalSupport(), state.strategicSupport(),
                state.equipmentWorkability(), state.equipmentFeel(), state.injuryImpairment());
    }

    /**
     * A derived statistic (REQ-015): a form-adjusted skill estimate, always recalculated from current
     * inputs and never stored. Illustrates the derived-statistics category.
     */
    public double deriveFormRating() {
        double meanAttribute = 0;
        for (Attribute a : Attribute.values()) {
            meanAttribute += attributes.get(a);
        }
        meanAttribute /= Attribute.values().length;

        double mean = meanAttribute;
        double ratingAdjustment = (state.rating().value() - PlayerConstants.RATING_BASELINE) * 0.2;
        double fatiguePenalty = state.fatigue() * 10.0;
        return mean + ratingAdjustment - fatiguePenalty;
    }
}
