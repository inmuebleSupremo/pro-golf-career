package com.progolf.app.api.dto;

/**
 * The GraphQL view of a world session's status (capability graphql-api): the engine's current season, week,
 * active population size, whether the session is paused awaiting a player event, and — when it is — whether
 * that event has been played to the end and only awaits completion. This is the API-edge DTO that replaces the
 * removed provisional REST status record; no simulation-engine type is exposed.
 */
public record WorldStatusDto(String id, int season, int week, int activePopulation, boolean hasPendingEvent,
                             boolean pendingEventFinished) {
}
