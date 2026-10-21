import type { Point } from "./hole-geometry";

/** Real-world dimensions retained by the solver and mirrored by the visual cup/ball markers. */
export const PHYSICAL_CUP_DIAMETER_INCHES = 4.25;
export const PHYSICAL_BALL_DIAMETER_INCHES = 1.68;
export const PHYSICAL_CUP_RADIUS_YARDS = PHYSICAL_CUP_DIAMETER_INCHES / 72;
export const PHYSICAL_BALL_RADIUS_YARDS = PHYSICAL_BALL_DIAMETER_INCHES / 72;

/** Active application tokens, kept here so the visual regression test protects the rendered SVG contract. */
export const PHYSICAL_PUTTING_SVG_STYLE = {
  canvasFill: "var(--surface-elevated)",
  greenFill: "var(--primary)",
  guideOutline: "var(--background)",
  guideStroke: "var(--foreground)",
  cupFill: "var(--background)",
  ballFill: "var(--foreground)",
} as const;

/** Screen geometry is separate from the solver's yard-space geometry. Locator rings are never capture areas. */
export const PHYSICAL_PUTTING_PRESENTATION = {
  width: 430,
  height: 300,
  minimumViewHeightYards: 2,
  contextPaddingYards: 2,
  minimumBallLocatorRadius: 4.5,
  minimumCupLocatorRadius: 5.5,
  slopeCellPixels: 52,
} as const;

export type PhysicalPuttingProjector = {
  readonly width: number;
  readonly height: number;
  readonly visibleSpanYards: number;
  readonly pixelsPerYard: number;
  project(point: Point): Point;
  unproject(point: Point): Point;
};

/**
 * Keeps a real, uniform yard-to-screen scale while framing the current decision instead of three times its
 * length. The optional footprint only constrains panning; it never changes ball/cup coordinates or scale.
 */
export function physicalPuttingProjector(ball: Point, cup: Point, green: readonly Point[]): PhysicalPuttingProjector {
  const { width, height, minimumViewHeightYards, contextPaddingYards } = PHYSICAL_PUTTING_PRESENTATION;
  const distance = Math.hypot(cup.x - ball.x, cup.y - ball.y);
  const spanY = Math.max(minimumViewHeightYards, distance + contextPaddingYards * 2);
  const spanX = spanY * width / height;
  const centreX = (ball.x + cup.x) / 2;
  const centreY = (ball.y + cup.y) / 2;
  const greenXs = green.map((point) => point.x);
  const greenYs = green.map((point) => point.y);
  const left = constrainViewport(centreX - spanX / 2, spanX, greenXs);
  const bottom = constrainViewport(centreY - spanY / 2, spanY, greenYs);
  const scale = width / spanX;

  return {
    width,
    height,
    visibleSpanYards: spanY,
    pixelsPerYard: scale,
    project: (point) => ({ x: (point.x - left) * scale, y: height - (point.y - bottom) * scale }),
    unproject: (point) => ({ x: left + point.x / scale, y: bottom + (height - point.y) / scale }),
  };
}

function constrainViewport(desiredStart: number, span: number, coordinates: readonly number[]): number {
  if (coordinates.length === 0) return desiredStart;
  const minimum = Math.min(...coordinates) - 2;
  const maximum = Math.max(...coordinates) + 2;
  if (maximum - minimum <= span) return (minimum + maximum - span) / 2;
  return Math.max(minimum, Math.min(desiredStart, maximum - span));
}

/** Neutral player defaults: the cup centre for line, measured yard-space distance converted to feet for pace. */
export function cupDirectedLine(ball: Point, cup: Point): Point {
  const dx = cup.x - ball.x;
  const dy = cup.y - ball.y;
  const length = Math.hypot(dx, dy);
  return length > 0.0001 ? { x: dx / length, y: dy / length } : { x: 0, y: 1 };
}

export function defaultPaceFeet(ball: Point, cup: Point): number {
  return Math.max(0.5, Math.hypot(cup.x - ball.x, cup.y - ball.y) * 3);
}

export function validPaceFeet(value: number): number {
  return Math.max(0.1, Number.isFinite(value) ? value : 0.1);
}

/**
 * A control source identifies the actual ball/cup pair behind one current shot.  The revision alone is not
 * enough: a query refresh can briefly render the new revision with an earlier hole projection.
 */
export function physicalPuttControlSource(revision: string, ball: Point, cup: Point): string {
  return `${revision}:${ball.x}:${ball.y}:${cup.x}:${cup.y}`;
}

export type PhysicalPuttControls = { readonly source: string; readonly direction: Point; readonly paceFeet: number };

/** New sources get neutral cup-directed controls; an unchanged source retains a deliberate player adjustment. */
export function currentPhysicalPuttControls(
  source: string,
  ball: Point,
  cup: Point,
  saved: PhysicalPuttControls | null,
): PhysicalPuttControls {
  return saved?.source === source
    ? saved
    : { source, direction: cupDirectedLine(ball, cup), paceFeet: defaultPaceFeet(ball, cup) };
}

/** Matches the trajectory renderer's timing so the sequence never races a physical result playback. */
export function physicalPuttPlaybackDurationMs(
  samples: readonly { timeSeconds: number; position: Point }[] | null | undefined,
): number {
  if (!samples || samples.length < 2) return 0;
  const total = Math.max(1, samples.at(-1)!.timeSeconds) * 1000;
  return Math.min(1450, Math.max(650, total * 0.45));
}

/** The ball marker uses the real ball-to-cup diameter ratio; capture is a solver rule, never a second marker. */
export function physicalBallMarkerRadius(cupMarkerRadius: number): number {
  return cupMarkerRadius * PHYSICAL_BALL_DIAMETER_INCHES / PHYSICAL_CUP_DIAMETER_INCHES;
}

/** Marker sizes transform with the same yard-space projector as ball and cup centres. */
export function physicalWorldMarkerRadius(radiusYards: number, pixelsPerYard: number): number {
  return radiusYards * pixelsPerYard;
}

/** A hollow, labelled locator makes a tiny real ball findable without enlarging its physical cross-section. */
export function physicalBallLocatorRadius(physicalRadiusPixels: number): number | null {
  return physicalRadiusPixels < 2.5 ? PHYSICAL_PUTTING_PRESENTATION.minimumBallLocatorRadius : null;
}

/** A dashed, unfilled locator identifies a tiny cup; the filled opening remains its true physical radius. */
export function physicalCupLocatorRadius(physicalRadiusPixels: number): number | null {
  return physicalRadiusPixels < 3.5 ? PHYSICAL_PUTTING_PRESENTATION.minimumCupLocatorRadius : null;
}

/** Positive means a stopped ball is visibly outside the physical cup rim at the supplied zoom. */
export function physicalCupBallScreenGap(distanceYards: number, pixelsPerYard: number): number {
  return physicalWorldMarkerRadius(distanceYards, pixelsPerYard)
    - physicalWorldMarkerRadius(PHYSICAL_CUP_RADIUS_YARDS + PHYSICAL_BALL_RADIUS_YARDS, pixelsPerYard);
}

/** The next-putt guide cannot coexist with an authoritative physical trajectory replay. */
export function physicalAimGuideVisible(hasPhysicalTrajectory: boolean): boolean {
  return !hasPhysicalTrajectory;
}

/** A visible finite guide follows exactly the submitted starting direction and is not a trajectory prediction. */
export function physicalAimGuideEndpoint(origin: Point, direction: Point, cupDistanceYards: number): Point {
  return { x: origin.x + direction.x * cupDistanceYards, y: origin.y + direction.y * cupDistanceYards };
}

/** Interpolates only between chronological solver samples; it never invents a route. */
export function physicalTrajectoryPoint(samples: readonly { timeSeconds: number; position: Point }[], progress: number): Point {
  if (samples.length === 0) throw new Error("physical trajectory must contain a solver sample");
  const total = Math.max(0.001, samples.at(-1)!.timeSeconds);
  const target = total * Math.min(1, Math.max(0, progress));
  const nextIndex = samples.findIndex((sample) => sample.timeSeconds >= target);
  if (nextIndex <= 0) return samples[0].position;
  if (nextIndex < 0) return samples.at(-1)!.position;
  const previous = samples[nextIndex - 1];
  const next = samples[nextIndex];
  const fraction = (target - previous.timeSeconds) / Math.max(0.0001, next.timeSeconds - previous.timeSeconds);
  return { x: previous.position.x + (next.position.x - previous.position.x) * fraction,
    y: previous.position.y + (next.position.y - previous.position.y) * fraction };
}

/** Returns only the chronological portion the player has already seen, including the current interpolated ball. */
export function physicalTraversedTrajectory<T extends { timeSeconds: number; position: Point }>(samples: readonly T[], progress: number): readonly T[] {
  if (samples.length === 0) return [];
  const boundedProgress = Math.min(1, Math.max(0, progress));
  const total = Math.max(0.001, samples.at(-1)!.timeSeconds);
  const target = total * boundedProgress;
  const nextIndex = samples.findIndex((sample) => sample.timeSeconds >= target);
  if (nextIndex < 0 || nextIndex === samples.length - 1 && samples[nextIndex].timeSeconds === target) return samples;
  if (nextIndex <= 0) return [samples[0]];
  const prefix = samples.slice(0, nextIndex);
  const current = physicalTrajectoryPoint(samples, boundedProgress);
  return [...prefix, { ...samples[nextIndex - 1], timeSeconds: target, position: current }];
}

type SlopeSample = { readonly position: Point; readonly slope: number };

/** Keeps only authoritative samples inside the current camera and one strongest sample per readable screen cell. */
export function visibleSlopeSamples<T extends SlopeSample>(samples: readonly T[], projector: PhysicalPuttingProjector): readonly T[] {
  const selected = new Map<string, T>();
  for (const sample of samples) {
    const point = projector.project(sample.position);
    if (point.x < -12 || point.x > projector.width + 12 || point.y < -12 || point.y > projector.height + 12) continue;
    const key = `${Math.floor(point.x / PHYSICAL_PUTTING_PRESENTATION.slopeCellPixels)}:${Math.floor(point.y / PHYSICAL_PUTTING_PRESENTATION.slopeCellPixels)}`;
    const current = selected.get(key);
    if (!current || sample.slope > current.slope) selected.set(key, sample);
  }
  return [...selected.values()];
}
