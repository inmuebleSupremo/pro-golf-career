import type { CanonicalPlayingGeometry, CanonicalRenderModel } from "./canonical-geometry";
import { pointInPolygon } from "./canonical-hole-decoration";

export type PremiumTree = Readonly<{
  x: number;
  y: number;
  size: number;
  rotation: number;
  variant: number;
  shade: number;
  opacity?: number;
}>;

export type PremiumTreePlan = Readonly<{
  regionIndex: number;
  trees: readonly PremiumTree[];
}>;

export type PremiumOpening = Readonly<{
  x: number; y: number; rx: number; ry: number; rotation: number;
}>;

/** Cosmetic crowns are reproducible and remain inside the backend's tree polygons. */
export function premiumTreePlans(
  geometry: CanonicalPlayingGeometry,
  layoutSeed: string,
  model: CanonicalRenderModel,
): readonly PremiumTreePlan[] {
  return geometry.regions.flatMap((region, regionIndex) => {
    if (region.surface !== "TREES" || region.boundary.length < 3) return [];
    const xs = region.boundary.map((point) => point.x);
    const ys = region.boundary.map((point) => point.y);
    const minX = Math.min(...xs);
    const maxX = Math.max(...xs);
    const minY = Math.min(...ys);
    const maxY = Math.max(...ys);
    const area = Math.abs(region.boundary.reduce((sum, point, index) => {
      const next = region.boundary[(index + 1) % region.boundary.length];
      return sum + point.x * next.y - next.x * point.y;
    }, 0) / 2);
    const target = Math.min(55, Math.max(7, Math.round(area / 21)));
    const random = seededRandom(`${layoutSeed}:premium-trees:${regionIndex}:${region.boundary.length}`);
    const trees: PremiumTree[] = [];
    const pixelsPerYard = Math.abs(model.project({ x: minX + 1, y: minY }).x - model.project({ x: minX, y: minY }).x);

    for (let attempt = 0; attempt < target * 55 && trees.length < target; attempt += 1) {
      const world = { x: minX + random() * (maxX - minX), y: minY + random() * (maxY - minY) };
      if (!pointInPolygon(world, region.boundary)) continue;
      const screen = model.project(world);
      const size = Math.max(3.5, Math.min(9, (4.6 + random() * 3.6) * pixelsPerYard));
      if (trees.some((tree) => Math.hypot(tree.x - screen.x, tree.y - screen.y) < (tree.size + size) * .38)) continue;
      trees.push({ x: screen.x, y: screen.y, size, rotation: random() * 360,
        variant: Math.floor(random() * 6), shade: random() });
    }
    return [{ regionIndex, trees }];
  });
}

/** Broad grassland breaks up the cosmetic woodland without adding a gameplay surface. */
export function premiumOpenings(layoutSeed: string, left: number, width: number, height: number): readonly PremiumOpening[] {
  const random = seededRandom(`${layoutSeed}:premium-openings`);
  return Array.from({ length: 7 }, (_, index) => ({
    x: index % 2 === 0 ? left + 18 + random() * 70 : left + width - 18 - random() * 70,
    y: 22 + random() * (height - 44),
    rx: 24 + random() * 22,
    ry: 28 + random() * 37,
    rotation: -35 + random() * 70,
  }));
}

/** Cosmetic woodland is seeded beyond the hole, with openings and uneven group density. */
export function premiumDistantTrees(
  geometry: CanonicalPlayingGeometry,
  layoutSeed: string,
  model: CanonicalRenderModel,
  left: number,
  width: number,
  height: number,
  maxTrees = 470,
): readonly PremiumTree[] {
  const random = seededRandom(`${layoutSeed}:premium-surroundings`);
  const wideStage = width > 550;
  const openings = wideStage ? [] : premiumOpenings(layoutSeed, left, width, height);
  const phase = random() * Math.PI * 2;
  const woodlandAnchors = geometry.regions.filter((region) => region.surface === "TREES" && region.boundary.length >= 3)
    .map((region) => model.project({
      x: region.boundary.reduce((sum, point) => sum + point.x, 0) / region.boundary.length,
      y: region.boundary.reduce((sum, point) => sum + point.y, 0) / region.boundary.length,
    }));
  // Wider stages gain irregular actual canopy groups, rather than smooth dark grove ellipses.
  const propertyAnchors = wideStage ? [-1, 1].flatMap((side) => [0, 1].map((row) => ({
    x: side < 0 ? -80 - random() * Math.max(20, -left - 115)
      : 300 + random() * Math.max(20, left + width - 335),
    y: height * (.2 + row * .43 + random() * .17),
  }))) : [];
  const trees: PremiumTree[] = [];
  for (let attempt = 0; attempt < (wideStage ? 12000 : 10000) && trees.length < maxTrees; attempt += 1) {
    const propertyAnchor = propertyAnchors.length > 0 && random() < .52
      ? propertyAnchors[Math.floor(random() * propertyAnchors.length)] : null;
    const anchor = !propertyAnchor && woodlandAnchors.length > 0 && random() < (wideStage ? .65 : .58)
      ? woodlandAnchors[Math.floor(random() * woodlandAnchors.length)] : null;
    const outward = anchor && anchor.x < left + width / 2 ? -1 : 1;
    const x = propertyAnchor ? propertyAnchor.x + (random() + random() + random() - 1.5) * 64
      : anchor ? anchor.x + outward * (15 + random() * 48) + (random() - .5) * 36 : left + random() * width;
    const y = propertyAnchor ? propertyAnchor.y + (random() + random() + random() - 1.5) * 76
      : anchor ? anchor.y + (random() + random() - 1) * 85 : random() * height;
    if (x < left || x > left + width || y < 0 || y > height) continue;
    if (pointInPolygon(model.unproject({ x, y }), geometry.playableBoundary)) continue;
    if (openings.some((opening) => {
      const angle = opening.rotation * Math.PI / 180;
      const dx = x - opening.x;
      const dy = y - opening.y;
      const u = (dx * Math.cos(angle) + dy * Math.sin(angle)) / opening.rx;
      const v = (dy * Math.cos(angle) - dx * Math.sin(angle)) / opening.ry;
      return u * u + v * v < .84;
    })) continue;
    const density = .53 + .23 * Math.sin(x * .039 + phase) * Math.cos(y * .027 - phase)
      + .15 * Math.sin((x + y) * .021 + phase);
    if (random() > density) continue;
    const distance = Math.min(1, Math.abs(x - (left + width / 2)) / (width / 2));
    const size = (propertyAnchor ? 7.5 + random() * 5.5 : 4.8 + random() * 5.2) * (1 - distance * .18);
    if (trees.some((tree) => Math.hypot(tree.x - x, tree.y - y) < (tree.size + size) * .39)) continue;
    const edgeDistance = Math.min(x - left, left + width - x);
    const opacity = wideStage ? Math.min(1, Math.max(0, edgeDistance / (width * .18))) : undefined;
    trees.push({ x, y, size, rotation: random() * 360, variant: Math.floor(random() * 6), shade: random(),
      ...(wideStage ? { opacity } : {}) });
  }
  return trees;
}

/** SVG painter's order matching CourseGeometry.precedence: large number first, priority last. */
export function premiumSurfacePrecedence(surface: string): number {
  switch (surface) {
    case "WATER": case "OUT_OF_BOUNDS": return 0;
    case "BUNKER": case "WASTE_AREA": return 1;
    case "GREEN": return 2;
    case "FRINGE": return 3;
    case "TREES": case "RECOVERY_AREA": return 4;
    case "FAIRWAY": return 5;
    case "FIRST_CUT": return 6;
    case "PRIMARY_ROUGH": return 7;
    case "DEEP_ROUGH": return 8;
    case "TEE_BOX": return 9;
    default: return 10;
  }
}

export function seededRandom(value: string): () => number {
  let state = 2166136261;
  for (let index = 0; index < value.length; index += 1) {
    state ^= value.charCodeAt(index);
    state = Math.imul(state, 16777619);
  }
  return () => {
    state += 0x6d2b79f5;
    let result = state;
    result = Math.imul(result ^ (result >>> 15), result | 1);
    result ^= result + Math.imul(result ^ (result >>> 7), result | 61);
    return ((result ^ (result >>> 14)) >>> 0) / 4294967296;
  };
}
