package com.progolf.sim.progression;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Persistent weekly preparation ledger. The calendar key and reward state travel in one snapshot. */
public record CareerPreparation(PreparationFocus standingFocus, int consumedSeason, int consumedWeek,
                                PreparationFocus committedFocus,
                                Map<PreparationFocus, Double> progress, boolean ready,
                                PlayableTrainingSession playableSession) {
    public CareerPreparation {
        standingFocus = standingFocus == null ? PreparationFocus.DRIVING : standingFocus;
        EnumMap<PreparationFocus, Double> copy = new EnumMap<>(PreparationFocus.class);
        if (progress != null) {
            progress.forEach((focus, value) -> {
                Objects.requireNonNull(focus);
                if (!focus.technical() || value == null || !Double.isFinite(value) || value < 0) {
                    throw new IllegalArgumentException("Invalid training progress");
                }
                copy.put(focus, value);
            });
        }
        progress = Map.copyOf(copy);
    }

    public static CareerPreparation initial() {
        return new CareerPreparation(PreparationFocus.DRIVING, 0, 0, null, Map.of(), false, null);
    }

    public boolean consumed(int season, int week) {
        return consumedSeason == season && consumedWeek == week;
    }

    public double progressFor(PreparationFocus focus) {
        return progress.getOrDefault(focus, 0.0);
    }

    public CareerPreparation withStanding(PreparationFocus focus) {
        return new CareerPreparation(Objects.requireNonNull(focus), consumedSeason, consumedWeek,
                committedFocus, progress, ready, playableSession);
    }

    public CareerPreparation committed(int season, int week, PreparationFocus focus, double remaining) {
        EnumMap<PreparationFocus, Double> next = new EnumMap<>(PreparationFocus.class);
        next.putAll(progress);
        if (focus.technical()) next.put(focus, remaining);
        return new CareerPreparation(standingFocus, season, week, focus, next,
                ready || focus == PreparationFocus.TOURNAMENT_PREPARATION, playableSession);
    }

    public CareerPreparation withProgress(PreparationFocus focus, double remaining) {
        EnumMap<PreparationFocus, Double> next = new EnumMap<>(PreparationFocus.class);
        next.putAll(progress);
        next.put(focus, remaining);
        return new CareerPreparation(standingFocus, consumedSeason, consumedWeek, committedFocus, next,
                ready, playableSession);
    }

    public CareerPreparation withPlayableSession(PlayableTrainingSession session) {
        if (session != null && (!consumed(session.season(), session.week()) || committedFocus != session.focus()))
            throw new IllegalArgumentException("playable session must match the committed week");
        return new CareerPreparation(standingFocus, consumedSeason, consumedWeek, committedFocus, progress,
                ready, session);
    }

    public CareerPreparation withoutReadiness() {
        return new CareerPreparation(standingFocus, consumedSeason, consumedWeek, committedFocus, progress, false,
                playableSession);
    }
}
