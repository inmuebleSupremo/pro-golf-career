import type { HoleGeom, Point, ResolvedShot } from "./hole-geometry";
import { distanceToPolygon, pointInPolygon, premiumPuttingFamily, validPoint, validPolygon, type PuttingScene } from "./premium-putting";
import type { PhysicalPuttingProjector } from "./physical-putting";

export const AROUND_GREEN_BOUNDARY_YARDS = 40;

const SHORT_GAME_LIES = new Set(["FRINGE", "FIRST_CUT", "PRIMARY_ROUGH", "FAIRWAY", "BUNKER"]);

/** The ball-to-green edge determines the view, regardless of its distance to the cup. */
export function aroundGreenEligible(hole: HoleGeom | null, lie: string, presentation: string): boolean {
  if (!hole || lie === "GREEN" || !SHORT_GAME_LIES.has(lie) || !premiumPuttingFamily(hole, presentation)) return false;
  const geometry = hole.geometry!;
  const green = geometry.regions.find((region) => region.surface === "GREEN")!.boundary;
  return validPolygon(geometry.playableBoundary) && validPoint(hole.ball?.position)
    && !pointInPolygon(hole.ball.position, green)
    && distanceToPolygon(hole.ball.position, green) <= AROUND_GREEN_BOUNDARY_YARDS;
}

/** Uniform yard-space camera for the existing putting SVG, fitted to the live ball and complete green. */
export function aroundGreenProjector(hole: HoleGeom, scene: PuttingScene, aim?: Point,
                                     trace?: ResolvedShot["trace"]): PhysicalPuttingProjector {
  const geometry = hole.geometry!;
  const green = geometry.regions.find((region) => region.surface === "GREEN")!.boundary;
  const fringe = geometry.regions.find((region) => region.surface === "FRINGE")!.boundary;
  const ball = hole.ball!.position;
  const core = [...green, ...fringe, ball, geometry.cup];
  const xs = core.map((point) => point.x), ys = core.map((point) => point.y);
  const coreBounds = { left: Math.min(...xs), right: Math.max(...xs), bottom: Math.min(...ys), top: Math.max(...ys) };
  const nearby = geometry.regions.filter((region) => (region.surface === "BUNKER" || region.surface === "WATER" || region.surface === "WASTE_AREA")
    && validPolygon(region.boundary)
    && (distanceToPolygon(ball, region.boundary) <= 18
      || green.some((point) => distanceToPolygon(point, region.boundary) <= 18)));
  // Large water and bunker polygons may extend across the hole. Only their nearby portion affects framing.
  const hazardPoints = nearby.flatMap((region) => {
    const regionXs = region.boundary.map((point) => point.x), regionYs = region.boundary.map((point) => point.y);
    if (Math.max(...regionXs) - Math.min(...regionXs) <= 45
      && Math.max(...regionYs) - Math.min(...regionYs) <= 45) return region.boundary;
    return region.boundary.map((point) => ({
      x: Math.max(coreBounds.left - 18, Math.min(coreBounds.right + 18, point.x)),
      y: Math.max(coreBounds.bottom - 18, Math.min(coreBounds.top + 18, point.y)),
    }));
  });
  const tracePoints = trace ? [trace.origin, trace.contact.position, trace.finalPoint,
    ...(trace.airbornePath?.map((sample) => sample.position) ?? []),
    ...(trace.groundPath?.map((sample) => sample.position) ?? []),
    ...(trace.roll ? [trace.roll.from, trace.roll.to] : []),
    ...(trace.transition ? [trace.transition.from, trace.transition.to] : [])] : [];
  const points = [...core, ...hazardPoints, ...(aim && validPoint(aim) ? [aim] : []), ...tracePoints];
  const minX = Math.min(...points.map((point) => point.x)) - 3;
  const maxX = Math.max(...points.map((point) => point.x)) + 3;
  const minY = Math.min(...points.map((point) => point.y)) - 3;
  const maxY = Math.max(...points.map((point) => point.y)) + 3;
  const sideMargin = Math.min(36, Math.max(18, scene.width * .055));
  const verticalMargin = Math.min(42, Math.max(24, scene.height * .11));
  const scale = Math.min(Math.max(1, scene.width - sideMargin * 2) / Math.max(1, maxX - minX),
    Math.max(1, scene.height - verticalMargin * 2) / Math.max(1, maxY - minY));
  const centreX = (minX + maxX) / 2, centreY = (minY + maxY) / 2;
  const screenCentreX = scene.x + scene.width / 2, screenCentreY = scene.y + scene.height / 2;
  return {
    width: scene.width, height: scene.height, visibleSpanYards: scene.height / scale, pixelsPerYard: scale,
    project: (point) => ({ x: screenCentreX + (point.x - centreX) * scale,
      y: screenCentreY - (point.y - centreY) * scale }),
    unproject: (point) => ({ x: centreX + (point.x - screenCentreX) / scale,
      y: centreY - (point.y - screenCentreY) / scale }),
  };
}
