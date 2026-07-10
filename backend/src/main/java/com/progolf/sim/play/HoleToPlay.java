package com.progolf.sim.play;

import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.HoleModel;
import java.util.Objects;

/**
 * The per-hole inputs a {@link PlayableRound} needs to play one hole (spec: playable-round): its geometry
 * ({@link HoleModel}), its par, and the {@link Environment} in force (from the event's conditions). The
 * caller assembles the eighteen of these from the real course + weather; standalone use can pass
 * {@link Environment#calm()}. Immutable.
 */
public record HoleToPlay(HoleModel model, int par, Environment environment) {

    public HoleToPlay {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(environment, "environment");
        if (par < 3 || par > 5) {
            throw new IllegalArgumentException("par must be 3..5: " + par);
        }
    }
}
