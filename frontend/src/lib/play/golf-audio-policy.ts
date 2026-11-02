import type { HoleGeom } from "./hole-geometry";

export type ImpactCategory = "driver" | "wood" | "iron" | "wedge" | "putt";

const CLUB_CATEGORY: Record<string, ImpactCategory> = {
  DRIVER: "driver",
  THREE_WOOD: "wood", FIVE_WOOD: "wood", THREE_HYBRID: "wood", FOUR_HYBRID: "wood",
  FOUR_IRON: "iron", FIVE_IRON: "iron", SIX_IRON: "iron", SEVEN_IRON: "iron",
  EIGHT_IRON: "iron", NINE_IRON: "iron",
  PITCHING_WEDGE: "wedge", GAP_WEDGE: "wedge", SAND_WEDGE: "wedge",
  PUTTER: "putt",
};

const ROOT = "/audio/golf-v1/";
export const IMPACT_SAMPLES: Record<ImpactCategory, readonly string[]> = {
  driver: [`${ROOT}618277__yukon456__drive_11_dink.wav`],
  wood: [`${ROOT}wood_golf_12.mp3`, `${ROOT}wood_golf_3.mp3`],
  iron: [`${ROOT}iron_golf_9.mp3`, `${ROOT}iron_golf_5.wav`],
  wedge: [`${ROOT}wedge_floorball.wav`, `${ROOT}wedge_golf_shot.wav`],
  putt: [`${ROOT}putt_hit_01.mp3`, `${ROOT}putt_hit_03.mp3`, `${ROOT}putt_hit_05.mp3`],
};
export const CUP_SAMPLE = `${ROOT}cup_vase_drop.mp3`;

export type LandingCategory = "fairway" | "green" | "rough" | "bunker" | "water";
const LANDING_ROOT = "/audio/golf-v2/landing/";
export const LANDING_SAMPLES: Record<LandingCategory, readonly string[]> = {
  fairway: Array.from({ length: 24 }, (_, index) =>
    `${LANDING_ROOT}fairway/fairway_${String(index + 1).padStart(2, "0")}.mp3`),
  green: [`${LANDING_ROOT}green/green_01.wav`, `${LANDING_ROOT}green/green_02_experimental.wav`],
  rough: [`${LANDING_ROOT}rough/rough_01.mp3`],
  bunker: Array.from({ length: 3 }, (_, index) => `${LANDING_ROOT}bunker/bunker_0${index + 1}.mp3`),
  water: Array.from({ length: 3 }, (_, index) => `${LANDING_ROOT}water/water_0${index + 1}.mp3`),
};

/** The trace contact is the first landing surface, distinct from the ball's final lie. */
export function landingCategory(surface: string | null | undefined): LandingCategory | null {
  switch (surface) {
    case "FAIRWAY": return "fairway";
    case "GREEN": return "green";
    case "PRIMARY_ROUGH":
    case "DEEP_ROUGH": return "rough";
    case "BUNKER": return "bunker";
    case "WATER": return "water";
    default: return null;
  }
}

export function impactCategory(club: string | null | undefined, technique?: string): ImpactCategory | null {
  if (technique === "PUTT") return "putt";
  return club ? CLUB_CATEGORY[club] ?? null : null;
}

export type AmbienceFamily = "parkland" | "links" | "desert";

export function ambienceFamily(hole: HoleGeom | null): AmbienceFamily {
  // courseType is the rendered scene token, including historical Woodland -> Heathland mapping.
  switch (hole?.courseType?.toUpperCase()) {
    case "LINKS": return "links";
    case "DESERT": return "desert";
    default: return "parkland";
  }
}

export const AMBIENCE_SAMPLES: Record<AmbienceFamily, string> = {
  parkland: `${ROOT}ambience_golf_course_preview.mp3`,
  links: `${ROOT}ambience_links_wind.mp3`,
  desert: `${ROOT}ambience_desert_insects_trimmed.mp3`,
};

/** Per-category shuffle bag; refills avoid repeats when a category has alternative takes. */
export class SampleBag {
  private remaining: string[] = [];
  private previous: string | null = null;

  constructor(private readonly samples: readonly string[], private readonly random: () => number = Math.random) { }

  next(): string {
    if (this.samples.length === 0) throw new Error("No audio samples");
    if (this.remaining.length === 0) {
      this.remaining = [...this.samples];
      for (let i = this.remaining.length - 1; i > 0; i--) {
        const j = Math.floor(this.random() * (i + 1));
        [this.remaining[i], this.remaining[j]] = [this.remaining[j], this.remaining[i]];
      }
      if (this.remaining.length > 1 && this.remaining.at(-1) === this.previous) {
        [this.remaining[0], this.remaining[this.remaining.length - 1]] =
          [this.remaining[this.remaining.length - 1], this.remaining[0]];
      }
    }
    const sample = this.remaining.pop()!;
    this.previous = sample;
    return sample;
  }
}
