/**
 * Maps an event to a contextual course scene (spec: play-event imagery). The backend supplies an authoritative
 * scene token (`courseType`) — authored for curated Pro events, derived from the host course's classification
 * for the Development tour — so location, name, and imagery always agree. Each scene has a small pool of square
 * photographs in /public/scenes; one is chosen deterministically per event so a given event always shows the
 * same backdrop.
 */

export type Scene = "parkland" | "links" | "desert" | "tropical" | "mountain" | "coastal";

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
  links: ["heath-2.jpg", "heath-4.jpg", "heath-5.jpg", "links-1.jpg", "links-2.jpg", "links-3.jpg"],
  desert: ["desert-1.jpg", "desert-2.jpg", "desert-3.jpg"],
  tropical: ["florida-1.jpg", "florida-2.jpg"],
  mountain: ["mountain-1.jpg", "mountain-2.jpg", "mountain-3.jpg"],
  coastal: ["cali-1.jpg", "cali-2.jpg"],
};

/**
 * The scene for an event, from the backend's authoritative `courseType` token (PARKLAND, LINKS, DESERT,
 * TROPICAL, MOUNTAIN, COASTAL). Defaults to Parkland — the majority case — for anything unrecognised.
 */
export function resolveScene(courseType?: string | null): Scene {
  switch ((courseType ?? "").toUpperCase()) {
    case "LINKS":
      return "links";
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

export type SceneBackdrop = { scene: Scene; src: string };

/**
 * Resolves an event to a concrete backdrop image. `seed` (the event's name) keeps the choice stable across
 * renders and rounds, so the scene is a consistent part of the event's identity.
 */
export function sceneBackdrop(courseType?: string | null, seed?: string | null): SceneBackdrop {
  const scene = resolveScene(courseType);
  const pool = SCENE_IMAGES[scene];
  const src = pool[hash(seed && seed.length > 0 ? seed : scene) % pool.length];
  return { scene, src: `/scenes/${src}` };
}
