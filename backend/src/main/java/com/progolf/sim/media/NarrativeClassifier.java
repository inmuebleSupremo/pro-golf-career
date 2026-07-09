package com.progolf.sim.media;

/**
 * Classifies a {@link CareerSummary} into a descriptive {@link CareerNarrative} (spec: career-narrative,
 * REQ-244/248). Pure and deterministic: the most specific narrative that fits wins, recognising diverse
 * forms of success rather than only championship winners.
 */
public final class NarrativeClassifier {

    private NarrativeClassifier() {
    }

    public static CareerNarrative classify(CareerSummary s) {
        if (s.rankingPosition() <= MediaConstants.DOMINANT_RANKING && s.careerWins() >= MediaConstants.DOMINANT_WINS) {
            return CareerNarrative.DOMINANT_CHAMPION;
        }
        if (s.age() >= MediaConstants.VETERAN_AGE && s.careerWins() >= 1
                && s.seasonsSinceLastWin() <= MediaConstants.RESURGENCE_MAX_SEASONS_SINCE_WIN) {
            return CareerNarrative.VETERAN_RESURGENCE;
        }
        if (s.careerWins() >= 1 && s.seasonsSinceLastWin() >= MediaConstants.DROUGHT_SEASONS) {
            return CareerNarrative.CHAMPIONSHIP_DROUGHT;
        }
        if (s.seasonsPlayed() <= MediaConstants.BREAKTHROUGH_MAX_SEASONS && s.careerWins() >= 1) {
            return CareerNarrative.BREAKTHROUGH_SEASON;
        }
        if (s.age() <= MediaConstants.PROSPECT_MAX_AGE && s.careerWins() == 0
                && s.rankingPosition() <= MediaConstants.PROSPECT_RANKING) {
            return CareerNarrative.RISING_PROSPECT;
        }
        if (s.careerWins() == 0 && s.seasonsPlayed() >= MediaConstants.CONTENDER_MIN_SEASONS
                && s.rankingPosition() <= MediaConstants.CONTENDER_RANKING) {
            return CareerNarrative.CONSISTENT_CONTENDER;
        }
        return CareerNarrative.ESTABLISHED_PROFESSIONAL;
    }
}
