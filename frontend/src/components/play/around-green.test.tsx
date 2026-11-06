import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import type { HoleGeom, Point, ResolvedShot } from "@/lib/play/hole-geometry";
import type { Situation } from "./play-shared";
import { AROUND_GREEN_BOUNDARY_YARDS, aroundGreenEligible, aroundGreenProjector } from "@/lib/play/around-green";
import { aimWithinEnvelope, buildBallStrikeIntent } from "@/lib/play/intent";
import { pointerWorldTarget } from "@/lib/play/canonical-geometry";
import { puttingPolygonPath } from "@/lib/play/premium-putting";
import { stagePlaybackState } from "@/lib/play/stage-playback-state";
import { scenicFrame } from "@/lib/play/stage-viewport";
import { cupDirectedLine, physicalTrajectoryPoint, visibleSlopeSamples } from "@/lib/play/physical-putting";
import { StageHole } from "./hole-transition";
import { PhysicalPuttingGreen } from "./physical-putting-green";
import { ShotMotionFrame } from "./shot-motion-overlay";
import { BallStrikeDock } from "./ball-strike-dock";
import { ReadGreenButton, aroundGreenSlopeReview } from "./play-mode";

const polygon = (left: number, bottom: number, right: number, top: number): Point[] => [
  { x: left, y: bottom }, { x: right, y: bottom }, { x: right, y: top }, { x: left, y: top },
];

function hole(position: Point = { x: 12, y: -33 }, lie = "FIRST_CUT"): HoleGeom {
  return {
    holeNumber: 6, par: 4, length: 350, fairwayHalfWidth: 10, greenHalfWidth: 12, greenDepth: 40,
    elevationDelta: 0, hasGreensideBunker: true, hasWater: true, hasTrees: false, pinLateral: 0, pinDepth: 0,
    courseType: "PARKLAND", courseFamily: "PARKLAND", canonicalBiome: "PARKLAND",
    courseGeneratorVersion: 8, canonicalGeometrySource: "GENERATOR_8_PRINCIPLED_ARCHITECTURE",
    layoutSeed: "around-green-fixture", effectiveWind: { x: 0, y: 0, magnitude: 0, unit: "EFFECTIVE_YARDS" },
    puttingPolicy: "PHYSICAL_V1", physicalGreen: {
      slopeSamples: [{ position: { x: 0, y: -5 }, elevation: .4, downhillX: .8, downhillY: -.6, slope: .02 }],
      contours: [{ elevation: .4, points: [{ x: -8, y: -5 }, { x: 0, y: -4 }, { x: 8, y: -3 }] }],
    }, ball: { position, lie },
    geometry: { tee: { x: 0, y: -300 }, cup: { x: 0, y: 0 }, playableBoundary: polygon(-70, -80, 70, 60),
      regions: [
        { surface: "PRIMARY_ROUGH", boundary: polygon(-40, -80, 40, 45) },
        { surface: "FIRST_CUT", boundary: polygon(-20, -78, 20, 28) },
        { surface: "FAIRWAY", boundary: polygon(-10, -75, 10, 25) },
        { surface: "FRINGE", boundary: polygon(-15, -28, 15, 18) },
        { surface: "GREEN", boundary: polygon(-12, -25, 12, 15) },
        { surface: "BUNKER", boundary: polygon(18, -10, 29, 8) },
        { surface: "WATER", boundary: polygon(-45, -12, -25, 18) },
      ] },
  };
}

function situation(lie: string, position: Point): Situation {
  return { holeNumber: 6, par: 4, shotNumber: 3, strokesThisHole: 2,
    distanceToPin: Math.hypot(position.x, position.y), lie, pinLateral: 0, minReach: 0, maxReach: 100,
    shotRevision: "6:3", aimEnvelope: { minX: -60, maxX: 60, minY: -80, maxY: 70 },
    guidance: { safe: { x: 0, y: -9 }, primary: { x: 0, y: -6 }, aggressive: { x: 0, y: 0 },
      clubs: [{ club: "GAP_WEDGE", label: "Gap Wedge", nominalCarry: 90, normalReach: 90,
        families: [{ family: "CHIP", available: true, reason: null,
          shapes: [{ shape: "STRAIGHT", available: true, reason: null }] }] }], strategicOptions: [] } };
}

const stage = { x: 0, y: 0, width: 430, height: 300 };
const narrow = { x: 0, y: 0, width: 190, height: 500 };
const target = { x: 0, y: -6 };

function stageHtml(h: HoleGeom, lie: string, playbackShot: ResolvedShot | null = null,
                   stageViewport?: { width: number; height: number; safeTop: number; safeBottom: number }, read = false,
                   fringePutting = false) {
  return renderToStaticMarkup(<StageHole displayHole={h} situation={situation(lie, h.ball!.position)} pressure={null}
    playbackShot={playbackShot} phase="play" postHole={null} aimPoint={target} onAimPoint={() => {}}
    physicalAimDirection={{ x: 0, y: 1 }} onPhysicalAimDirection={() => {}} showGreenReading={read}
    onPhysicalPlaybackComplete={() => {}} onTracePlaybackComplete={() => {}} presentation="premium"
    stageViewport={stageViewport} fringePutting={fringePutting} />);
}

describe("around the green in the existing tournament stage", () => {
  it("uses boundary distance for first cut and primary rough even when the pin is 35–37 yards away", () => {
    const firstCut = hole();
    const rough = hole({ x: 24, y: -28 }, "PRIMARY_ROUGH");
    expect(situation("FIRST_CUT", firstCut.ball!.position).distanceToPin).toBeGreaterThan(35);
    expect(situation("PRIMARY_ROUGH", rough.ball!.position).distanceToPin).toBeGreaterThan(36);
    expect(aroundGreenEligible(firstCut, "FIRST_CUT", "premium")).toBe(true);
    expect(aroundGreenEligible(rough, "PRIMARY_ROUGH", "premium")).toBe(true);
    expect(AROUND_GREEN_BOUNDARY_YARDS).toBe(40);
    for (const boundaryDistance of [15, 30, 40]) {
      expect(aroundGreenEligible(hole({ x: 0, y: -25 - boundaryDistance }, "FIRST_CUT"),
        "FIRST_CUT", "premium")).toBe(true);
    }
    expect(situation("FIRST_CUT", { x: 0, y: -51 }).distanceToPin).toBe(51);
    expect(aroundGreenEligible(hole({ x: 0, y: -51 }, "FIRST_CUT"), "FIRST_CUT", "premium")).toBe(true);
    for (const [position, lie] of [[{ x: 0, y: -27 }, "FRINGE"], [{ x: 0, y: -35 }, "FAIRWAY"],
      [{ x: 19, y: 0 }, "BUNKER"]] as const) {
      expect(aroundGreenEligible(hole(position, lie), lie, "premium")).toBe(true);
    }
  });

  it("returns to full hole beyond 40 yards or without valid premium geometry, while green takes precedence", () => {
    const far = hole({ x: 0, y: -65.1 }, "FIRST_CUT"); // 40.1 yards beyond the green edge
    expect(aroundGreenEligible(far, "FIRST_CUT", "premium")).toBe(false);
    expect(stageHtml(far, "FIRST_CUT")).toContain("canonical terrain");
    const greenBall = hole({ x: 0, y: -5 }, "GREEN");
    expect(aroundGreenEligible(greenBall, "GREEN", "premium")).toBe(false);
    expect(stageHtml(greenBall, "GREEN")).toContain("Physical putting green: choose a starting line");
    expect(aroundGreenEligible(hole(), "FIRST_CUT", "polished")).toBe(false);
    expect(aroundGreenEligible({ ...hole(), physicalGreen: null }, "FIRST_CUT", "premium")).toBe(false);
    const invalid = hole();
    const missingFringe = { ...invalid, geometry: { ...invalid.geometry!,
      regions: invalid.geometry!.regions.filter((region) => region.surface !== "FRINGE") } };
    expect(aroundGreenEligible(missingFringe, "FIRST_CUT", "premium")).toBe(false);
    expect(stageHtml(missingFringe, "FIRST_CUT")).toContain("canonical terrain");
    expect(aroundGreenEligible({ ...invalid, geometry: { ...invalid.geometry!, playableBoundary: [] } },
      "FIRST_CUT", "premium")).toBe(false);
    expect(aroundGreenEligible({ ...invalid, geometry: { ...invalid.geometry!, regions: invalid.geometry!.regions.map(
      (region) => region.surface === "GREEN" ? { ...region, boundary: [] } : region) } },
    "FIRST_CUT", "premium")).toBe(false);
    expect(aroundGreenEligible(hole({ x: Number.NaN, y: -33 }), "FIRST_CUT", "premium")).toBe(false);
  });

  it("fits actual ball, green, hazards and target with invertible desktop and narrow cameras", () => {
    const h = hole({ x: 0, y: -65 });
    for (const scene of [stage, narrow]) {
      const camera = aroundGreenProjector(h, scene, target);
      for (const point of [h.ball!.position, h.geometry!.cup, target,
        ...h.geometry!.regions.filter((region) => ["GREEN", "BUNKER", "WATER"].includes(region.surface))
          .flatMap((region) => region.boundary)]) {
        const screen = camera.project(point);
        expect(screen.x).toBeGreaterThanOrEqual(scene.x + 18 - 1e-6);
        expect(screen.x).toBeLessThanOrEqual(scene.x + scene.width - 18 + 1e-6);
        expect(screen.y).toBeGreaterThanOrEqual(scene.y + 30 - 1e-6);
        expect(screen.y).toBeLessThanOrEqual(scene.y + scene.height - 30 + 1e-6);
        expect(camera.unproject(screen).x).toBeCloseTo(point.x, 8);
        expect(camera.unproject(screen).y).toBeCloseTo(point.y, 8);
      }
      const projected = camera.project(target);
      const world = pointerWorldTarget({ x: projected.x * 2 + 11, y: projected.y * 2 + 17 },
        { a: .5, b: 0, c: 0, d: .5, e: -5.5, f: -8.5 }, camera.unproject);
      expect(world.x).toBeCloseTo(target.x, 8);
      expect(world.y).toBeCloseTo(target.y, 8);
      expect(aimWithinEnvelope(world, situation("FIRST_CUT", h.ball!.position).aimEnvelope)).toBe(true);
      expect(buildBallStrikeIntent("GAP_WEDGE", world, "CHIP", "STRAIGHT", "6:3"))
        .toEqual({ club: "GAP_WEDGE", aimPoint: world, shotFamily: "CHIP", shotShape: "STRAIGHT", expectedShotRevision: "6:3" });
    }
  });

  it("fits an irregular green without moving any world-space anchors", () => {
    const base = hole();
    const irregular = { ...base, geometry: { ...base.geometry!, regions: base.geometry!.regions.map((region) =>
      region.surface === "GREEN" ? { ...region, boundary: [
        { x: -16, y: -20 }, { x: -8, y: -29 }, { x: 10, y: -25 },
        { x: 17, y: -4 }, { x: 8, y: 18 }, { x: -14, y: 11 },
      ] } : region) } };
    expect(aroundGreenEligible(irregular, "FIRST_CUT", "premium")).toBe(true);
    const camera = aroundGreenProjector(irregular, narrow, target);
    for (const point of irregular.geometry.regions.find((region) => region.surface === "GREEN")!.boundary) {
      expect(camera.project(point).x).toBeGreaterThan(narrow.x + 17);
      expect(camera.project(point).x).toBeLessThan(narrow.x + narrow.width - 17);
      expect(camera.unproject(camera.project(point)).y).toBeCloseTo(point.y, 8);
    }
  });

  it("renders authoritative terrain with green contours and toggleable slope arrows in the expanded projection", () => {
    const h = hole();
    const svg = stageHtml(h, "FIRST_CUT");
    const read = stageHtml(h, "FIRST_CUT", null, undefined, true);
    const projector = aroundGreenProjector(h, { x: -51.6666666667, y: 0, width: 533.3333333333, height: 300 }, target);
    expect(svg).toContain("Around the green: click or tap to aim");
    expect(svg).toContain("Premium parkland putting terrain");
    expect(svg).toContain('aria-label="FAIRWAY terrain"');
    expect(svg).toContain('aria-label="PRIMARY_ROUGH terrain"');
    expect(svg).toContain(puttingPolygonPath(h.geometry!.regions.find((region) => region.surface === "GREEN")!.boundary, projector));
    expect(svg).toContain('aria-label="Selected landing target"');
    const ball = svg.match(/aria-label="Ball marker at authoritative lie">.*?<circle cx="([^"]+)" cy="([^"]+)"/);
    expect(Number(ball?.[1])).toBeCloseTo(projector.project(h.ball!.position).x, 8);
    expect(Number(ball?.[2])).toBeCloseTo(projector.project(h.ball!.position).y, 8);
    expect(svg).toContain('aria-label="Green reading overlay"');
    expect(svg).toContain('aria-label="Physical elevation contours"');
    const contour = svg.match(/aria-label="Physical elevation contours"[^>]*><path d="([^"]+)"/)?.[1];
    const contourCoords = contour?.match(/-?\d+(?:\.\d+)?/g)?.map(Number) ?? [];
    expect(contourCoords).toHaveLength(6);
    h.physicalGreen!.contours[0].points.forEach((point, i) => {
      expect(contourCoords[i * 2]).toBeCloseTo(projector.project(point).x, 8);
      expect(contourCoords[i * 2 + 1]).toBeCloseTo(projector.project(point).y, 8);
    });
    expect(svg).not.toContain('marker-end="url(#downhill-arrow-');
    const slopePoint = projector.project(h.physicalGreen!.slopeSamples[0].position);
    const readSample = read.match(/aria-label="Green reading overlay"[\s\S]*?<circle cx="([^"]+)" cy="([^"]+)"/);
    expect(Number(readSample?.[1])).toBeCloseTo(slopePoint.x, 8);
    expect(Number(readSample?.[2])).toBeCloseTo(slopePoint.y, 8);
    expect(read).toContain('marker-end="url(#downhill-arrow-');
  });

  it("toggles reading without changing dock boxes, stage framing or aim positions at desktop and mobile sizes", () => {
    const h = hole();
    const current = situation("FIRST_CUT", h.ball!.position);
    const selection = { club: "GAP_WEDGE", technique: "CHIP", shape: "STRAIGHT" };
    const dock = (read: boolean) => renderToStaticMarkup(<BallStrikeDock situation={current}
      origin={h.ball!.position} aimPoint={target} onAimPoint={() => {}} selection={selection}
      onSelection={() => {}} onSubmit={async () => {}} locked={false} submitting={false}
      review={aroundGreenSlopeReview(true)} floating
      auxiliary={<ReadGreenButton active={read} onToggle={() => {}} disabled={false} aroundGreen />} />);
    const offDock = dock(false);
    const onDock = dock(true);
    const stableBoxes = (markup: string) => markup.replace(/aria-pressed="(?:true|false)"/g, 'aria-pressed="STATE"')
      .replace(/<svg\b[^>]*>[\s\S]*?<\/svg>/g, "<svg/>");
    expect(stableBoxes(onDock)).toBe(stableBoxes(offDock));
    expect(offDock).toContain("Green slopes show terrain; chip and pitch rollout follows existing shot rules.");
    expect(aroundGreenSlopeReview(false)).toBeUndefined();
    expect(onDock).toContain('value="CHIP"');
    expect(onDock).toContain('value="GAP_WEDGE"');

    for (const viewport of [{ width: 1600, height: 900, safeTop: 63, safeBottom: 157 },
      { width: 390, height: 844, safeTop: 63, safeBottom: 265 }]) {
      const offStage = stageHtml(h, "FIRST_CUT", null, viewport, false);
      const onStage = stageHtml(h, "FIRST_CUT", null, viewport, true);
      const viewBox = (markup: string) => markup.match(/viewBox="([^"]+)"/)?.[1];
      const marker = (markup: string, label: string) => markup.match(
        new RegExp(`aria-label="${label}"[\\s\\S]*?<circle cx="([^"]+)" cy="([^"]+)"`))?.slice(1);
      expect(viewBox(onStage)).toBe(viewBox(offStage));
      for (const label of ["Ball marker at authoritative lie", "Cup marker at authoritative pin", "Selected landing target"]) {
        expect(marker(onStage, label)).toEqual(marker(offStage, label));
      }
      expect(onStage).toContain('marker-end="url(#downhill-arrow-');
      expect(offStage).not.toContain('marker-end="url(#downhill-arrow-');
      const frame = scenicFrame(viewport, 300);
      const width = frame.height * frame.aspect;
      const scene = { x: (430 - width) / 2, y: 0, width, height: frame.safeHeight };
      const camera = aroundGreenProjector(h, scene, target);
      for (const [label, point] of [["Ball marker at authoritative lie", h.ball!.position],
        ["Cup marker at authoritative pin", h.geometry!.cup], ["Selected landing target", target]] as const) {
        expect(Number(marker(onStage, label)?.[0])).toBeCloseTo(camera.project(point).x, 8);
        expect(Number(marker(onStage, label)?.[1])).toBeCloseTo(camera.project(point).y, 8);
      }
      for (const region of h.geometry!.regions.filter((candidate) =>
        ["GREEN", "BUNKER", "WATER"].includes(candidate.surface))) {
        const path = puttingPolygonPath(region.boundary, camera);
        expect(onStage).toContain(path);
        expect(offStage).toContain(path);
      }
    }
  });

  it("keeps the SVG camera aligned with desktop and narrow stage areas across the four biomes", () => {
    const families = [
      ["PARKLAND", "PARKLAND"], ["LINKS", "LINKS"],
      ["HEATHLAND", "HEATHLAND"], ["DESERT", "DESERT"],
    ] as const;
    for (const [courseFamily, courseType] of families) {
      const h = { ...hole(), courseFamily, courseType, canonicalBiome: courseType };
      for (const viewport of [{ width: 1600, height: 900, safeTop: 63, safeBottom: 157 },
        { width: 390, height: 844, safeTop: 63, safeBottom: 265 }]) {
        const svg = stageHtml(h, "FIRST_CUT", null, viewport);
        expect(svg).toContain(`Premium ${courseFamily.toLowerCase()} putting terrain`);
        const [,, width, height] = svg.match(/viewBox="([^"]+)"/)![1].split(" ").map(Number);
        expect(width / height).toBeCloseTo(viewport.width / viewport.height, 8);
      }
    }
  });

  it("keeps a 40-yard ball and landing target clear of the playable stage edges on desktop and mobile", () => {
    const h = hole({ x: 0, y: -65 });
    for (const viewport of [{ width: 1600, height: 900, safeTop: 63, safeBottom: 157 },
      { width: 390, height: 844, safeTop: 63, safeBottom: 265 }]) {
      const frame = scenicFrame(viewport, 300);
      const sceneWidth = frame.height * frame.aspect;
      const cameraScene = { x: (430 - sceneWidth) / 2, y: 0, width: sceneWidth, height: frame.safeHeight };
      const camera = aroundGreenProjector(h, cameraScene, target);
      const edgeSample = { position: { x: 12, y: -5 }, elevation: .4, downhillX: 1, downhillY: 0, slope: .03 };
      expect(visibleSlopeSamples([edgeSample], camera, cameraScene)).toEqual([edgeSample]);
      if (viewport.width === 390) expect(camera.project(edgeSample.position).x).toBeGreaterThan(camera.width);
      const edgeHole = { ...h, physicalGreen: { ...h.physicalGreen!, slopeSamples: [edgeSample] } };
      const readSvg = stageHtml(edgeHole, "FIRST_CUT", null, viewport, true);
      const readSample = readSvg.match(/aria-label="Green reading overlay"[\s\S]*?<circle cx="([^"]+)" cy="([^"]+)"/);
      expect(Number(readSample?.[1])).toBeCloseTo(camera.project(edgeSample.position).x, 8);
      expect(Number(readSample?.[2])).toBeCloseTo(camera.project(edgeSample.position).y, 8);
      const svg = stageHtml(h, "FIRST_CUT", null, viewport);
      const ball = svg.match(/aria-label="Ball marker at authoritative lie">.*?<circle cx="([^"]+)" cy="([^"]+)"/);
      expect(Number(ball?.[1])).toBeCloseTo(camera.project(h.ball!.position).x, 8);
      expect(Number(ball?.[2])).toBeCloseTo(camera.project(h.ball!.position).y, 8);
      for (const point of [h.ball!.position, target, h.geometry!.cup,
        ...h.geometry!.regions.find((region) => region.surface === "GREEN")!.boundary]) {
        const projected = camera.project(point);
        expect(projected.y).toBeGreaterThanOrEqual(30);
        expect(projected.y).toBeLessThanOrEqual(270);
        expect(projected.x).toBeGreaterThanOrEqual(cameraScene.x + 18);
        expect(projected.x).toBeLessThanOrEqual(cameraScene.x + cameraScene.width - 18);
        expect(camera.unproject(projected).y).toBeCloseTo(point.y, 8);
      }
    }
  });

  it("holds the around-green presentation through the authoritative trace, then allows physical putting", () => {
    const before = hole();
    const after = hole({ x: 0, y: -6 }, "GREEN");
    const trace: NonNullable<ResolvedShot["trace"]> = { club: "GAP_WEDGE", origin: before.ball!.position,
      aimPoint: target, contact: { position: { x: 0, y: -7 }, surface: "GREEN" },
      airbornePath: [{ progress: 0, position: before.ball!.position, height: 0 },
        { progress: 1, position: { x: 0, y: -7 }, height: 0 }],
      groundPath: [{ kind: "ROLL", position: { x: 0, y: -6 }, heightYards: 0 }],
      roll: { from: { x: 0, y: -7 }, to: { x: 0, y: -6 } }, transition: null,
      finalPoint: { x: 0, y: -6 } };
    const shot = { trace, physicalPutt: null, finalSurface: "GREEN", carry: 25, lateral: 0,
      distanceRemaining: 6, holed: false } satisfies ResolvedShot;
    expect(stageHtml(before, "FIRST_CUT", shot)).toContain('data-shot-ball="true"');
    const camera = aroundGreenProjector(before, stage, target, trace);
    const final = camera.project(trace.finalPoint);
    const frame = renderToStaticMarkup(<ShotMotionFrame trace={trace} project={camera.project}
      contact={camera.project(trace.contact.position)} finalPoint={final} contactEffect="var(--accent)"
      ballStroke="var(--background)" shadowFill="var(--background)" statusY={284} progress={1} />);
    expect(frame).toContain(`data-shot-final="true" cx="${final.x}" cy="${final.y}"`);
    expect(frame).toContain('data-shot-contact="true"');
    expect(frame).toContain("Roll");
    const held = stagePlaybackState({ playbackShot: shot, displaySituation: situation("FIRST_CUT", before.ball!.position),
      displayHole: before, liveSituation: situation("GREEN", after.ball!.position), liveHole: after,
      liveAimPoint: target, shotAimPoint: target });
    expect(aroundGreenEligible(held.dockHole, held.dockSituation.lie, "premium")).toBe(true);
    const settled = stagePlaybackState({ ...held, playbackShot: null, displaySituation: held.dockSituation,
      displayHole: held.dockHole, liveSituation: situation("GREEN", after.ball!.position), liveHole: after,
      liveAimPoint: target });
    expect(stageHtml(settled.dockHole!, settled.dockSituation.lie)).toContain("Physical putting green: choose a starting line");
  });

  it("uses the fringe camera for a physical line and holds the authoritative roll until the green transition", () => {
    const before = hole({ x: 0, y: -27 }, "FRINGE");
    const after = hole({ x: 0, y: -5 }, "GREEN");
    const trajectory = [
      { timeSeconds: 0, position: before.ball!.position, speedYardsPerSecond: 4 },
      { timeSeconds: 1, position: { x: 0, y: -20 }, speedYardsPerSecond: 2 },
      { timeSeconds: 2, position: after.ball!.position, speedYardsPerSecond: 0 },
    ];
    const shot = { physicalPutt: { trajectory, finalPosition: after.ball!.position, holed: false,
      terminalCondition: "STOPPED" }, finalSurface: "GREEN", carry: 22, lateral: 0,
      distanceRemaining: 5, holed: false } satisfies ResolvedShot;
    const camera = aroundGreenProjector(before, stage);
    for (const scene of [stage, narrow]) {
      const projector = aroundGreenProjector(before, scene);
      const selected = { x: 2, y: -15 };
      const screen = projector.project(selected);
      const mapped = pointerWorldTarget({ x: screen.x * 2 + 11, y: screen.y * 2 + 17 },
        { a: .5, b: 0, c: 0, d: .5, e: -5.5, f: -8.5 }, projector.unproject);
      expect(cupDirectedLine(before.ball!.position, mapped)).toEqual(cupDirectedLine(before.ball!.position, selected));
    }
    expect(stageHtml(before, "FRINGE", null, undefined, false, true))
      .toContain("Physical fringe putt: click or tap to choose a starting line");
    expect(stageHtml(before, "FRINGE", null, undefined, false, true))
      .toContain('aria-label="Submitted starting direction"');
    expect(stageHtml(before, "FRINGE", null, undefined, false, true))
      .not.toContain('aria-label="Selected landing target"');
    const held = stagePlaybackState({ playbackShot: shot, displaySituation: situation("FRINGE", before.ball!.position),
      displayHole: before, liveSituation: situation("GREEN", after.ball!.position), liveHole: after,
      liveAimPoint: target, shotAimPoint: target });
    expect(stageHtml(held.dockHole!, held.dockSituation.lie, shot, undefined, false, true))
      .toContain("Physical fringe putt: click or tap to choose a starting line");
    expect(stageHtml(held.dockHole!, held.dockSituation.lie, shot, undefined, false, true))
      .not.toContain('aria-label="Submitted starting direction"');
    const midPoint = physicalTrajectoryPoint(trajectory, .5);
    expect(camera.unproject(camera.project(midPoint))).toEqual(midPoint);
    expect(stageHtml(after, "GREEN")).toContain("Physical putting green: choose a starting line");
  });

  it("keeps the putting renderer's existing green view available", () => {
    const h = hole({ x: 0, y: -5 }, "GREEN");
    for (const read of [false, true]) {
      const svg = renderToStaticMarkup(<PhysicalPuttingGreen hole={h} ball={null} aimDirection={{ x: 0, y: 1 }}
        onAimDirection={() => {}} showReading={read} presentation="premium" />);
      expect(svg).toContain("Physical putting green: choose a starting line");
      expect(svg).toContain('aria-label="Green reading overlay"');
      expect(svg.includes('marker-end="url(#downhill-arrow-')).toBe(read);
    }
  });
});
