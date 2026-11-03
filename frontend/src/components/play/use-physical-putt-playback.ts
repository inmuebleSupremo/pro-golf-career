"use client";

import { useLayoutEffect, useMemo, useRef, useState } from "react";

import type { ResolvedShot } from "@/lib/play/hole-geometry";
import { physicalPuttPlaybackDurationMs } from "@/lib/play/physical-putting";
import { golfAudio } from "@/lib/play/golf-audio";

/** Tournament and practice use one solver-trajectory clock and the same rolling/cup audio. */
export function usePhysicalPuttPlayback(ball: Pick<ResolvedShot, "physicalPutt" | "holed"> | null, onPlaybackComplete?: () => void) {
  const trajectory = useMemo(() => ball?.physicalPutt?.trajectory ?? [], [ball?.physicalPutt?.trajectory]);
  const [animation, setAnimation] = useState<{ trajectory: typeof trajectory | null; elapsed: number }>({ trajectory: null, elapsed: 0 });
  const elapsed = animation.trajectory === trajectory ? animation.elapsed : 0;
  const completedTrajectory = useRef<typeof trajectory | null>(null);
  const playbackCompleteRef = useRef(onPlaybackComplete);

  useLayoutEffect(() => {
    playbackCompleteRef.current = onPlaybackComplete;
  }, [onPlaybackComplete]);

  // A response contains the final point immediately; never paint it before playback begins.
  useLayoutEffect(() => {
    if (trajectory.length < 2) return;
    completedTrajectory.current = null;
    let frame = 0;
    let finished = false;
    const start = performance.now();
    const duration = physicalPuttPlaybackDurationMs(trajectory);
    const tick = (now: number) => {
      const next = Math.min(1, (now - start) / duration);
      if (ball?.physicalPutt) golfAudio.movement(ball.physicalPutt, next, "PUTTER", "PUTT");
      setAnimation({ trajectory, elapsed: next });
      if (next < 1) frame = requestAnimationFrame(tick);
      else if (completedTrajectory.current !== trajectory) {
        finished = true;
        completedTrajectory.current = trajectory;
        if (ball?.physicalPutt) golfAudio.cup(ball.physicalPutt, ball.physicalPutt.holed && !!ball.holed);
        playbackCompleteRef.current?.();
      }
    };
    frame = requestAnimationFrame((now) => {
      setAnimation({ trajectory, elapsed: 0 });
      frame = requestAnimationFrame(() => tick(now));
    });
    return () => { cancelAnimationFrame(frame); if (!finished) golfAudio.stopEffects(); };
  }, [trajectory, ball]);

  return { trajectory, elapsed };
}
