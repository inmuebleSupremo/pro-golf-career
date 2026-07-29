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

export type Vegetation = "deciduous" | "pine" | "palm" | "yucca";

/** The style kit for one biome: palette, hazard/vegetation styling, and terrain cues. */
export interface BiomeKit {
  readonly biome: Biome;
  /** Out-of-play surround (the frame behind the hole). */
  readonly out: string;
  /** Primary rough band colour (ignored when `roughPattern` is set). */
  readonly rough: string;
  /** When set, the rough is filled with a generated texture instead of a solid colour. */
  readonly roughPattern: "fescue" | null;
  readonly fairway: string;
  /** Mower-stripe pair overlaid on the fairway; `null` for biomes that aren't striped (links, desert). */
  readonly mowStripe: readonly [string, string] | null;
  readonly green: string;
  readonly fringe: string;
  readonly sand: string;
  readonly sandStroke: string;
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
}

export const BIOME_KITS: Record<Biome, BiomeKit> = {
  parkland: {
    biome: "parkland",
    out: "#2a5f30", rough: "#3c8f42", roughPattern: null,
    fairway: "#57b855", mowStripe: ["#63c65e", "#54b350"],
    green: "#84d47f", fringe: "#5fae5b", sand: "#e9dcae", sandStroke: "rgba(60,45,10,.2)",
    potBunkers: false, water: "#2f7fb5",
    vegetation: "deciduous", vegetationDensity: 1, waste: false, rock: false, elevationShading: false,
  },
  links: {
    biome: "links",
    out: "#cdbb84", rough: "#cdbb84", roughPattern: "fescue",
    fairway: "#b7c48d", mowStripe: null,
    green: "#aebf86", fringe: "#94a06e", sand: "#efe6c4", sandStroke: "#7a6a3f",
    potBunkers: true, water: "#5a86a0",
    vegetation: "deciduous", vegetationDensity: 0.35, waste: false, rock: false, elevationShading: false,
  },
  desert: {
    biome: "desert",
    out: "#e4cea1", rough: "#d7be8a", roughPattern: null,
    fairway: "#5aa64a", mowStripe: null,
    green: "#7bbf62", fringe: "#5c9a48", sand: "#f3f1ea", sandStroke: "#d8cfa8",
    potBunkers: false, water: null,
    vegetation: "yucca", vegetationDensity: 0.4, waste: true, rock: false, elevationShading: false,
  },
  alpine: {
    biome: "alpine",
    out: "#20463a", rough: "#357a4b", roughPattern: null,
    fairway: "#4caf50", mowStripe: ["#54b95a", "#46a64c"],
    green: "#74c483", fringe: "#3f8a4d", sand: "#eeeeee", sandStroke: "#c9c1a4",
    potBunkers: false, water: "#2b6f9e",
    vegetation: "pine", vegetationDensity: 1, waste: false, rock: true, elevationShading: true,
  },
  heathland: {
    biome: "heathland",
    out: "#4a3c5c", rough: "#5a4a63", roughPattern: null,
    fairway: "#6f9e57", mowStripe: ["#79a860", "#67954f"],
    green: "#8ec079", fringe: "#5f8a4a", sand: "#d8c79a", sandStroke: "#6e5f3f",
    potBunkers: false, water: "#3f6a86",
    vegetation: "deciduous", vegetationDensity: 0.6, waste: false, rock: false, elevationShading: false,
  },
  tropical: {
    biome: "tropical",
    out: "#176d3d", rough: "#2f9455", roughPattern: null,
    fairway: "#31c766", mowStripe: ["#00e676", "#00c853"],
    green: "#8bef9d", fringe: "#3fae62", sand: "#efe3bf", sandStroke: "rgba(40,60,10,.18)",
    potBunkers: false, water: "#0277bd",
    vegetation: "palm", vegetationDensity: 1, waste: false, rock: false, elevationShading: false,
  },
};

/**
 * Resolves the schematic biome from the host course's environment classification (`courseType`). COASTAL maps
 * to the water-heavy Tropical look (LINKS already covers the seaside); an unknown token falls back to Parkland.
 */
export function resolveBiome(courseType?: string | null): Biome {
  switch ((courseType ?? "").toUpperCase()) {
    case "LINKS":
      return "links";
    case "DESERT":
      return "desert";
    case "MOUNTAIN":
      return "alpine";
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

/** A primitive shape within a foliage symbol, authored at the template's native coordinates. */
export type SvgPrim =
  | { readonly t: "path"; readonly d: string; readonly fill: string }
  | { readonly t: "circle"; readonly r: number; readonly fill: string }
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
};

/** Render scale applied to each foliage symbol (its native coords are template-sized). */
export const SYMBOL_SCALE: Record<Vegetation, number> = {
  deciduous: 0.3,
  pine: 0.24,
  palm: 0.26,
  yucca: 1,
};
