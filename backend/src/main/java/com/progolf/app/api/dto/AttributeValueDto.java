package com.progolf.app.api.dto;

/**
 * The GraphQL view of one golfer attribute (capability player-profile-api): its engine name, current value,
 * and potential — the ceiling this attribute can develop toward.
 */
public record AttributeValueDto(String attribute, int value, int potential) {
}
