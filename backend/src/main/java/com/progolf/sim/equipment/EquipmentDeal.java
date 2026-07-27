package com.progolf.sim.equipment;

/**
 * An equipment brand deal (spec: equipment-influence) — a season-length endorsement, built parallel to the
 * generic sponsorship. A brand pays the player a one-off {@code signingBonus} plus a {@code perSeasonRetainer}
 * for {@code durationSeasons}, and kits their whole bag with the brand's gear at {@code gearTier}. In
 * exchange the player is locked in for the term (no other deal, no à-la-carte upgrades) and inherits the
 * brand's characteristic bias — so a deal is worth most when the brand's strengths fit the golfer's build.
 * Immutable.
 */
public record EquipmentDeal(EquipmentBrand brand, double signingBonus, double perSeasonRetainer,
                            int startSeason, int durationSeasons, double gearTier) {

    public EquipmentDeal {
        if (brand == null) {
            throw new IllegalArgumentException("brand must not be null");
        }
        if (signingBonus < 0 || perSeasonRetainer < 0) {
            throw new IllegalArgumentException("signing bonus and retainer must be >= 0");
        }
        if (durationSeasons < 1) {
            throw new IllegalArgumentException("durationSeasons must be >= 1: " + durationSeasons);
        }
        if (!Double.isFinite(gearTier) || gearTier < 0 || gearTier > 1) {
            throw new IllegalArgumentException("gearTier must be in [0,1]: " + gearTier);
        }
    }

    /** The last season (inclusive) this deal pays a retainer and provides gear. */
    public int lastActiveSeason() {
        return startSeason + durationSeasons - 1;
    }

    /** Whether the deal is active (paying / kitting) in the given season. */
    public boolean isActiveIn(int season) {
        return season >= startSeason && season <= lastActiveSeason();
    }

    /** Seasons of the deal still to run at the given season (0 once it has ended). */
    public int seasonsRemaining(int season) {
        return Math.max(0, lastActiveSeason() - season + 1);
    }
}
