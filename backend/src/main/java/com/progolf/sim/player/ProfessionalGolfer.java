package com.progolf.sim.player;

import com.progolf.sim.shot.Strategy;
import java.util.Objects;
import java.util.Optional;

/**
 * A competitive participant (REQ-113): a {@link Player}, a Career reference (by id only in this
 * capability), and an immutable {@link ControlType}. Both control types share identical gameplay rules;
 * a simulation golfer carries a {@link DecisionPolicy} seam, a human golfer does not.
 */
public record ProfessionalGolfer(String id, Player player, String careerRef, ControlType controlType, DecisionPolicy decisionPolicy) {

    public ProfessionalGolfer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(careerRef, "careerRef");
        Objects.requireNonNull(controlType, "controlType");
        // The decision seam mirrors control type exactly: present for simulation, absent for human.
        if (controlType == ControlType.HUMAN && decisionPolicy != null) {
            throw new IllegalArgumentException("Human-controlled golfers do not carry a decision policy");
        }
        if (controlType == ControlType.SIMULATION && decisionPolicy == null) {
            throw new IllegalArgumentException("Simulation-controlled golfers must carry a decision policy");
        }
    }

    /** Creates a human-controlled golfer (decisions come from the human, not a policy). */
    public static ProfessionalGolfer human(String id, Player player, String careerRef) {
        return new ProfessionalGolfer(id, player, careerRef, ControlType.HUMAN, null);
    }

    /** Creates a simulation-controlled golfer with a decision-policy seam. */
    public static ProfessionalGolfer simulation(String id, Player player, String careerRef, DecisionPolicy policy) {
        return new ProfessionalGolfer(id, player, careerRef, ControlType.SIMULATION, Objects.requireNonNull(policy, "policy"));
    }

    /** The decision-policy seam, present only for simulation-controlled golfers. */
    public Optional<DecisionPolicy> policy() {
        return Optional.ofNullable(decisionPolicy);
    }

    /**
     * An immutable capture of a golfer (spec: world-snapshot). The decision policy is captured as its fixed
     * default {@link Strategy} (null for a human), since the policy is a pure function returning that strategy.
     */
    public record Snapshot(String id, Player.Snapshot player, String careerRef, ControlType controlType,
                           Strategy strategy) {
    }

    /** Captures this golfer. */
    public Snapshot snapshot() {
        Strategy strategy = policy().map(DecisionPolicy::defaultStrategy).orElse(null);
        return new Snapshot(id, player.snapshot(), careerRef, controlType, strategy);
    }

    /** Rebuilds a golfer from a snapshot, restoring the fixed-strategy decision seam for simulation golfers. */
    public static ProfessionalGolfer restore(Snapshot s) {
        Player player = Player.restore(s.player());
        if (s.controlType() == ControlType.HUMAN) {
            return human(s.id(), player, s.careerRef());
        }
        Strategy strategy = s.strategy();
        return simulation(s.id(), player, s.careerRef(), () -> strategy);
    }
}
