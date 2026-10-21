package com.progolf.app.api.dto;

/** A sparse sample of the resolver-owned green height field, including its downhill vector. */
public record GreenSlopeSampleDto(PositionDto position, double elevation, double downhillX,
                                  double downhillY, double slope) {
}
