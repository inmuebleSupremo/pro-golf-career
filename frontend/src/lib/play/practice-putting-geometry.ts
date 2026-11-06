import type { Point } from "./hole-geometry";
import type { PhysicalPuttingProjector } from "./physical-putting";

/** The approved overhead composition uses a uniform 35 SVG units per authoritative yard. */
export const PRACTICE_PUTTING_SCENE = { width: 1280, height: 720, pixelsPerYard: 35 } as const;
export const PRACTICE_PUTTING_AIM_STEP_DEGREES = 0.5;

/** The selected point defines a line from the live ball, with no target-area snapping. */
export function practicePuttingAimDirection(ball: Point, target: Point): Point | null {
  const dx = target.x - ball.x;
  const dy = target.y - ball.y;
  const length = Math.hypot(dx, dy);
  return length > 0.01 ? { x: dx / length, y: dy / length } : null;
}

export function rotatePracticePuttingLine(direction: Point, degrees: number): Point {
  const radians = degrees * Math.PI / 180;
  return { x: direction.x * Math.cos(radians) - direction.y * Math.sin(radians),
    y: direction.x * Math.sin(radians) + direction.y * Math.cos(radians) };
}

/** Practice and training send the selected physical controls without quantizing either value. */
export function practicePuttingShotIntent(
  direction: Point, paceFeet: number, expectedShotRevision: string,
) {
  return { direction, intendedRollFeet: paceFeet, expectedShotRevision };
}

export const practicePuttingProjector: PhysicalPuttingProjector = {
  ...PRACTICE_PUTTING_SCENE,
  visibleSpanYards: PRACTICE_PUTTING_SCENE.height / PRACTICE_PUTTING_SCENE.pixelsPerYard,
  project: (point: Point) => ({
    x: PRACTICE_PUTTING_SCENE.width / 2 + point.x * PRACTICE_PUTTING_SCENE.pixelsPerYard,
    y: PRACTICE_PUTTING_SCENE.height / 2 - point.y * PRACTICE_PUTTING_SCENE.pixelsPerYard,
  }),
  unproject: (point: Point) => ({
    x: (point.x - PRACTICE_PUTTING_SCENE.width / 2) / PRACTICE_PUTTING_SCENE.pixelsPerYard,
    y: (PRACTICE_PUTTING_SCENE.height / 2 - point.y) / PRACTICE_PUTTING_SCENE.pixelsPerYard,
  }),
};

/** A wider desktop camera keeps the entire playable footprint in view without scaling world X and Y differently. */
export function practicePuttingWideViewBox(stageWidth: number, stageHeight: number) {
  const width = PRACTICE_PUTTING_SCENE.width;
  const height = Math.min(PRACTICE_PUTTING_SCENE.height, Math.max(580,
    width * stageHeight / Math.max(1, stageWidth)));
  return { x: 0, y: (PRACTICE_PUTTING_SCENE.height - height) / 2, width, height };
}

/** Extend only the surrounding landscape to the viewport aspect; the playable camera keeps its scale and centre. */
export function practicePuttingEnvironmentViewBox(
  camera: { x: number; y: number; width: number; height: number }, stageWidth: number, stageHeight: number,
) {
  const aspect = stageWidth / Math.max(1, stageHeight);
  const width = Math.max(camera.width, camera.height * aspect);
  const height = Math.max(camera.height, camera.width / Math.max(aspect, 0.01));
  return {
    x: camera.x - (width - camera.width) / 2,
    y: camera.y - (height - camera.height) / 2,
    width,
    height,
  };
}

export function practicePuttingFocusViewBox(start: Point, cup: Point, stageWidth: number, stageHeight: number) {
  const width = 590;
  const height = width * stageHeight / Math.max(1, stageWidth);
  const first = practicePuttingProjector.project(start);
  const target = practicePuttingProjector.project(cup);
  const centreX = (first.x + target.x) / 2;
  const centreY = (first.y + target.y) / 2;
  const x = Math.max(0, Math.min(PRACTICE_PUTTING_SCENE.width - width, centreX - width / 2));
  const y = height >= PRACTICE_PUTTING_SCENE.height ? (PRACTICE_PUTTING_SCENE.height - height) / 2
    : Math.max(0, Math.min(PRACTICE_PUTTING_SCENE.height - height, centreY - height / 2));
  return { x, y, width, height };
}
