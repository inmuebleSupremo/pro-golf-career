import { useEffect, useRef } from "react";

export const TRAINING_RESULT_DISPLAY_MS = 1750;

export function scheduleTrainingResultTransition(attemptNumber: number | null, playingBack: boolean,
  advance: () => void): () => void {
  if (attemptNumber === null || playingBack) return () => {};
  const timer = globalThis.setTimeout(advance, TRAINING_RESULT_DISPLAY_MS);
  return () => globalThis.clearTimeout(timer);
}

/** The caller only supplies a visible result after the scene's playback completion callback. */
export function useTrainingResultTransition(attemptNumber: number | null, playingBack: boolean,
  advance: () => Promise<void>) {
  const advanceRef = useRef(advance);
  const advancingRef = useRef(false);
  useEffect(() => { advanceRef.current = advance; }, [advance]);

  useEffect(() => {
    if (advancingRef.current) return;
    return scheduleTrainingResultTransition(attemptNumber, playingBack, () => {
      if (advancingRef.current) return;
      advancingRef.current = true;
      void advanceRef.current().finally(() => { advancingRef.current = false; });
    });
  }, [attemptNumber, playingBack]);
}
