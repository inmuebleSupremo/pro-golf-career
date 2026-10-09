package com.progolf.app.api.dto;
import java.util.List;
public record ShotGuidanceDto(AimPointDto safe, AimPointDto primary, AimPointDto aggressive, List<ClubReachDto> clubs,
                              List<StrategicTargetOptionDto> strategicOptions) { }
