package com.progolf.app.api.dto;

/** The GraphQL view of one golfer attribute (capability player-profile-api): its engine name and value. */
public record AttributeValueDto(String attribute, int value) {
}
