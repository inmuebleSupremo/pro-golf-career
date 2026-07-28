package com.progolf.sim.economy;

import static com.progolf.sim.economy.ReputationTier.ELITE;
import static com.progolf.sim.economy.ReputationTier.INTERNATIONAL;
import static com.progolf.sim.economy.ReputationTier.NATIONAL;
import static com.progolf.sim.economy.ReputationTier.REGIONAL;
import static com.progolf.sim.economy.SponsorIndustry.AUTOMOTIVE;
import static com.progolf.sim.economy.SponsorIndustry.FASHION;
import static com.progolf.sim.economy.SponsorIndustry.FINANCE;
import static com.progolf.sim.economy.SponsorIndustry.FRAGRANCE;
import static com.progolf.sim.economy.SponsorIndustry.WATCHES;

import java.util.ArrayList;
import java.util.List;

/**
 * The catalogue of fictional non-golf sponsor brands (spec: sponsorship). Each brand has an
 * {@link SponsorIndustry} for identity and a hidden {@link #luxury} tier that scales its money and gates who
 * it approaches — a brand only courts golfers of roughly its own prestige, so a journeyman is offered a
 * community bank or a sports-apparel label while a superstar attracts a luxury watchmaker or a prestige
 * carmaker. All names are original (no licensing exposure). The luxury tier is never surfaced to the player;
 * only the resulting money is.
 */
public enum SponsorBrand {

    // --- Watches ---
    TEMPO("Tempo", WATCHES, REGIONAL),
    HALDEN("Halden", WATCHES, NATIONAL),
    CHRONARCH("Chronarch", WATCHES, INTERNATIONAL),
    AURELIAN("Aurélian", WATCHES, ELITE),

    // --- Automotive ---
    DRIVEWELL("Drivewell", AUTOMOTIVE, REGIONAL),
    HALEWOOD("Halewood Motors", AUTOMOTIVE, NATIONAL),
    TORENZA("Torenza", AUTOMOTIVE, INTERNATIONAL),
    VERANO("Verano", AUTOMOTIVE, ELITE),

    // --- Fashion / sportswear ---
    CITYKIT("CityKit", FASHION, REGIONAL),
    NORTHGATE("Northgate", FASHION, NATIONAL),
    LUMIERE("Lumière", FASHION, INTERNATIONAL),
    ATELIER_NOIR("Atelier Noir", FASHION, ELITE),

    // --- Fragrance ---
    BLOOM("Bloom & Co", FRAGRANCE, REGIONAL),
    MAISON_CLEO("Maison Cléo", FRAGRANCE, NATIONAL),
    RIVARD("Rivard Parfums", FRAGRANCE, INTERNATIONAL),
    CENDRE("Cendre", FRAGRANCE, ELITE),

    // --- Banks / investment ---
    COMMUNITY_TRUST("Community Trust", FINANCE, REGIONAL),
    FIRSTLINE("Firstline Bank", FINANCE, NATIONAL),
    MERIDIAN_CAPITAL("Meridian Capital", FINANCE, INTERNATIONAL),
    STERLING_VAULT("Sterling Vault", FINANCE, ELITE);

    private final String displayName;
    private final SponsorIndustry industry;
    private final ReputationTier luxury;

    SponsorBrand(String displayName, SponsorIndustry industry, ReputationTier luxury) {
        this.displayName = displayName;
        this.industry = industry;
        this.luxury = luxury;
    }

    public String displayName() {
        return displayName;
    }

    public SponsorIndustry industry() {
        return industry;
    }

    public ReputationTier luxury() {
        return luxury;
    }

    /** How much the brand's luxury tier multiplies its money — a prestige brand pays far more than a modest one. */
    public double luxuryMultiplier() {
        return switch (luxury) {
            case UNKNOWN, REGIONAL -> 0.7;
            case NATIONAL -> 1.0;
            case INTERNATIONAL -> 1.7;
            case ELITE -> 2.8;
        };
    }

    /**
     * The brands willing to court a golfer of the given commercial tier: their own band and the one just
     * below (so there is range, and signing the top-tier brand pays more than settling for the lesser one).
     * A brand never approaches a golfer far above or below its prestige, so luxury status tracks standing.
     */
    public static List<SponsorBrand> eligibleFor(ReputationTier golferTier) {
        int top = Math.max(REGIONAL.ordinal(), golferTier.ordinal());
        int floor = Math.max(REGIONAL.ordinal(), top - 1);
        List<SponsorBrand> eligible = new ArrayList<>();
        for (SponsorBrand brand : values()) {
            int lux = brand.luxury.ordinal();
            if (lux >= floor && lux <= top) {
                eligible.add(brand);
            }
        }
        return eligible;
    }
}
