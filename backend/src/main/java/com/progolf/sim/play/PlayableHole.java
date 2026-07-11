package com.progolf.sim.play;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.ShotContext;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.ShotResolver;
import com.progolf.sim.shot.SimConstants;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.shot.StrategyPolicy;
import com.progolf.sim.spatial.Surface;
import java.util.Objects;

/**
 * An interactive single hole a human plays shot-by-shot through the shot engine (spec: playable-event) —
 * used to play a sudden-death playoff hole. Its per-shot loop mirrors {@code RoundResolver.resolveHole}
 * exactly: the same stroke-and-distance rule, holed threshold, and shot cap, seeded at the supplied hole
 * coordinate via {@code coordinate.withShot(n)}. So a fully-simmed hole is identical to the automatic
 * playoff-hole resolution for the same inputs and seed, and playing by hand differs only by the human's
 * decisions.
 *
 * <p>Unlike {@link PlayableRound} (which addresses holes with {@code base.withHole(h)}), a playoff hole is
 * seeded with the hole coordinate already fixed — the automatic sudden-death path seeds each playoff hole
 * at {@code (…, playoffRound, fieldIndex, holeNumber, shot)} — so this takes the hole coordinate directly.
 */
public final class PlayableHole {

    private final int holeNumber;
    private final int par;
    private final Attributes attributes;
    private final GolferState state;
    private final HoleModel model;
    private final Environment environment;
    private final SeedCoordinate coordinate;
    private final StrategyPolicy simPolicy;

    private int shotNumber = 1;
    private double remaining;
    private int strokes;
    private Surface lie = Surface.TEE_BOX;
    private boolean complete;

    public PlayableHole(int holeNumber, int par, Attributes attributes, GolferState state, HoleModel model,
                        Environment environment, SeedCoordinate coordinate, Strategy simStrategy) {
        this.holeNumber = holeNumber;
        this.par = par;
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.state = Objects.requireNonNull(state, "state");
        this.model = Objects.requireNonNull(model, "model");
        this.environment = Objects.requireNonNull(environment, "environment");
        this.coordinate = Objects.requireNonNull(coordinate, "coordinate");
        this.simPolicy = new StrategyPolicy(Objects.requireNonNull(simStrategy, "simStrategy"));
        this.remaining = model.startDistance();
    }

    /** Whether the hole has been holed out (or hit the shot cap). */
    public boolean isComplete() {
        return complete;
    }

    /** The situation for the current shot. */
    public ShotSituation situation() {
        requireNotComplete();
        return new ShotSituation(holeNumber, par, shotNumber, strokes, remaining, lie,
                model.zoneProfileFor(remaining));
    }

    /** Plays the current shot with the human's decision (club / target / risk). */
    public ShotOutcome playShot(ShotDecision decision) {
        requireNotComplete();
        return resolveOne(Objects.requireNonNull(decision, "decision"));
    }

    /** Sims the current shot with the automatic policy. */
    public ShotOutcome simShot() {
        requireNotComplete();
        return resolveOne(simPolicy.decide(remaining));
    }

    /** Sims the rest of the hole with the automatic policy. */
    public void simHole() {
        while (!complete) {
            resolveOne(simPolicy.decide(remaining));
        }
    }

    private ShotOutcome resolveOne(ShotDecision decision) {
        double preShotRemaining = remaining;
        ShotContext context = new ShotContext(attributes, state, environment, remaining,
                model.zoneProfileFor(remaining), decision, coordinate.withShot(shotNumber), lie);
        ShotOutcome outcome = ShotResolver.resolveShot(context);

        strokes += outcome.strokes();
        lie = outcome.finalSurface();
        boolean holed;
        if (outcome.hazardEntered()) {
            remaining = preShotRemaining; // stroke-and-distance
            holed = false;
        } else {
            remaining = outcome.distanceRemaining();
            holed = remaining <= SimConstants.HOLED_THRESHOLD;
        }
        shotNumber++;
        if (holed || shotNumber > SimConstants.MAX_SHOTS_PER_HOLE) {
            complete = true;
        }
        return outcome;
    }

    /** Total strokes taken on the hole (final once complete). */
    public int strokes() {
        return strokes;
    }

    private void requireNotComplete() {
        if (complete) {
            throw new IllegalStateException("the hole is complete");
        }
    }
}
