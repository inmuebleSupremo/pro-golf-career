/** Client-only presentation state for an already-resolved shot; canonical game state remains server-owned. */
export interface ResultPresentation<TShot, TFeedback> {
  readonly playbackShot: TShot | null;
  readonly lastOutcome: TFeedback | null;
}

export function showResolvedShot<TShot, TFeedback>(playbackShot: TShot, lastOutcome: TFeedback): ResultPresentation<TShot, TFeedback> {
  return { playbackShot, lastOutcome };
}

/** Releases only the visual trace when a non-holing playback completes; feedback stays available. */
export function completeNonHolingPlayback<TShot, TFeedback>(
  presentation: ResultPresentation<TShot, TFeedback>,
): ResultPresentation<TShot, TFeedback> {
  return { ...presentation, playbackShot: null };
}

export function clearResultPresentation<TShot, TFeedback>(): ResultPresentation<TShot, TFeedback> {
  return { playbackShot: null, lastOutcome: null };
}
