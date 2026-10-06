/**
 * Maps an event to a contextual course scene (spec: play-event imagery). The backend supplies an authoritative
 * scene token (`courseType`) — authored for curated Pro events, derived from the host course's classification
 * for the Development tour — so location, name, and imagery always agree. Each scene has a small pool of square
 * photographs in /public/scenes; one is chosen deterministically per event so a given event always shows the
 * same backdrop. The four majors have their own photo pools, selected deterministically from the season and
 * round so repeated appearances retain their identity while still gaining visual variety.
 */

export type Scene = "parkland" | "links" | "desert" | "tropical" | "mountain" | "coastal" | "heathland";

/** The photographs available for each scene, relative to /public/scenes. All are square. */
const SCENE_IMAGES: Record<Scene, readonly string[]> = {
  parkland: [
    "parkland-1.jpg",
    "parkland-2.jpg",
    "parkland-3.jpg",
    "parkland-4.jpg",
    "parkland-5.jpg",
    "parkland-6.jpg",
    "parkland-7.jpg",
  ],
  links: ["links-1.jpg", "links-2.jpg", "links-3.jpg"],
  heathland: ["heath-2.jpg", "heath-4.jpg", "heath-5.jpg"],
  desert: ["desert-1.jpg", "desert-2.jpg", "desert-3.jpg"],
  tropical: ["florida-1.jpg", "florida-2.jpg"],
  mountain: ["mountain-1.jpg", "mountain-2.jpg", "mountain-3.jpg"],
  coastal: ["cali-1.jpg", "cali-2.jpg"],
};

/** Bespoke photographs for the four named majors, relative to /public/scenes. */
const MAJOR_TOURNAMENT_IMAGES = {
  "The Grandmaster Invitational": [
    "grandmaster-invitational-1.jpg",
    "grandmaster-invitational-2.jpg",
    "grandmaster-invitational-3.jpg",
    "grandmaster-invitational-4.jpg",
    "grandmaster-invitational-5.jpg",
    "grandmaster-invitational-6.jpg",
    "grandmaster-invitational-7.jpg",
    "grandmaster-invitational-8.jpg",
    "grandmaster-invitational-9.jpg",
    "grandmaster-invitational-10.jpg",
    "grandmaster-invitational-11.jpg",
  ],
  "The National Open": [
    "national-open-1.jpg",
    "national-open-2.jpg",
    "national-open-3.jpg",
    "national-open-4.jpg",
    "national-open-5.jpg",
    "national-open-7.jpg",
    "national-open-8.jpg",
    "national-open-9.jpg",
    "national-open-10.jpg",
  ],
  "The Seaside Open": [
    "seaside-open-1.jpg",
    "seaside-open-2.jpg",
    "seaside-open-3.jpg",
    "seaside-open-4.jpg",
    "seaside-open-5.jpg",
    "seaside-open-6.jpg",
    "seaside-open-7.jpg",
    "seaside-open-8.jpg",
    "seaside-open-9.jpg",
    "seaside-open-10.jpg",
    "seaside-open-11.jpg",
    "seaside-open-12.jpg",
  ],
  "The Continental Championship": [
    "continental-championship-1.jpg",
    "continental-championship-2.jpg",
    "continental-championship-3.jpg",
    "continental-championship-4.jpg",
    "continental-championship-5.jpg",
    "continental-championship-6.jpg",
    "continental-championship-7.jpg",
    "continental-championship-8.jpg",
    "continental-championship-9.jpg",
  ],
} as const satisfies Record<string, readonly string[]>;

/**
 * The scene for an event, from the backend's authoritative `courseType` token (PARKLAND, LINKS, DESERT,
 * TROPICAL, MOUNTAIN, COASTAL, HEATHLAND). The legacy WOODLAND token still resolves to heathland. Defaults to
 * Parkland — the majority case — for anything unrecognised.
 */
export function resolveScene(courseType?: string | null): Scene {
  switch ((courseType ?? "").toUpperCase()) {
    case "LINKS":
      return "links";
    case "HEATHLAND":
    case "WOODLAND":
      return "heathland";
    case "DESERT":
      return "desert";
    case "TROPICAL":
      return "tropical";
    case "MOUNTAIN":
      return "mountain";
    case "COASTAL":
      return "coastal";
    case "PARKLAND":
    default:
      return "parkland";
  }
}

/** A small, stable string hash (FNV-1a-ish) so image selection is deterministic per event. */
function hash(key: string): number {
  let h = 2166136261;
  for (let i = 0; i < key.length; i++) {
    h ^= key.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

export type SceneBackdrop = { scene: Scene; src: string; fallbackSrc?: string };

export type BackdropRotation = { season?: number | null; round?: number | null };

function imagePath(image: string): string {
  return `/scenes/${image}`;
}

function majorImagePool(tournamentName?: string | null): readonly string[] | undefined {
  if (!tournamentName) return undefined;
  return MAJOR_TOURNAMENT_IMAGES[tournamentName as keyof typeof MAJOR_TOURNAMENT_IMAGES];
}

function rotationOffset({ season, round }: BackdropRotation): number {
  return Math.max(0, season ?? 0) + Math.max(0, round ?? 0);
}

/** Replaces a failed bespoke image with the event's normal course-scene selection exactly once. */
export function applySceneFallback(image: HTMLImageElement, fallbackSrc: string): void {
  if (image.dataset.sceneFallbackApplied === "true") return;
  image.dataset.sceneFallbackApplied = "true";
  image.src = fallbackSrc;
}

/**
 * Resolves an event to a concrete backdrop image. Normal events retain their existing stable selection from
 * `seed`. Named majors rotate through their bespoke pools using their season and round, while retaining the
 * standard scene image as a load-error fallback.
 */
export function sceneBackdrop(
  courseType?: string | null,
  seed?: string | null,
  rotation: BackdropRotation = {},
): SceneBackdrop {
  const scene = resolveScene(courseType);
  const scenePool = SCENE_IMAGES[scene];
  const sceneImage = scenePool[hash(seed && seed.length > 0 ? seed : scene) % scenePool.length];
  const majorPool = majorImagePool(seed);

  if (!majorPool) return { scene, src: imagePath(sceneImage) };

  const majorImage = majorPool[(hash(seed ?? scene) + rotationOffset(rotation)) % majorPool.length];
  return { scene, src: imagePath(majorImage), fallbackSrc: imagePath(sceneImage) };
}
