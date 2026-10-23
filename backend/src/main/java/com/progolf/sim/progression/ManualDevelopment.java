package com.progolf.sim.progression;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.player.AttributeChange;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** One rule for manual purchase validation and every read of current purchase availability. */
public final class ManualDevelopment {

    private ManualDevelopment() {
    }

    public enum BlockingReason {
        NONE, AT_POTENTIAL, SEASON_LIMIT, INSUFFICIENT_POINTS
    }

    public record PurchaseOption(Attribute attribute, int rating, int potentialHeadroom, Integer nextCost,
                                 int remainingSeasonal, boolean purchasable, BlockingReason blockingReason) {
    }

    /** Only positive purchased development in the current calendar season consumes allowance. */
    public static List<PurchaseOption> options(Attributes current, Attributes potential, int points,
                                                List<AttributeChange> history, int season) {
        int[] purchased = new int[Attribute.values().length];
        for (AttributeChange change : history) {
            if (change.season() == season && change.reason() == AttributeChange.Reason.DEVELOPMENT
                    && change.delta() > 0) {
                purchased[change.attribute().ordinal()] += change.delta();
            }
        }
        return Arrays.stream(Attribute.values()).map(attribute -> {
            int rating = current.get(attribute);
            int headroom = Math.max(0, potential.get(attribute) - rating);
            int remaining = Math.max(0,
                    ProgressionConstants.PLAYER_MAX_PURCHASED_GAIN_PER_ATTRIBUTE_SEASON
                            - purchased[attribute.ordinal()]);
            Integer nextCost = headroom > 0 ? DevelopmentPoints.costToRaise(rating) : null;
            BlockingReason reason = headroom == 0 ? BlockingReason.AT_POTENTIAL
                    : remaining == 0 ? BlockingReason.SEASON_LIMIT
                    : points < nextCost ? BlockingReason.INSUFFICIENT_POINTS
                    : BlockingReason.NONE;
            return new PurchaseOption(attribute, rating, headroom, nextCost, remaining,
                    reason == BlockingReason.NONE, reason);
        }).toList();
    }

    /** Validates the entire request before any points or attributes are changed, then returns its exact cost. */
    public static int costOfPurchase(Map<Attribute, Integer> raises, List<PurchaseOption> options, int points) {
        int total = 0;
        for (PurchaseOption option : options) {
            int steps = raises.getOrDefault(option.attribute(), 0);
            if (steps < 0 || steps > option.potentialHeadroom() || steps > option.remainingSeasonal()) {
                throw new IllegalArgumentException("Raise exceeds potential or seasonal allowance: "
                        + option.attribute());
            }
            for (int rating = option.rating(); rating < option.rating() + steps; rating++) {
                total += DevelopmentPoints.costToRaise(rating);
            }
        }
        if (total > points) {
            throw new IllegalArgumentException("Not enough Development Points (need " + total
                    + ", have " + points + ")");
        }
        return total;
    }
}
