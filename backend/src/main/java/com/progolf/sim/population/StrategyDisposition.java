package com.progolf.sim.population;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.shot.Strategy;

/**
 * Derives a generated golfer's innate strategic disposition — their risk appetite — from their attributes,
 * so the AI field plays a realistic spread of styles rather than a uniform {@link Strategy#BALANCED}
 * (spec: golfer-population). A golfer whose driving distance outstrips their course management and
 * composure is a boom-or-bust attacker ({@link Strategy#AGGRESSIVE}); a disciplined, composed,
 * well-managed golfer protects par ({@link Strategy#CONSERVATIVE}); most sit in between
 * ({@link Strategy#BALANCED}).
 *
 * <p>Because strategy widens (aggressive) or tightens (conservative) shot dispersion, this gives attackers
 * genuinely higher-variance results — a wider boom/bust tail — and steady players lower variance, without
 * any scripted outcomes. (Strategy is a single dispersion scalar, so a wider tail also costs mean score;
 * a mean-neutral aggression would need a compensating upside, which is the deferred situational tier.)
 * The mapping is a pure, deterministic function of attributes and is skill-neutral: the appetite formula's
 * discipline weights sum to the distance weight, so a "flat" profile (all attributes equal) scores exactly
 * zero appetite at any overall skill level.
 */
public final class StrategyDisposition {

    private StrategyDisposition() {
    }

    /** The strategic disposition implied by {@code attrs}. Pure and deterministic. */
    public static Strategy fromAttributes(Attributes attrs) {
        // Distance pushes toward risk; course management and composure (the discipline attributes) toward
        // caution. Weights sum to 1 on each side, so the result is centred at zero for a flat profile.
        double appetite = attrs.norm(Attribute.DRIVING_DISTANCE)
                - 0.5 * (attrs.norm(Attribute.COURSE_MANAGEMENT) + attrs.norm(Attribute.COMPOSURE));
        if (appetite > PopulationConstants.STRATEGY_APPETITE_THRESHOLD) {
            return Strategy.AGGRESSIVE;
        }
        if (appetite < -PopulationConstants.STRATEGY_APPETITE_THRESHOLD) {
            return Strategy.CONSERVATIVE;
        }
        return Strategy.BALANCED;
    }
}
