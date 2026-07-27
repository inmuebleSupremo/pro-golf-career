package com.progolf.app.api.dto;

import java.util.List;

/**
 * The GraphQL view of a completed season's review (capability graphql-api): the off-season moment's read
 * model. Composes existing projections — the player's season {@link SeasonStatDto}, their world-ranking
 * movement across the season ({@code rankStart} → {@code rankEnd}, either null before the golfer was ranked),
 * the season's development gains ({@link DevelopmentDeltaDto}), and the season's news {@link NewsItemDto}
 * (prominence-sorted). {@code playerGolferId} lets the client split the golfer's own headlines from the
 * wider tour's. Assembled by {@code WorldService}; no {@code sim.*} type is exposed.
 */
public record SeasonReviewDto(int season,
                              Integer rankStart,
                              Integer rankEnd,
                              SeasonStatDto stats,
                              List<DevelopmentDeltaDto> development,
                              List<NewsItemDto> headlines,
                              String playerGolferId) {

    public SeasonReviewDto {
        development = development == null ? List.of() : List.copyOf(development);
        headlines = headlines == null ? List.of() : List.copyOf(headlines);
    }
}
