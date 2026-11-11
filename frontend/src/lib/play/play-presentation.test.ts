import { describe, expect, it } from "vitest";

import type { HoleGeom } from "./hole-geometry";
import { defaultPlayPresentation, playPresentation, tournamentPresentationHole } from "./play-presentation";
import { resolveBiome } from "./biomes";
import { resolveScene } from "./scene";

const generator8Parkland: HoleGeom = {
  holeNumber: 1, par: 4, length: 410, fairwayHalfWidth: 20, greenHalfWidth: 12,
  greenDepth: 22, elevationDelta: 0, hasGreensideBunker: false, hasWater: false,
  hasTrees: true, pinLateral: 0, pinDepth: 0, courseType: "PARKLAND",
  canonicalBiome: "PARKLAND", courseFamily: "PARKLAND", courseGeneratorVersion: 8,
  canonicalGeometrySource: "GENERATOR_8_PRINCIPLED_ARCHITECTURE", layoutSeed: "123",
  effectiveWind: { x: 0, y: 0, magnitude: 0, unit: "EFFECTIVE_YARDS" },
  geometry: {
    tee: { x: 0, y: 0 }, cup: { x: 0, y: 410 },
    playableBoundary: [{ x: -40, y: -10 }, { x: 40, y: -10 }, { x: 0, y: 430 }],
    regions: [],
  },
};

describe("normal play presentation", () => {
  it("uses a supported host family for curated scenes without changing geometry", () => {
    const green = { surface: "GREEN", boundary: [
      { x: -10, y: 390 }, { x: 10, y: 390 }, { x: 0, y: 410 },
    ] };
    const cases = [
      { scene: "COASTAL", family: "PARKLAND", biome: "PARKLAND", expected: "PARKLAND" },
      { scene: "TROPICAL", family: "PARKLAND", biome: "WOODLAND", expected: "WOODLAND" },
      { scene: "TROPICAL", family: "DESERT", biome: "DESERT", expected: "DESERT" },
      { scene: "COASTAL", family: "HEATHLAND", biome: "HEATHLAND", expected: "HEATHLAND" },
      { scene: "PARKLAND", family: "LINKS", biome: "LINKS", expected: "LINKS" },
    ];
    for (const { scene, family, biome, expected } of cases) {
      for (const version of [8, 9]) {
        const raw = { ...generator8Parkland, courseType: scene, courseFamily: family,
          canonicalBiome: biome, courseGeneratorVersion: version,
          canonicalGeometrySource: version === 8
            ? "GENERATOR_8_PRINCIPLED_ARCHITECTURE" : "GENERATOR_9_COASTAL_LINKS",
          geometry: { ...generator8Parkland.geometry!, regions: [green] } };
        expect(defaultPlayPresentation(raw)).toBe("polished");
        const playable = tournamentPresentationHole(raw)!;
        expect(playable.courseType).toBe(expected);
        expect(playable.geometry).toBe(raw.geometry);
        expect(defaultPlayPresentation(playable)).toBe("premium");
      }
    }
  });

  it("retains deliberate fallbacks for legacy and unsupported host geometry", () => {
    const raw = { ...generator8Parkland, courseType: "COASTAL" };
    for (const hole of [
      { ...raw, courseGeneratorVersion: 7 },
      { ...raw, canonicalGeometrySource: "GENERATOR_9_COASTAL_LINKS" },
      { ...raw, courseFamily: undefined },
      { ...raw, canonicalBiome: "DESERT" },
      { ...raw, geometry: { ...raw.geometry!, playableBoundary: [] } },
      { ...raw, courseFamily: "MOUNTAIN", canonicalBiome: "MOUNTAIN" },
      { ...raw, courseFamily: "COASTAL", canonicalBiome: "COASTAL" },
    ]) {
      expect(tournamentPresentationHole(hole)).toBe(hole);
      expect(defaultPlayPresentation(hole)).toBe("polished");
    }
    expect(tournamentPresentationHole(generator8Parkland)).toBe(generator8Parkland);
  });

  it("uses the accepted Links renderer for a curated event on a supported Links host", () => {
    const curatedParklandScene = { ...generator8Parkland, courseType: "PARKLAND",
      canonicalBiome: "LINKS", courseFamily: "LINKS", courseGeneratorVersion: 9,
      canonicalGeometrySource: "GENERATOR_9_COASTAL_LINKS" };
    expect(defaultPlayPresentation(curatedParklandScene)).toBe("polished");
    const playable = tournamentPresentationHole(curatedParklandScene)!;
    expect(playable.courseType).toBe("LINKS");
    expect(playable.geometry).toBe(curatedParklandScene.geometry);
    expect(defaultPlayPresentation(playable)).toBe("premium");
    expect(tournamentPresentationHole({ ...curatedParklandScene, courseGeneratorVersion: 7 }))
      .toHaveProperty("courseType", "PARKLAND");
    expect(tournamentPresentationHole({ ...curatedParklandScene, courseFamily: "PARKLAND" }))
      .toHaveProperty("courseType", "PARKLAND");
  });
  it("keeps the core and historical scene/biome routing available", () => {
    for (const [token, scene, biome] of [
      ["PARKLAND", "parkland", "parkland"],
      ["WOODLAND", "parkland", "parkland"],
      ["HEATHLAND", "heathland", "heathland"],
      ["LINKS", "links", "links"],
      ["DESERT", "desert", "desert"],
      ["MOUNTAIN", "mountain", "alpine"],
      ["COASTAL", "coastal", "tropical"],
      ["TROPICAL", "tropical", "tropical"],
    ]) {
      expect(resolveScene(token)).toBe(scene);
      expect(resolveBiome(token)).toBe(biome);
    }
    for (const token of ["DESERT", "MOUNTAIN", "COASTAL", "TROPICAL"]) {
      expect(defaultPlayPresentation({ ...generator8Parkland, courseType: token, canonicalBiome: token,
        courseFamily: token }))
        .toBe("polished");
    }
  });

  it("uses the Parkland V2 foundation for new Woodland and retains historical Heathland routing", () => {
    const woodland = { ...generator8Parkland, canonicalBiome: "WOODLAND", courseFamily: "PARKLAND",
      courseType: "WOODLAND" };
    expect(defaultPlayPresentation(woodland)).toBe("premium");
    expect(defaultPlayPresentation({ ...woodland, courseFamily: "HEATHLAND", courseType: "HEATHLAND" }))
      .toBe("polished");
    const heathland = { ...woodland, canonicalBiome: "HEATHLAND", courseFamily: "HEATHLAND",
      courseType: "HEATHLAND", geometry: { ...woodland.geometry!, regions: [
        { surface: "GREEN", boundary: [{ x: -10, y: 390 }, { x: 10, y: 390 }, { x: 0, y: 410 }] },
      ] } };
    expect(defaultPlayPresentation(heathland)).toBe("premium");
    expect(defaultPlayPresentation({ ...heathland, geometry: woodland.geometry })).toBe("polished");
  });

  it("uses V2 for authoritative Generator 8 Parkland geometry in production", () => {
    expect(defaultPlayPresentation(generator8Parkland)).toBe("premium");
    expect(playPresentation(generator8Parkland, "polished", false)).toBe("premium");
  });

  it("uses Premium Links only for supported Generator 8 Links holes", () => {
    const links: HoleGeom = { ...generator8Parkland, courseType: "LINKS", canonicalBiome: "LINKS",
      courseFamily: "LINKS" };
    expect(defaultPlayPresentation(links)).toBe("premium");
    expect(defaultPlayPresentation({ ...links, courseGeneratorVersion: 9,
      canonicalGeometrySource: "GENERATOR_9_COASTAL_LINKS" })).toBe("premium");
    expect(defaultPlayPresentation({ ...links, courseGeneratorVersion: 9 })).toBe("polished");
    expect(defaultPlayPresentation({ ...links, courseGeneratorVersion: 7 })).toBe("polished");
    expect(defaultPlayPresentation({ ...links, canonicalGeometrySource: undefined })).toBe("polished");
    expect(defaultPlayPresentation({ ...links, courseFamily: undefined })).toBe("polished");
    expect(defaultPlayPresentation({ ...links, geometry: undefined })).toBe("polished");
  });

  it("selects Premium Desert only for complete Generator 8 Desert geometry", () => {
    const desert: HoleGeom = { ...generator8Parkland, courseType: "DESERT", canonicalBiome: "DESERT",
      courseFamily: "DESERT", geometry: { ...generator8Parkland.geometry!, regions: [
        { surface: "GREEN", boundary: [{ x: -10, y: 390 }, { x: 10, y: 390 }, { x: 0, y: 410 }] },
      ] } };
    expect(defaultPlayPresentation(desert)).toBe("premium");
    expect(defaultPlayPresentation({ ...desert, courseGeneratorVersion: 7 })).toBe("polished");
    expect(defaultPlayPresentation({ ...desert, courseFamily: undefined })).toBe("polished");
    expect(defaultPlayPresentation({ ...desert, geometry: generator8Parkland.geometry })).toBe("polished");
  });

  it("keeps Current for legacy saves, unsupported geometry and other biomes", () => {
    const unsupported: (HoleGeom | null)[] = [
      null,
      { ...generator8Parkland, courseGeneratorVersion: 7, canonicalGeometrySource: "GENERATOR_7_CANONICAL" },
      { ...generator8Parkland, canonicalGeometrySource: undefined },
      { ...generator8Parkland, canonicalBiome: "LINKS", courseType: "LINKS" },
      { ...generator8Parkland, courseType: "LINKS" },
      { ...generator8Parkland, geometry: undefined },
      { ...generator8Parkland, geometry: { ...generator8Parkland.geometry!, playableBoundary: [] } },
    ];
    for (const hole of unsupported) {
      expect(defaultPlayPresentation(hole)).toBe("polished");
      expect(playPresentation(hole, "premium", false)).toBe("polished");
    }
  });

  it("preserves the development-only Current/V1/V2 comparison", () => {
    expect(playPresentation(generator8Parkland, null, true)).toBe("premium");
    expect(playPresentation(generator8Parkland, "polished", true)).toBe("polished");
    expect(playPresentation(generator8Parkland, "premium-v1", true)).toBe("premium-v1");
    expect(playPresentation(generator8Parkland, "premium", true)).toBe("premium");
  });
});
