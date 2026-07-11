package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** financial-strategy (modified): tour-tier purse scaling and a money cut (only part of the field is paid). */
class PrizeStructureTierTest {

    @Test
    void higherTiersPayLargerPurses() {
        double dev = PrizeStructure.forEvent(Tier.DEVELOPMENT, EventPrestige.REGULAR, 20).topPrize();
        double std = PrizeStructure.forEvent(Tier.STANDARD, EventPrestige.REGULAR, 20).topPrize();
        double prem = PrizeStructure.forEvent(Tier.PREMIER, EventPrestige.REGULAR, 20).topPrize();
        double elite = PrizeStructure.forEvent(Tier.ELITE, EventPrestige.REGULAR, 20).topPrize();
        assertThat(dev).isLessThan(std);
        assertThat(std).isLessThan(prem);
        assertThat(prem).isLessThan(elite);
    }

    @Test
    void prestigeStillCompoundsWithTier() {
        double eliteRegular = PrizeStructure.forEvent(Tier.ELITE, EventPrestige.REGULAR, 20).topPrize();
        double eliteMajor = PrizeStructure.forEvent(Tier.ELITE, EventPrestige.MAJOR, 20).topPrize();
        assertThat(eliteMajor).isGreaterThan(eliteRegular); // a major is the biggest purse of all
    }

    @Test
    void theStandardCurveIsUnchanged() {
        assertThat(PrizeStructure.standard().amountForPosition(1)).isEqualTo(TournamentConstants.TOP_PRIZE);
    }

    @Test
    void onlyThePaidPositionsEarnMoney() {
        PrizeStructure purse = PrizeStructure.forEvent(Tier.ELITE, EventPrestige.REGULAR, 10);
        assertThat(purse.amountForPosition(1)).isGreaterThan(0.0);
        assertThat(purse.amountForPosition(10)).isGreaterThan(0.0);
        assertThat(purse.amountForPosition(11)).isEqualTo(0.0); // out of the money
        assertThat(purse.amountForPosition(40)).isEqualTo(0.0);
    }
}
