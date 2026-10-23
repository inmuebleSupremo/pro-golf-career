package com.progolf.app.api.dto;

/** Backend-authoritative availability of the next manual development purchase. */
public record AttributePurchaseOptionDto(String attribute, int rating, int potentialHeadroom,
                                         Integer nextCost, int remainingSeasonal, boolean purchasable,
                                         String blockingReason) {
}
