import type { Point } from "@/lib/play/hole-geometry";

type RecordedPosition = { number: number; finalPosition: Point };

/** A server-accepted attempt is disclosed only after its available shot playback settles. */
export function visibleTrainingAttempt<T>(attempt: T | null, playbackActive: boolean): T | null {
  return playbackActive ? null : attempt;
}

export function trainingRestingBalls<T extends RecordedPosition>(attempts: readonly T[],
    currentAttempt: T | null, challengeNumber: number, playbackActive: boolean): RecordedPosition[] {
  const prior = attempts.filter((attempt) => !playbackActive || attempt.number !== challengeNumber);
  const visible = visibleTrainingAttempt(currentAttempt, playbackActive);
  return [...prior, ...(visible && !prior.some((attempt) => attempt.number === visible.number) ? [visible] : [])]
    .map((attempt) => ({ number: attempt.number, finalPosition: attempt.finalPosition }));
}
