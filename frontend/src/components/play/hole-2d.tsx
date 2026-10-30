"use client";

import { CanonicalHole2d } from "@/components/play/canonical-hole-2d";
import type { HoleGeom, Point, ResolvedShot } from "@/lib/play/hole-geometry";
import type { StageViewport } from "@/lib/play/stage-viewport";

export interface Hole2dProps {
  readonly hole: HoleGeom;
  /** The last observable result, rendered only from its backend-authored trace. */
  readonly ball?: ResolvedShot | null;
  readonly className?: string;
  readonly aimPoint?: Point;
  readonly onAimPoint?: (point: Point) => void;
  readonly showAimMarker?: boolean;
  /** Normal play selects V2 for supported Parkland holes; development can compare all modes. */
  readonly presentation?: "baseline" | "polished" | "premium-v1" | "premium";
  /** Development preview's known panel ratio; live play measures its stage. */
  readonly previewAspect?: number;
  /** Development preview only: inspect scenery before the stage lighting pass. */
  readonly unlitAudit?: boolean;
  readonly stageViewport?: StageViewport;
  readonly onPlaybackComplete?: () => void;
}

/** Active play has one terrain and shot authority: canonical backend geometry plus a returned trace. */
export function Hole2d(props: Hole2dProps) {
  if (!props.hole.geometry) {
    throw new Error("Canonical geometry is required for playable hole rendering");
  }
  return <CanonicalHole2d {...props} />;
}
