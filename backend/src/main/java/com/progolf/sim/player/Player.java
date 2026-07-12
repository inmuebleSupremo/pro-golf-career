package com.progolf.sim.player;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
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
    private Attributes attributes;
    private final PlayerState state;
    private CareerStatus status;
    private final List<AttributeChange> attributeChanges = new ArrayList<>();

    public Player(String id, Identity identity, Attributes attributes) {
        this.id = Objects.requireNonNull(id, "id");
        this.identity = Objects.requireNonNull(identity, "identity");
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.state = PlayerState.fresh();
        this.status = CareerStatus.CREATED;
    }

    public String id() {
        return id;
    }

    /** The permanent, immutable identity. */
    public Identity identity() {
        return identity;
    }

    /** The referenced permanent attributes (single source of truth). */
    public Attributes attributes() {
        return attributes;
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
                state.equipmentWorkability(), state.equipmentFeel());
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
