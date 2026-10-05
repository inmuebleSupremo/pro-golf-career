package com.progolf.app.api.dto;

public record ShotSettlementDto(ShotContactDto contact, PositionDto recoveryPosition, String recoveryKind,
                                BallStateDto ball) { }
