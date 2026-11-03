package com.progolf.app.api.dto;

import java.util.List;

public record CareerPreparationDto(String standingFocus, boolean available, int consumedSeason, int consumedWeek,
                                   String committedFocus,
                                   boolean ready, List<TrainingProgressDto> progress, Rules rules) {
    public record TrainingProgressDto(String focus, double amount) {}
    public record Rules(double trainingBaseProgress, double simulatedBonusProgress,
                        double totalSimulatedProgress, double trainingFatigue,
                        double recoveryReduction, double readinessSupport) {}
}
