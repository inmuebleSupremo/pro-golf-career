package com.progolf.sim.play;

import com.progolf.sim.shot.ShotOutcome;

/** Atomic result of submitting a planned human strike. */
public record ShotSubmission(ShotOutcome outcome, boolean stale) {
    public static ShotSubmission staleResult() { return new ShotSubmission(null, true); }
    public static ShotSubmission resolved(ShotOutcome outcome) { return new ShotSubmission(outcome, false); }
}
