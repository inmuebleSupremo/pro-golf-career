import type { Situation } from "@/components/play/play-shared";
import type { HoleGeom, Point, ResolvedShot } from "@/lib/play/hole-geometry";
import type { StageViewport } from "@/lib/play/stage-viewport";

/** The planning target belongs only to editable pre-shot state. */
export function shotAimMarkerVisible(nonPuttSubmitting: boolean, playbackShot: ResolvedShot | null): boolean {
  return !nonPuttSubmitting && playbackShot === null;
}

/** Keep the non-putt shot's controls and camera with its frozen course until its renderer finishes. */
export function stagePlaybackState({ playbackShot, displayHole, displaySituation, liveHole, liveSituation,
  shotViewport, liveViewport, shotAimPoint, liveAimPoint }: {
  playbackShot: ResolvedShot | null;
  displayHole: HoleGeom | null;
  displaySituation: Situation;
  liveHole: HoleGeom | null;
  liveSituation: Situation;
  shotViewport?: StageViewport | null;
  liveViewport?: StageViewport;
  shotAimPoint?: Point;
  liveAimPoint: Point;
}) {
  const nonPuttPlayback = playbackShot !== null && playbackShot.physicalPutt == null;
  return {
    dockHole: nonPuttPlayback ? displayHole : liveHole,
    dockSituation: nonPuttPlayback ? displaySituation : liveSituation,
    aimPoint: nonPuttPlayback ? shotAimPoint ?? liveAimPoint : liveAimPoint,
    viewport: nonPuttPlayback
      ? shotViewport === undefined ? liveViewport : shotViewport ?? undefined
      : liveViewport,
  };
}
