package com.progolf.sim.economy;

/**
 * The industry a non-golf sponsor belongs to (spec: sponsorship). Endorsements come from outside golf —
 * watchmakers, carmakers, fashion houses, fragrance and finance — which gives each deal identity. Purely
 * cosmetic flavour; the money is decided by the golfer's reputation and the brand's luxury tier.
 */
public enum SponsorIndustry {
    WATCHES("Watches"),
    AUTOMOTIVE("Automotive"),
    FASHION("Fashion"),
    FRAGRANCE("Fragrance"),
    FINANCE("Finance");

    private final String label;

    SponsorIndustry(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
