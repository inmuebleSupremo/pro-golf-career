import type { Point } from "./hole-geometry";

export const RANGE_SCENE = { width: 1200, height: 700, bayY: 630, farY: 80, farDistance: 320 } as const;
export const RANGE_MOBILE_VIEWBOX = { x: 185, width: 830 } as const;
export const ACTIVE_BAY_MAT = { x: RANGE_SCENE.width / 2, y: 668 } as const;

export function rangeScaleAt(distance: number): number {
  return 2.64 - 0.62 * distance / RANGE_SCENE.farDistance;
}

/** One reversible yard-to-SVG projection for targets, aim, markers and shot traces. */
export function projectRangePoint(point: Point): Point {
  return {
    x: RANGE_SCENE.width / 2 + point.x * rangeScaleAt(point.y),
    y: RANGE_SCENE.bayY - point.y * (RANGE_SCENE.bayY - RANGE_SCENE.farY) / RANGE_SCENE.farDistance,
  };
}

/** Visual launch rises from the mat and rejoins the yard projection before the first target. */
export function projectRangeShotPoint(point: Point): Point {
  const projected = projectRangePoint(point);
  const matOffset = (ACTIVE_BAY_MAT.y - RANGE_SCENE.bayY) * Math.max(0, 1 - Math.max(0, point.y) / 25);
  return { x: projected.x, y: projected.y + matOffset };
}

export function unprojectRangePoint(point: Point, aimHalfWidth: number): Point {
  const distance = Math.min(RANGE_SCENE.farDistance, Math.max(15,
    (RANGE_SCENE.bayY - point.y) * RANGE_SCENE.farDistance / (RANGE_SCENE.bayY - RANGE_SCENE.farY)));
  const lateral = (point.x - RANGE_SCENE.width / 2) / rangeScaleAt(distance);
  return {
    x: Math.round(Math.min(aimHalfWidth, Math.max(-aimHalfWidth, lateral))),
    y: Math.round(distance),
  };
}

/** The visible mown field is the projection of the server's fairway width over 320 yards. */
export function rangeFieldPath(fieldHalfWidth: number): string {
  const farLeft = projectRangePoint({ x: -fieldHalfWidth, y: RANGE_SCENE.farDistance });
  const farRight = projectRangePoint({ x: fieldHalfWidth, y: RANGE_SCENE.farDistance });
  const nearLeft = projectRangePoint({ x: -fieldHalfWidth, y: 0 });
  const nearRight = projectRangePoint({ x: fieldHalfWidth, y: 0 });
  const pixel = (point: Point) => `${Math.round(point.x)} ${Math.round(point.y)}`;
  return `M${pixel(farLeft)} Q${RANGE_SCENE.width / 2} ${RANGE_SCENE.farY - 5} ${pixel(farRight)} L${pixel(nearRight)} L${pixel(nearLeft)}Z`;
}
