import { projectCanonicalFlow, type CanonicalRenderModel } from "../../lib/play/canonical-geometry";
import type { Point } from "../../lib/play/hole-geometry";

export interface EffectiveWindDisplay {
  readonly x: number;
  readonly y: number;
  readonly magnitude: number;
  readonly unit: string;
}

export interface WindDisplayGeometry {
  readonly start: Point;
  readonly tip: Point;
  readonly firstWing: Point;
  readonly secondWing: Point;
}

const ARROW_LENGTH = 16;
const ARROW_HEAD_LENGTH = 5;
const ARROW_HEAD_HALF_WIDTH = 3;

/** Presentation geometry only: the server-authored flow is projected, never interpreted as flight authority. */
export function windDisplayGeometry(wind: EffectiveWindDisplay, model: CanonicalRenderModel, tee: Point): WindDisplayGeometry | null {
  if (wind.magnitude === 0) return null;
  const { x: dx, y: dy } = projectCanonicalFlow(tee, wind, model.project);
  const start = { x: 158, y: 27 };
  const tip = { x: start.x + dx * ARROW_LENGTH, y: start.y + dy * ARROW_LENGTH };
  const back = { x: -dx * ARROW_HEAD_LENGTH, y: -dy * ARROW_HEAD_LENGTH };
  const normal = { x: -dy * ARROW_HEAD_HALF_WIDTH, y: dx * ARROW_HEAD_HALF_WIDTH };
  return {
    start,
    tip,
    firstWing: { x: tip.x + back.x + normal.x, y: tip.y + back.y + normal.y },
    secondWing: { x: tip.x + back.x - normal.x, y: tip.y + back.y - normal.y },
  };
}

export function WindDisplay({ wind, model, tee }: {
  readonly wind: EffectiveWindDisplay;
  readonly model: CanonicalRenderModel;
  readonly tee: Point;
}) {
  const label = wind.unit === "EFFECTIVE_YARDS" ? "effective yd" : wind.unit;
  const geometry = windDisplayGeometry(wind, model, tee);
  if (!geometry) {
    return <text x={216} y={14} fill="var(--muted)" fontSize="6" textAnchor="end">Calm</text>;
  }
  const { start, tip, firstWing, secondWing } = geometry;
  return <g aria-label={`Wind toward ${wind.magnitude.toFixed(1)} ${label}`}>
    <line data-wind-stem="true" x1={start.x} y1={start.y} x2={tip.x} y2={tip.y} stroke="var(--info)" strokeWidth="1.6" />
    <path data-wind-arrowhead="true" d={`M${tip.x} ${tip.y} L${firstWing.x} ${firstWing.y} L${secondWing.x} ${secondWing.y} Z`} fill="var(--info)" />
    <text x={216} y={11} fill="var(--info)" fontSize="6" textAnchor="end">Wind toward</text>
    <text x={216} y={20} fill="var(--info)" fontSize="6" textAnchor="end">{wind.magnitude.toFixed(1)} {label}</text>
  </g>;
}
