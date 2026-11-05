package com.progolf.app.api.dto;

import java.util.List;

public record CareerPreparationResultDto(boolean committed, String focus, int season, int week,
                                         double baseProgress, double simulatedBonusProgress, double progressAwarded,
                                         List<DevelopmentDeltaDto> attributeImprovements,
                                         double fatigueBefore, double fatigueAfter, double fatigueChange,
                                         boolean readinessPending) {}
