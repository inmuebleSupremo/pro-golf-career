package com.progolf.app.api.dto;

/**
 * The GraphQL input for an explicit world configuration on {@code createWorld} (capability graphql-api).
 * Every field is nullable; a null field falls back to the engine default (see {@code WorldConstants}). Maps
 * to {@code sim.world.WorldConfig} in the mapper. Boxed types are used so "unset" is distinguishable from 0.
 */
public record WorldConfigInput(Integer populationSize, Integer weeksPerSeason, Integer eventsPerTierPerSeason,
                               Integer fieldSize, Integer coursePoolSize, Integer majorsPerSeason,
                               Integer signatureEventsPerTier) {
}
