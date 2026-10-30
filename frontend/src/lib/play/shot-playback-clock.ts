type Frame = (timestamp: number) => void;
type RequestFrame = (frame: Frame) => number;
const MAX_FRAME_DELTA_MS = 34;

/** Begin only after the origin has committed; finish one painted frame after the terminal trace point. */
export function startShotPlayback(durationMs: number, onProgress: (progress: number) => void,
  onComplete?: () => void, request: RequestFrame = requestAnimationFrame,
  cancel: (id: number) => void = cancelAnimationFrame): () => void {
  let frame = 0;
  let stopped = false;
  const start = (started: number) => {
    if (stopped) return;
    let previous = started;
    let elapsed = 0;
    const advance = (now: number) => {
      if (stopped) return;
      // A delayed frame must not teleport the ball through an unseen part of its trajectory.
      elapsed += Math.min(MAX_FRAME_DELTA_MS, Math.max(0, now - previous));
      previous = now;
      const progress = Math.min(1, elapsed / durationMs);
      onProgress(progress);
      if (progress < 1) frame = request(advance);
      else if (onComplete) frame = request(() => { if (!stopped) onComplete(); });
    };
    frame = request(advance);
  };
  frame = request(start);
  return () => { stopped = true; cancel(frame); };
}
