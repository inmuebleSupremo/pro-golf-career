package com.progolf.sim.progression;

import com.progolf.sim.player.AttributeChange;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable session ledger; the weekly preparation ledger owns its lifetime and reward state. */
public record PlayableTrainingSession(String id, PreparationFocus focus, int season, int week,
                                     List<TrainingChallenge> challenges, List<TrainingAttempt> attempts,
                                     Status status, double baseProgress, double bonusProgress, boolean bonusSettled,
                                     List<AttributeChange> attributeImprovements,
                                     Double fatigueBefore, Double fatigueAfter, List<TrainingRatingChange> ratingChanges) {
    public enum Status { ACTIVE, COMPLETED, ABANDONED }

    public PlayableTrainingSession {
        if (id == null || id.isBlank() || focus == null || !focus.technical() || season < 1 || week < 1
                || challenges == null || challenges.size() != 8 || attempts == null || attempts.size() > 8
                || attributeImprovements == null
                || status == null || !Double.isFinite(baseProgress) || baseProgress < 0
                || !Double.isFinite(bonusProgress) || bonusProgress < 0 || bonusProgress > baseProgress * 0.15 + 1e-9)
            throw new IllegalArgumentException("invalid playable training session");
        challenges = List.copyOf(challenges);
        attempts = List.copyOf(attempts);
        attributeImprovements = List.copyOf(attributeImprovements);
        ratingChanges = ratingChanges == null ? List.of() : List.copyOf(ratingChanges);
        if ((fatigueBefore == null) != (fatigueAfter == null)
                || fatigueBefore != null && (!Double.isFinite(fatigueBefore) || !Double.isFinite(fatigueAfter)
                || fatigueBefore < 0 || fatigueAfter < 0 || fatigueBefore > 1 || fatigueAfter > 1))
            throw new IllegalArgumentException("invalid training fatigue receipt");
        for (int i = 0; i < challenges.size(); i++) {
            if (challenges.get(i).number() != i + 1) throw new IllegalArgumentException("challenge order is invalid");
        }
        for (int i = 0; i < attempts.size(); i++) {
            if (attempts.get(i).number() != i + 1) throw new IllegalArgumentException("attempt order is invalid");
        }
        if ((status == Status.COMPLETED) != (attempts.size() == 8) || status == Status.ACTIVE && attempts.size() == 8)
            throw new IllegalArgumentException("session status disagrees with attempts");
        if (bonusSettled != (status == Status.COMPLETED))
            throw new IllegalArgumentException("completion and bonus settlement must be atomic");
        if (status != Status.COMPLETED && bonusProgress != 0)
            throw new IllegalArgumentException("incomplete training cannot have a performance bonus");
    }

    public int nextChallenge() { return status == Status.ACTIVE ? attempts.size() + 1 : 0; }

    public double score() {
        return attempts.isEmpty() ? 0.0 : attempts.stream().mapToDouble(TrainingAttempt::score).average().orElse(0.0);
    }

    public PlayableTrainingSession addAttempt(TrainingAttempt attempt) {
        Objects.requireNonNull(attempt);
        if (status != Status.ACTIVE || attempt.number() != nextChallenge() || attempts.size() == 7)
            throw new IllegalStateException("training attempt is stale or session has ended");
        List<TrainingAttempt> next = new ArrayList<>(attempts);
        next.add(attempt);
        return new PlayableTrainingSession(id, focus, season, week, challenges, next,
                Status.ACTIVE, baseProgress, bonusProgress, false, attributeImprovements,
                fatigueBefore, fatigueAfter, ratingChanges);
    }

    public PlayableTrainingSession complete(TrainingAttempt last, double bonus,
                                           List<AttributeChange> bonusImprovements,
                                           List<TrainingRatingChange> bonusRatings) {
        if (status != Status.ACTIVE || attempts.size() != 7 || last.number() != 8)
            throw new IllegalStateException("training session is not ready to complete");
        List<TrainingAttempt> next = new ArrayList<>(attempts);
        next.add(last);
        List<AttributeChange> improvements = new ArrayList<>(attributeImprovements);
        improvements.addAll(bonusImprovements);
        List<TrainingRatingChange> ratings = new ArrayList<>(ratingChanges);
        ratings.addAll(bonusRatings);
        return new PlayableTrainingSession(id, focus, season, week, challenges, next,
                Status.COMPLETED, baseProgress, bonus, true, improvements, fatigueBefore, fatigueAfter, ratings);
    }

    public PlayableTrainingSession abandon() {
        return status == Status.ACTIVE
                ? new PlayableTrainingSession(id, focus, season, week, challenges, attempts,
                        Status.ABANDONED, baseProgress, 0, false, attributeImprovements,
                        fatigueBefore, fatigueAfter, ratingChanges) : this;
    }
}
