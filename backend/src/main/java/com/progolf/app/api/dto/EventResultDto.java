package com.progolf.app.api.dto;

import java.util.List;

/**
 * The GraphQL view of a completed event's headline result (capability graphql-api): the winner, the top
 * finishers, and — when the player was in the field — the player's own finish (null otherwise). Assembled
 * from a {@code sim.tournament.TournamentResult}.
 */
public record EventResultDto(FinisherDto winner, List<FinisherDto> topThree, FinisherDto playerFinish) {
}
