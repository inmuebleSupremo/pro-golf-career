package com.progolf.app.api.dto;

/**
 * The GraphQL view of one golfer's finish in a completed event (capability graphql-api): their finishing
 * position, name, score relative to par, whether they made the cut, and prize money earned. Projects a
 * {@code sim.tournament.TournamentResult.Finish}.
 */
public record FinisherDto(int position, String name, int score, boolean madeCut, double earnings) {
}
