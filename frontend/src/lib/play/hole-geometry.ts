/** Shared canonical yard-space point used by the play read model and SVG renderer. */
export interface Point {
  readonly x: number;
  readonly y: number;
}

/** The complete gameplay-bearing geometry supplied by the PlayingHole API. */
export interface HoleGeom {
  readonly holeNumber: number;
  readonly par: number;
  readonly length: number;
  readonly fairwayHalfWidth: number;
  readonly greenHalfWidth: number;
  readonly greenDepth: number;
  readonly elevationDelta: number;
  readonly hasGreensideBunker: boolean;
  readonly hasWater: boolean;
  readonly hasTrees: boolean;
  readonly pinLateral: number;
  readonly pinDepth: number;
  readonly courseType: string;
  readonly layoutSeed: string;
  readonly geometry?: {
    readonly tee: Point;
    readonly cup: Point;
    readonly playableBoundary: readonly Point[];
    readonly regions: readonly { readonly surface: string; readonly boundary: readonly Point[] }[];
  };
  readonly ball?: { readonly position: Point; readonly lie: string };
}

/** A resolved observable shot, including the backend-authored spatial playback facts when available. */
export interface ResolvedShot {
  readonly finalSurface: string;
  readonly carry: number;
  readonly lateral: number;
  readonly distanceRemaining: number;
  readonly settlement?: {
    readonly contact: { readonly position: Point; readonly surface: string };
    readonly recoveryPosition?: Point | null;
    readonly recoveryKind: string;
    readonly ball: { readonly position: Point; readonly lie: string };
  } | null;
  readonly trace?: {
    readonly club: string;
    readonly origin: Point;
    readonly aimPoint: Point;
    readonly contact: { readonly position: Point; readonly surface: string };
    readonly roll?: { readonly from: Point; readonly to: Point } | null;
    readonly transition?: { readonly kind: string; readonly from: Point; readonly to: Point } | null;
    readonly finalPoint: Point;
  } | null;
}
