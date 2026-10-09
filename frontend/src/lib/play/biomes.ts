/**
 * Biome style kits for the 2D hole schematic (spec: web-hole-visualization).
 *
 * This is the single source of truth for the **golf-course illustration palette** and the reusable vector
 * symbols ported from the authored templates in `docs/course-holes/*.svg`. It is deliberately separate from
 * the app's UI design tokens: those govern the surrounding chrome (cards, captions, controls) and MUST be used
 * there; these are domain illustration colours for the drawing itself (grass, sand, water, foliage), centralized
 * here so no component ever hardcodes them. Components consume a `BiomeKit` and the symbol data — they invent
 * no colours of their own.
 *
 * The biome for a hole is resolved from the host course's `EnvironmentClassification` (`courseType`). The
 * mapping is the authority for the schematic (it differs from `scene.ts`, which picks a backdrop photo):
 * COASTAL → a water-heavy Tropical look, since LINKS already carries the seaside-links styling.
 */

export type Biome = "parkland" | "links" | "desert" | "alpine" | "heathland" | "tropical";

export type Vegetation =
  | "deciduous"
  | "pine"
  | "palm"
  | "yucca"
  | "saguaro"
  | "barrel"
  | "scrub"
  | "rock"
  | "heather"
  | "gorse"
  | "birch";

/** The style kit for one biome: palette, hazard/vegetation styling, and terrain cues. */
export interface BiomeKit {
  readonly biome: Biome;
  /** Out-of-play surround (the frame behind the hole). */
  readonly out: string;
  /** Primary rough band colour (ignored when `roughPattern` is set). */
  readonly rough: string;
  /** When set, the rough is filled with a generated texture instead of a solid colour. */
  readonly roughPattern: "fescue" | "heather" | null;
  readonly fairway: string;
  /** Mower-stripe pair overlaid on the fairway; `null` for biomes that aren't striped (links, desert). */
  readonly mowStripe: readonly [string, string] | null;
  readonly green: string;
  readonly fringe: string;
  readonly sand: string;
  readonly sandStroke: string;
  /** Canonical tree/recovery-canopy fill, kept in the same environment palette as the terrain. */
  readonly tree: string;
  /** Pot bunkers (small, deep, ringed — links) vs. flashed bunkers. */
  readonly potBunkers: boolean;
  /** Water hazard colour, or `null` for biomes without water (desert). */
  readonly water: string | null;
  readonly vegetation: Vegetation;
  /** Relative tree/flora density in [0,1] — parkland dense, links sparse, desert scattered. */
  readonly vegetationDensity: number;
  /** Desert waste-area flash: a bright pale edge between fairway and surround. */
  readonly waste: boolean;
  /** Mountain rock outcrops at the frame edges. */
  readonly rock: boolean;
  /** Alpine elevation shading (light high / dark low). */
  readonly elevationShading: boolean;

  // --- restyle: planting, tee, mow, and biome features ---
  /** Tee-marker colour — one per course type (identity is between courses). */
  readonly tee: string;
  /** How scatter (flora / rock) fills the hole: dense forest, medium scatter, sparse edges, or none. */
  readonly scatterMode: "forest" | "scatter" | "edge" | "none";
  /** Placement cull in [0,1]; higher is sparser. */
  readonly scatterCull: number;
  /** Weighted scatter kinds — the biome's cohesive plant/rock family. */
  readonly scatter: readonly { readonly kind: Vegetation; readonly weight: number }[];
  /** Fairway mow style. */
  readonly mowKind: "horizontal" | "diagonal" | "premium" | null;
  /** Biome-specific rendering features (used by later stages). */
  readonly walls?: boolean; // mountain slate cliffs
  readonly waterfall?: boolean; // mountain cliff waterfalls
  readonly tiered?: boolean; // mountain multi-layer green
  readonly arroyo?: boolean; // desert dry washes
  readonly grain?: boolean; // desert sand texture
  readonly dryGreen?: boolean; // desert burnt radial green
  readonly coastal?: boolean; // links sea margins
  readonly island?: boolean; // tropical island fairways
  readonly ocean?: boolean; // tropical ocean surround
  /** Faint topographic contour lines in the surround (undulating dunes/heath); the stroke colour, or absent. */
  readonly contour?: string;
}

export const BIOME_KITS: Record<Biome, BiomeKit> = {
  parkland: {
    biome: "parkland",
    out: "#2a5f30", rough: "#3c8f42", roughPattern: null,
    fairway: "#57b855", mowStripe: ["#63c65e", "#54b350"],
    green: "#84d47f", fringe: "#5fae5b", sand: "#e9dcae", sandStroke: "rgba(60,45,10,.2)", tree: "#245b2c",
    potBunkers: false, water: "#2f7fb5",
    vegetation: "deciduous", vegetationDensity: 1, waste: false, rock: false, elevationShading: false,
    tee: "#eef0f2", scatterMode: "forest", scatterCull: 0.16, scatter: [{ kind: "deciduous", weight: 1 }], mowKind: "horizontal",
  },
  // Links keeps its dusty seaside identity in the SURROUND and rough (khaki fescue), but the playing surfaces are
  // lifted to an irrigated green — the world's best links are watered, so the fairway/green read lush against the
  // wild dusty fescue (the same lush-turf-vs-dry-surround contrast that makes desert & parkland pop).
  links: {
    biome: "links",
    out: "#c6bd80", rough: "#c6bd80", roughPattern: "fescue",
    fairway: "#93bd66", mowStripe: null,
    green: "#88b860", fringe: "#6c974e", sand: "#efe6c4", sandStroke: "#7a6a3f", tree: "#5e713f",
    potBunkers: true, water: "#5a86a0",
    vegetation: "deciduous", vegetationDensity: 0.35, waste: false, rock: false, elevationShading: false,
    tee: "#d4af37", scatterMode: "edge", scatterCull: 0.5, scatter: [{ kind: "gorse", weight: 1 }], mowKind: null, coastal: true,
    contour: "#b09b6a",
  },
  desert: {
    biome: "desert",
    out: "#e4cea1", rough: "#d7be8a", roughPattern: null,
    fairway: "#5aa64a", mowStripe: null,
    green: "#7bbf62", fringe: "#5c9a48", sand: "#f3f1ea", sandStroke: "#d8cfa8", tree: "#5a7c3f",
    potBunkers: false, water: null,
    vegetation: "yucca", vegetationDensity: 0.4, waste: true, rock: false, elevationShading: false,
    tee: "#c1502e", scatterMode: "scatter", scatterCull: 0.7, mowKind: "diagonal", arroyo: true, grain: true, dryGreen: true,
    scatter: [{ kind: "saguaro", weight: 0.42 }, { kind: "barrel", weight: 0.2 }, { kind: "scrub", weight: 0.2 }, { kind: "rock", weight: 0.18 }],
  },
  alpine: {
    biome: "alpine",
    out: "#20463a", rough: "#357a4b", roughPattern: null,
    fairway: "#4caf50", mowStripe: ["#54b95a", "#46a64c"],
    green: "#74c483", fringe: "#3f8a4d", sand: "#eeeeee", sandStroke: "#c9c1a4", tree: "#184a3c",
    potBunkers: false, water: "#2b6f9e",
    vegetation: "pine", vegetationDensity: 1, waste: false, rock: true, elevationShading: true,
    tee: "#3b6fd4", scatterMode: "forest", scatterCull: 0.12, scatter: [{ kind: "pine", weight: 1 }], mowKind: "premium",
    walls: true, waterfall: true, tiered: true,
  },
  // Heathland reads khaki/tan with heather flecks (docs/course-holes/heathland001.svg), NOT a purple field: the
  // surround is a solid khaki frame and the rough carries a generated heather texture (sparse purple/green over
  // olive), so the heather is a scattered accent rather than the dominant colour.
  heathland: {
    biome: "heathland",
    out: "#949a68", rough: "#828f56", roughPattern: "heather",
    fairway: "#68a850", mowStripe: ["#72b25a", "#61984c"],
    green: "#83bb63", fringe: "#58893f", sand: "#ddceac", sandStroke: "#8a765a", tree: "#4f633c",
    potBunkers: false, water: "#3f6a86",
    vegetation: "deciduous", vegetationDensity: 0.6, waste: false, rock: false, elevationShading: false,
    tee: "#8f7268", scatterMode: "scatter", scatterCull: 0.42, mowKind: "premium", contour: "#5f6b3c",
    scatter: [{ kind: "heather", weight: 0.52 }, { kind: "gorse", weight: 0.4 }, { kind: "pine", weight: 0.08 }],
  },
  tropical: {
    biome: "tropical",
    out: "#0d84b8", rough: "#3fa85a", roughPattern: null,
    fairway: "#31c766", mowStripe: ["#00e676", "#00c853"],
    green: "#8bef9d", fringe: "#3fae62", sand: "#efe3bf", sandStroke: "rgba(40,60,10,.18)", tree: "#176b43",
    potBunkers: false, water: "#0277bd",
    vegetation: "palm", vegetationDensity: 1, waste: false, rock: false, elevationShading: false,
    tee: "#00bcd4", scatterMode: "edge", scatterCull: 0.18, scatter: [{ kind: "palm", weight: 1 }], mowKind: "diagonal",
    island: true, ocean: true,
  },
};

/**
 * Resolves the schematic biome from the event's canonical scene token (`courseType`) — the same token the scene
 * backdrop keys on, so the hole illustration always agrees with the event's photo, name, and place. HEATHLAND
 * (the presentation face of the sim's WOODLAND) gets its khaki-heather look; COASTAL maps to the water-heavy
 * Tropical look (LINKS already covers the seaside). The legacy WOODLAND token is still accepted as a safety net.
 * An unknown token falls back to Parkland.
 */
export function resolveBiome(courseType?: string | null): Biome {
  switch ((courseType ?? "").toUpperCase()) {
    case "LINKS":
      return "links";
    case "DESERT":
      return "desert";
    case "MOUNTAIN":
      return "alpine";
    case "HEATHLAND":
    case "WOODLAND":
      return "heathland";
    case "COASTAL":
    case "TROPICAL":
      return "tropical";
    case "PARKLAND":
    default:
      return "parkland";
  }
}

// --- Reusable vector symbols ported from the authored templates (docs/course-holes/*.svg) ---

/** A primitive shape within a foliage/scatter symbol, authored at the template's native coordinates. */
export type SvgPrim =
  | { readonly t: "path"; readonly d: string; readonly fill: string; readonly opacity?: number }
  | { readonly t: "circle"; readonly r: number; readonly fill: string; readonly cx?: number; readonly cy?: number; readonly opacity?: number }
  | { readonly t: "ellipse"; readonly rx: number; readonly ry: number; readonly fill: string; readonly cx?: number; readonly cy?: number; readonly opacity?: number }
  | { readonly t: "stroke"; readonly d: string; readonly stroke: string; readonly width: number };

/** The foliage symbols, keyed by vegetation kind. Rendered scaled by {@link SYMBOL_SCALE}. */
export const VEG_SYMBOLS: Record<Vegetation, readonly SvgPrim[]> = {
  // Parkland lobed deciduous canopy (two layers), from parkland001.svg.
  deciduous: [
    { t: "path", fill: "#1b5e20", d: "M 0 -22 C 15 -27,28 -12,22 5 C 32 15,22 32,5 28 C -8 38,-25 28,-28 12 C -38 -5,-22 -25,-5 -18 C -5 -28,0 -28,0 -22 Z" },
    { t: "path", fill: "#2e7d32", d: "M -2 -16 C 8 -20,18 -8,14 2 C 22 10,14 20,2 18 C -6 25,-16 18,-18 6 C -25 -2,-14 -16,-2 -12 C -2 -20,-2 -20,-2 -16 Z" },
  ],
  // Alpine jagged pine (two layers), from mountain001.svg.
  pine: [
    { t: "path", fill: "#004d40", d: "M 0 -35 L 12 -10 L 5 -10 L 15 10 L 7 10 L 20 30 L -20 30 L -7 10 L -15 10 L -5 -10 L -12 -10 Z" },
    { t: "path", fill: "#00695c", d: "M 0 -25 L 8 -5 L 3 -5 L 10 10 L -10 10 L -3 -5 L -8 -5 Z" },
  ],
  // Tropical palm (trunk knot + fronds + crown), from tropical001.svg.
  palm: [
    { t: "circle", r: 4, fill: "#4e3629" },
    { t: "path", fill: "#1b5e20", d: "M 0 0 C -8 -8,-18 -12,-28 -8 C -18 -4,-8 -2,0 0" },
    { t: "path", fill: "#1b5e20", d: "M 0 0 C 8 -8,18 -12,28 -8 C 18 -4,8 -2,0 0" },
    { t: "path", fill: "#0d5316", d: "M 0 0 C 12 -2,22 4,26 14 C 16 10,8 4,0 0" },
    { t: "path", fill: "#0d5316", d: "M 0 0 C -12 -2,-22 4,-26 14 C -16 10,-8 4,0 0" },
    { t: "path", fill: "#2e7d32", d: "M 0 0 C 8 8,12 20,6 28 C 4 18,2 8,0 0" },
    { t: "path", fill: "#2e7d32", d: "M 0 0 C -8 8,-12 20,-6 28 C -4 18,-2 8,0 0" },
    { t: "path", fill: "#1b5e20", d: "M 0 0 C -2 -12,2 -24,10 -28 C 6 -18,2 -8,0 0" },
    { t: "circle", r: 2.5, fill: "#81c784" },
  ],
  // Desert yucca/scrub — a spray of blades.
  yucca: [
    { t: "stroke", stroke: "#7f8a4a", width: 1.4, d: "M0 3 L-6 -9 M0 3 L-3 -12 M0 3 L0 -14 M0 3 L3 -12 M0 3 L6 -9" },
  ],
  // Desert saguaro cactus (trunk + two arms).
  saguaro: [
    { t: "stroke", stroke: "#4e7d3a", width: 2.6, d: "M0 3 L0 -13" },
    { t: "stroke", stroke: "#4e7d3a", width: 1.9, d: "M0 -6 L-3.4 -6 L-3.4 -10.5" },
    { t: "stroke", stroke: "#4e7d3a", width: 1.9, d: "M0 -8 L3.4 -8 L3.4 -12" },
  ],
  // Desert barrel cactus (ribbed body + flower buds).
  barrel: [
    { t: "ellipse", rx: 4.4, ry: 5.6, fill: "#5a8a3f" },
    { t: "stroke", stroke: "#3c6a2a", width: 0.6, d: "M0 -5 L0 5 M-2.4 -4.6 L-2.4 4.6 M2.4 -4.6 L2.4 4.6" },
    { t: "circle", cx: -1.4, cy: -5.4, r: 1, fill: "#e6a13a" },
    { t: "circle", cx: 1.6, cy: -5, r: 0.9, fill: "#e6a13a" },
  ],
  // Low desert/heath scrub bush.
  scrub: [
    { t: "circle", cx: -3, cy: 0.6, r: 3.1, fill: "#94a05f" },
    { t: "circle", cx: 2.6, cy: -0.8, r: 2.7, fill: "#94a05f" },
    { t: "circle", cx: 0.3, cy: 2.4, r: 2.5, fill: "#94a05f" },
  ],
  // Desert/mountain boulder (lit facet + shadow).
  rock: [
    { t: "path", fill: "#b8a184", d: "M-6 3 L-4 -3.5 L2 -5 L6 -1 L4 4 L-2 5 Z" },
    { t: "path", fill: "#cdba98", d: "M-4 -3.5 L2 -5 L3 -1 L-2 0 Z" },
    { t: "path", fill: "#8c7a5d", opacity: 0.6, d: "M-6 3 L-2 5 L4 4 L3 5.5 L-4 5 Z" },
  ],
  // Heathland low heather mound — cohesive olive/brush-brown, only a whisper of colour.
  heather: [
    { t: "ellipse", cx: 0, cy: 1, rx: 8, ry: 4.4, fill: "#697c42" },
    { t: "ellipse", cx: -3, cy: 0, rx: 4.6, ry: 3, fill: "#7c8a57" },
    { t: "ellipse", cx: 3.5, cy: 1.5, rx: 4, ry: 2.6, fill: "#8b7a52", opacity: 0.8 },
    { t: "circle", cx: -2, cy: -1, r: 0.8, fill: "#9a8a5e", opacity: 0.7 },
    { t: "circle", cx: 3, cy: 1, r: 0.8, fill: "#9a8a5e", opacity: 0.7 },
  ],
  // Gorse bush (links/heathland) — green mound with muted flower dots.
  gorse: [
    { t: "circle", cx: -3, cy: 0.6, r: 3.4, fill: "#3f5d3a" },
    { t: "circle", cx: 2.8, cy: -0.8, r: 3, fill: "#4d6e42" },
    { t: "circle", cx: 0.4, cy: 2.4, r: 2.7, fill: "#3f5d3a" },
    { t: "circle", cx: -2, cy: -1, r: 0.9, fill: "#d4b43a" },
    { t: "circle", cx: 2.6, cy: 1.2, r: 0.8, fill: "#d4b43a" },
  ],
  // Silver birch (heathland specimen) — pale trunk + airy canopy.
  birch: [
    { t: "stroke", stroke: "#d8d2c2", width: 1.4, d: "M0 4 L-0.6 -8" },
    { t: "stroke", stroke: "#d8d2c2", width: 0.8, d: "M0 -2 L-3 -5 M0 0 L3 -3" },
    { t: "ellipse", cx: 0, cy: -11, rx: 8, ry: 7.5, fill: "#6fa851" },
    { t: "ellipse", cx: -2, cy: -13, rx: 5, ry: 4.6, fill: "#8fc76e" },
  ],
};

/** Render scale applied to each foliage symbol (its native coords are template-sized). */
export const SYMBOL_SCALE: Record<Vegetation, number> = {
  deciduous: 0.3,
  pine: 0.3,
  palm: 0.5,
  yucca: 1,
  saguaro: 0.5,
  barrel: 0.9,
  scrub: 1,
  rock: 0.9,
  heather: 0.9,
  gorse: 1,
  birch: 0.6,
};
