import { describe, expect, it } from "vitest";

import type { Situation } from "@/components/play/play-shared";
import type { HoleGeom, ResolvedShot } from "@/lib/play/hole-geometry";
import { shotAimMarkerVisible, stagePlaybackState } from "./stage-playback-state";

const oldSituation = { lie: "FAIRWAY", shotRevision: "before" } as Situation;
const fairwaySituation = { lie: "FAIRWAY", shotRevision: "after-fairway" } as Situation;
const greenSituation = { lie: "GREEN", shotRevision: "after-green" } as Situation;
const oldHole = { layoutSeed: "old" } as HoleGeom;
const newHole = { layoutSeed: "new" } as HoleGeom;
const shotViewport = { width: 1280, height: 800, safeTop: 80, safeBottom: 170 };
const resizedViewport = { width: 800, height: 1000, safeTop: 80, safeBottom: 290 };
const tracedShot = { trace: { club: "SEVEN_IRON" }, physicalPutt: null } as ResolvedShot;
const shotAimPoint = { x: 2, y: 120 };
const liveAimPoint = { x: 5, y: 150 };
const aim = { shotAimPoint, liveAimPoint };

describe("shot-time stage presentation", () => {
  it("shows planning aim before submission and after failure or completion, but not while pending or playing", () => {
    expect(shotAimMarkerVisible(false, null)).toBe(true);
    expect(shotAimMarkerVisible(true, null)).toBe(false);
    expect(shotAimMarkerVisible(false, tracedShot)).toBe(false);
    expect(shotAimMarkerVisible(false, null)).toBe(true);
  });

  it.each([fairwaySituation, greenSituation])("holds the old dock and projection during an approach to %s", (liveSituation) => {
    const held = stagePlaybackState({ ...aim, playbackShot: tracedShot, displaySituation: oldSituation,
      displayHole: oldHole, liveSituation, liveHole: newHole,
      shotViewport, liveViewport: resizedViewport });
    expect(held).toEqual({ dockSituation: oldSituation, dockHole: oldHole, viewport: shotViewport, aimPoint: shotAimPoint });
    const released = stagePlaybackState({ ...aim, playbackShot: null, displaySituation: oldSituation,
      displayHole: oldHole, liveSituation, liveHole: newHole,
      shotViewport, liveViewport: resizedViewport });
    expect(released).toEqual({ dockSituation: liveSituation, dockHole: newHole, viewport: resizedViewport, aimPoint: liveAimPoint });
  });

  it("keeps a short pitch on the normal-hole stage until completion, then permits putting controls", () => {
    const pitch = { ...tracedShot, trace: { club: "GAP_WEDGE" } } as ResolvedShot;
    expect(stagePlaybackState({ ...aim, playbackShot: pitch, displaySituation: oldSituation, displayHole: oldHole,
      liveSituation: greenSituation, liveHole: newHole, shotViewport, liveViewport: resizedViewport })
      .dockSituation.lie).toBe("FAIRWAY");
    expect(stagePlaybackState({ ...aim, playbackShot: null, displaySituation: oldSituation, displayHole: oldHole,
      liveSituation: greenSituation, liveHole: newHole, shotViewport, liveViewport: resizedViewport })
      .dockSituation.lie).toBe("GREEN");
  });

  it("leaves the physical putting route on live state", () => {
    const physical = { ...tracedShot, physicalPutt: {
      trajectory: [], finalPosition: { x: 0, y: 0 }, holed: false, terminalCondition: "STOPPED",
    } } as ResolvedShot;
    expect(stagePlaybackState({ ...aim, playbackShot: physical, displaySituation: oldSituation, displayHole: oldHole,
      liveSituation: greenSituation, liveHole: newHole, shotViewport, liveViewport: resizedViewport }))
      .toEqual({ dockSituation: greenSituation, dockHole: newHole, viewport: resizedViewport, aimPoint: liveAimPoint });
  });

  it("does not introduce a new camera mid-shot if measurement completed after launch", () => {
    const held = stagePlaybackState({ ...aim, playbackShot: tracedShot, displaySituation: oldSituation,
      displayHole: oldHole, liveSituation: greenSituation, liveHole: newHole,
      shotViewport: null, liveViewport: resizedViewport });
    expect(held.viewport).toBeUndefined();
    const released = stagePlaybackState({ ...aim, playbackShot: null, displaySituation: oldSituation,
      displayHole: oldHole, liveSituation: greenSituation, liveHole: newHole,
      shotViewport: null, liveViewport: resizedViewport });
    expect(released.viewport).toEqual(resizedViewport);
  });
});
