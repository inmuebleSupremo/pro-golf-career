package com.progolf.app.api.dto;

/**
 * The GraphQL input for a player's shot decision (capability graphql-api): the {@code club} and
 * {@code strategy} as their engine enum names, the intended carry {@code targetDistance}, and an optional
 * {@code targetLateral} aim offset from the shot line (defaults to straight when omitted). Maps to
 * {@code sim.shot.ShotDecision} at the edge.
 */
public record ShotDecisionInput(String club, double targetDistance, Double targetLateral, String strategy) {
}
