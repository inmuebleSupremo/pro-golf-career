package com.progolf.sim.tour;

import java.util.List;
import java.util.Objects;

/**
 * The outcome of a season-end membership review (REQ-136): the season reviewed and the movements it
 * produced.
 */
public record SeasonReviewResult(int season, List<TourMovement> movements) {

    public SeasonReviewResult {
        Objects.requireNonNull(movements, "movements");
        movements = List.copyOf(movements);
    }

    public List<TourMovement> promotions() {
        return movements.stream().filter(m -> m.type() == MovementType.PROMOTION).toList();
    }

    public List<TourMovement> relegations() {
        return movements.stream().filter(m -> m.type() == MovementType.RELEGATION).toList();
    }
}
