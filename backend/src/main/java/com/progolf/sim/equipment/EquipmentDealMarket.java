package com.progolf.sim.equipment;

import com.progolf.sim.core.Rng;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates equipment brand-deal offers for a free-agent golfer (spec: equipment-influence). The count and
 * terms scale with the golfer's commercial reputation ([0,1]); each offer is from a distinct brand so the
 * player weighs different characteristic biases (and different money) against their build. Deterministic —
 * every draw comes from the supplied {@link Rng}. Stateless.
 */
public final class EquipmentDealMarket {

    private EquipmentDealMarket() {
    }

    /** Brand-deal offers for a golfer of the given reputation, for deals starting in {@code startSeason}. */
    public static List<EquipmentDeal> generateOffers(double reputation, int startSeason, Rng rng) {
        double rep = clampUnit(reputation);
        EquipmentBrand[] brands = shuffled(EquipmentBrand.UPGRADE_BRANDS, rng);
        int count = Math.min(brands.length,
                EquipmentConstants.DEAL_OFFER_BASE + (int) (rep * EquipmentConstants.DEAL_OFFER_REP_SPAN));

        List<EquipmentDeal> offers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double noise = Math.max(0.4, 1.0 + rng.nextGaussian() * EquipmentConstants.DEAL_VALUE_NOISE);
            double retainer = EquipmentConstants.DEAL_RETAINER_BASE
                    * (1.0 + rep * EquipmentConstants.DEAL_RETAINER_REP_SCALE) * noise;
            double signing = retainer * EquipmentConstants.DEAL_SIGNING_FRACTION;
            double gearTier = Math.min(EquipmentConstants.DEAL_GEAR_TIER_MAX,
                    EquipmentConstants.DEAL_GEAR_TIER_BASE + rep * EquipmentConstants.DEAL_GEAR_TIER_REP_SCALE
                            + rng.nextDouble() * 0.04);
            int duration = EquipmentConstants.DEAL_MIN_DURATION + (int) (rng.nextDouble()
                    * (EquipmentConstants.DEAL_MAX_DURATION - EquipmentConstants.DEAL_MIN_DURATION + 1));
            offers.add(new EquipmentDeal(brands[i], signing, retainer, startSeason, duration, gearTier));
        }
        return offers;
    }

    private static EquipmentBrand[] shuffled(EquipmentBrand[] source, Rng rng) {
        EquipmentBrand[] copy = source.clone();
        for (int i = copy.length - 1; i > 0; i--) {
            int j = (int) Math.floorMod(rng.nextLong(), i + 1);
            EquipmentBrand tmp = copy[i];
            copy[i] = copy[j];
            copy[j] = tmp;
        }
        return copy;
    }

    private static double clampUnit(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }
}
