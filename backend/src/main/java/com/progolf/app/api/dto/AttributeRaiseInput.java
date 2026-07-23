package com.progolf.app.api.dto;

/**
 * A GraphQL input for spending Development Points (capability player-profile-api): raise {@code attribute}
 * (an Attribute enum name) by {@code points} levels. Parsed to the engine enum in the mapper.
 */
public record AttributeRaiseInput(String attribute, int points) {
}
