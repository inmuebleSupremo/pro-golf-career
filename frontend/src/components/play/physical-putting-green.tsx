"use client";

import { useId, useLayoutEffect, useMemo, useRef, useState, type PointerEvent } from "react";

import type { HoleGeom, Point, ResolvedShot } from "../../lib/play/hole-geometry";
import { PHYSICAL_BALL_RADIUS_YARDS, PHYSICAL_CUP_RADIUS_YARDS, PHYSICAL_PUTTING_PRESENTATION, PHYSICAL_PUTTING_SVG_STYLE, physicalAimGuideEndpoint, physicalAimGuideVisible, physicalBallLocatorRadius, physicalCupLocatorRadius, physicalPuttPlaybackDurationMs, physicalPuttingProjector, physicalTrajectoryPoint, physicalTraversedTrajectory, physicalWorldMarkerRadius, type PhysicalPuttingProjector, visibleSlopeSamples } from "../../lib/play/physical-putting";
import { premiumPuttingFamily, puttingPolygonPath } from "../../lib/play/premium-putting";
import { PremiumPuttingTerrain } from "./premium-putting-terrain";
import { scenicFrame, type StageViewport } from "@/lib/play/stage-viewport";
import { golfAudio } from "@/lib/play/golf-audio";

/** A green-local SVG: all terrain facts, slope arrows and ball motion come from backend physical data. */
export function PhysicalPuttingGreen({
  hole,
  ball,
  aimDirection,
  onAimDirection,
  showReading,
  onPlaybackComplete,
  presentation = "polished",
  stageViewport,
}: {
  hole: HoleGeom;
  ball: ResolvedShot | null;
  aimDirection: Point;
  onAimDirection: (direction: Point) => void;
  showReading: boolean;
  onPlaybackComplete?: () => void;
  presentation?: string;
  stageViewport?: StageViewport;
}) {
  const svgRef = useRef<SVGSVGElement>(null);
  const [measuredAspect, setMeasuredAspect] = useState<number | null>(null);
  const clipId = useId().replace(/:/g, "");
  const trajectory = useMemo(() => ball?.physicalPutt?.trajectory ?? [], [ball?.physicalPutt?.trajectory]);
  const origin = trajectory[0]?.position ?? hole.ball?.position ?? hole.geometry!.tee;
  const green = useMemo(() => hole.geometry!.regions.find((region) => region.surface === "GREEN")?.boundary ?? [], [hole.geometry]);
  const premiumFamily = premiumPuttingFamily(hole, presentation);
  useLayoutEffect(() => {
    if (!premiumFamily || !svgRef.current) return;
    const svg = svgRef.current;
    const update = () => {
      const { width, height } = svg.getBoundingClientRect();
      if (width > 0 && height > 0) setMeasuredAspect((current) => current === null
        || Math.abs(current - width / height) > .01 ? width / height : current);
    };
    update();
    const observer = typeof ResizeObserver === "undefined" ? null : new ResizeObserver(update);
    observer?.observe(svg);
    window.addEventListener("resize", update);
    return () => { observer?.disconnect(); window.removeEventListener("resize", update); };
  }, [premiumFamily]);
  const frame = scenicFrame(premiumFamily ? stageViewport : undefined, PHYSICAL_PUTTING_PRESENTATION.height);
  const stageAspect = measuredAspect ?? (premiumFamily && stageViewport ? frame.aspect : 16 / 9);
  const scene = useMemo(() => {
    const { width, height } = PHYSICAL_PUTTING_PRESENTATION;
    if (!premiumFamily) return { x: 0, y: 0, width, height };
    if (!stageViewport) {
      const sceneWidth = Math.max(width, height * stageAspect);
      const sceneHeight = Math.max(height, width / stageAspect);
      return { x: (width - sceneWidth) / 2, y: (height - sceneHeight) / 2, width: sceneWidth, height: sceneHeight };
    }
    const sceneWidth = frame.height * stageAspect;
    return { x: (width - sceneWidth) / 2, y: -frame.safeTop, width: sceneWidth, height: frame.height };
  }, [premiumFamily, stageAspect, frame.height, frame.safeTop, stageViewport]);
  const cupDistanceFeet = Math.hypot(hole.geometry!.cup.x - origin.x, hole.geometry!.cup.y - origin.y) * 3;
  const basePadding = cupDistanceFeet >= 45 ? 6 : cupDistanceFeet >= 15 ? 3.5 : 2;
  const safeSceneWidth = Math.min(PHYSICAL_PUTTING_PRESENTATION.width, scene.width);
  const horizontalSpan = Math.abs(hole.geometry!.cup.x - origin.x);
  const neededVerticalSpan = horizontalSpan * PHYSICAL_PUTTING_PRESENTATION.width / Math.max(40, safeSceneWidth - 40);
  const visualPadding = premiumFamily ? Math.max(basePadding, (neededVerticalSpan - cupDistanceFeet / 3) / 2) : undefined;
  const projector = useMemo(() => physicalPuttingProjector(origin, hole.geometry!.cup, green, visualPadding), [green, origin, hole.geometry, visualPadding]);
  const [animation, setAnimation] = useState<{ trajectory: typeof trajectory | null; elapsed: number }>({ trajectory: null, elapsed: 0 });
  const elapsed = animation.trajectory === trajectory ? animation.elapsed : 0;
  const completedTrajectory = useRef<typeof trajectory | null>(null);
  const playbackCompleteRef = useRef(onPlaybackComplete);

  useLayoutEffect(() => {
    playbackCompleteRef.current = onPlaybackComplete;
  }, [onPlaybackComplete]);

  // This is intentionally a layout effect: a server response can contain the final point immediately, but it
  // must never be painted before playback begins.
  useLayoutEffect(() => {
    if (trajectory.length < 2) return;
    completedTrajectory.current = null;
    let frame = 0;
    let finished = false;
    const start = performance.now();
    // Preserve solver time ordering while keeping a decision readable in the existing shot sequence.
    const duration = physicalPuttPlaybackDurationMs(trajectory);
    const tick = (now: number) => {
      const next = Math.min(1, (now - start) / duration);
      if (ball?.physicalPutt) golfAudio.movement(ball.physicalPutt, next, "PUTTER", "PUTT");
      setAnimation({ trajectory, elapsed: next });
      if (next < 1) frame = requestAnimationFrame(tick);
      else if (completedTrajectory.current !== trajectory) {
        finished = true;
        completedTrajectory.current = trajectory;
        if (ball?.physicalPutt) golfAudio.cup(ball.physicalPutt, ball.physicalPutt.holed && !!ball.holed);
        playbackCompleteRef.current?.();
      }
    };
    frame = requestAnimationFrame((now) => {
      setAnimation({ trajectory, elapsed: 0 });
      frame = requestAnimationFrame(() => tick(now));
    });
    return () => { cancelAnimationFrame(frame); if (!finished) golfAudio.stopEffects(); };
  }, [trajectory, ball]);

  const traversedTrajectory = physicalTraversedTrajectory(trajectory, elapsed);
  const moving = trajectory.length ? physicalTrajectoryPoint(trajectory, elapsed) : origin;
  const ballPoint = projector.project(moving);
  const cup = projector.project(hole.geometry!.cup);
  const originPoint = projector.project(origin);
  const path = traversedTrajectory.length > 1 ? traversedTrajectory.map((sample, index) => `${index === 0 ? "M" : "L"}${projector.project(sample.position).x} ${projector.project(sample.position).y}`).join(" ") : null;
  const greenPath = puttingPolygonPath(green, projector);
  const aimEnd = projector.project(physicalAimGuideEndpoint(origin, aimDirection,
    Math.hypot(hole.geometry!.cup.x - origin.x, hole.geometry!.cup.y - origin.y)));
  const cupMarkerRadius = physicalWorldMarkerRadius(PHYSICAL_CUP_RADIUS_YARDS, projector.pixelsPerYard);
  const ballMarkerRadius = physicalWorldMarkerRadius(PHYSICAL_BALL_RADIUS_YARDS, projector.pixelsPerYard);
  // Flag artwork follows the existing yard-to-screen scale; physical ball/cup dimensions stay separate.
  const flagScale = Math.max(1, Math.min(1.8, Math.sqrt(projector.pixelsPerYard / 9)));
  const flagHeight = 15 * flagScale;
  const flagWidth = 8 * flagScale;
  const flagHalfHeight = 3 * flagScale;
  const cupLocatorRadius = physicalCupLocatorRadius(cupMarkerRadius);
  const ballLocatorRadius = physicalBallLocatorRadius(ballMarkerRadius);

  function chooseLine(event: PointerEvent<SVGSVGElement>) {
    if (trajectory.length > 0 || !svgRef.current) return;
    const screenMatrix = svgRef.current.getScreenCTM();
    if (!screenMatrix) return;
    const local = new DOMPoint(event.clientX, event.clientY).matrixTransform(screenMatrix.inverse());
    if (local.x < scene.x || local.x > scene.x + scene.width || local.y < scene.y || local.y > scene.y + scene.height) return;
    const target = projector.unproject({ x: local.x, y: local.y });
    const dx = target.x - origin.x;
    const dy = target.y - origin.y;
    const length = Math.hypot(dx, dy);
    if (length > 0.01) onAimDirection({ x: dx / length, y: dy / length });
  }

  return <svg ref={svgRef} viewBox={`${scene.x} ${scene.y} ${scene.width} ${scene.height}`} className={premiumFamily ? "absolute inset-0 block h-full w-full touch-manipulation" : "block max-h-full max-w-full"} role="img"
    aria-label="Physical putting green: choose a starting line by clicking the green" onPointerDown={chooseLine}>
    {premiumFamily ? <PremiumPuttingTerrain hole={hole} projector={projector} family={premiumFamily} id={clipId} greenPath={greenPath} scene={scene}/>
      : <rect width={PHYSICAL_PUTTING_PRESENTATION.width} height={PHYSICAL_PUTTING_PRESENTATION.height} fill={PHYSICAL_PUTTING_SVG_STYLE.canvasFill} />}
    <defs><clipPath id={clipId}><path d={greenPath} /></clipPath></defs>
    {!premiumFamily ? <><path d={greenPath} fill={PHYSICAL_PUTTING_SVG_STYLE.greenFill} fillOpacity=".62" stroke="var(--border-strong)" strokeWidth="2" />
      <path d={greenPath} fill="none" stroke="var(--foreground)" strokeOpacity=".18" strokeWidth=".8" /></> : null}
    {showReading || premiumFamily ? <GreenReading samples={hole.physicalGreen?.slopeSamples ?? []}
      contours={hole.physicalGreen?.contours ?? []} projector={projector} clipId={clipId} premium={!!premiumFamily} showArrows={showReading} /> : null}
    {path ? <path d={path} fill="none" stroke={premiumFamily ? "#eef6e5" : "var(--foreground)"} strokeWidth="1.5" strokeOpacity=".72" pointerEvents="none" /> : null}
    {physicalAimGuideVisible(trajectory.length > 0) ? <g aria-label="Submitted starting direction">
      <line x1={originPoint.x} y1={originPoint.y} x2={aimEnd.x} y2={aimEnd.y} stroke={premiumFamily ? "#132c1b" : PHYSICAL_PUTTING_SVG_STYLE.guideOutline} strokeWidth="4" strokeLinecap="round" strokeOpacity=".7" pointerEvents="none" />
      <line x1={originPoint.x} y1={originPoint.y} x2={aimEnd.x} y2={aimEnd.y} stroke={premiumFamily ? "#eef6e5" : PHYSICAL_PUTTING_SVG_STYLE.guideStroke} strokeWidth="2" strokeDasharray="6 4" strokeLinecap="round" pointerEvents="none" />
      {premiumFamily ? <circle cx={aimEnd.x} cy={aimEnd.y} r="3" fill="#eef6e5" stroke="#132c1b" strokeWidth="1" pointerEvents="none"/> : null}
    </g> : null}
    {cupLocatorRadius ? <g aria-label="Cup location aid, not to scale" pointerEvents="none">
      <circle cx={cup.x} cy={cup.y} r={cupLocatorRadius} fill="none" stroke={premiumFamily ? "#f3eee1" : "var(--gold)"} strokeWidth="1.2" strokeDasharray="2 2" />
    </g> : null}
    <g aria-label="Physical cup opening, 4.25 inches">
      {premiumFamily ? <ellipse cx={cup.x + 1.2} cy={cup.y + 1.7} rx={Math.max(2, cupMarkerRadius * 1.3)} ry={Math.max(1.3, cupMarkerRadius * .8)} fill="#223e23" opacity=".38" pointerEvents="none"/> : null}
      <circle cx={cup.x} cy={cup.y} r={cupMarkerRadius} fill={premiumFamily ? "#0c1811" : PHYSICAL_PUTTING_SVG_STYLE.cupFill} stroke={premiumFamily ? "#c8d9b0" : "var(--gold)"} strokeWidth={Math.max(.6, cupMarkerRadius * .22)} pointerEvents="none" />
      {premiumFamily ? <g pointerEvents="none" opacity=".86" aria-label="Flag anchored at cup">
        <path d={`M${cup.x} ${cup.y} v ${-flagHeight}`} stroke="#f3eee1" strokeWidth={Math.min(1.5, flagScale)}/>
        <path d={`M${cup.x} ${cup.y - flagHeight} l ${flagWidth} ${flagHalfHeight} ${-flagWidth} ${flagHalfHeight} Z`} fill="#e45d48"/>
      </g> : null}
    </g>
    {ballLocatorRadius ? <g aria-label="Ball location aid, not to scale" pointerEvents="none">
      <circle cx={ballPoint.x} cy={ballPoint.y} r={ballLocatorRadius} fill="none" stroke={premiumFamily ? "#fff" : "var(--foreground)"} strokeWidth="1.2" strokeOpacity=".88" />
    </g> : null}
    <g aria-label="Golf ball, 1.68 inches">
      {premiumFamily ? <ellipse cx={ballPoint.x + 1} cy={ballPoint.y + 1.3} rx={Math.max(1.5, ballMarkerRadius * 1.2)} ry={Math.max(.8, ballMarkerRadius * .65)} fill="#142d1d" opacity=".48" pointerEvents="none"/> : null}
      <circle cx={ballPoint.x} cy={ballPoint.y} r={ballMarkerRadius} fill={premiumFamily ? `url(#${clipId}-ball)` : PHYSICAL_PUTTING_SVG_STYLE.ballFill} pointerEvents="none" />
    </g>
  </svg>;
}

function GreenReading({ samples, contours, projector, clipId, premium, showArrows }: {
  samples: NonNullable<HoleGeom["physicalGreen"]>["slopeSamples"];
  contours: NonNullable<HoleGeom["physicalGreen"]>["contours"];
  projector: PhysicalPuttingProjector;
  clipId: string;
  premium: boolean;
  showArrows: boolean;
}) {
  const sampled = visibleSlopeSamples(samples, projector);
  const readableSamples = premium ? [...sampled.reduce((cells, sample) => {
    const point = projector.project(sample.position);
    const key = `${Math.floor(point.x / 82)}:${Math.floor(point.y / 82)}`;
    const prior = cells.get(key);
    if (!prior || sample.slope > prior.slope) cells.set(key, sample);
    return cells;
  }, new Map<string, typeof sampled[number]>()).values()] : sampled;
  return <g aria-label="Green reading overlay">
    <g aria-label="Physical elevation contours" clipPath={`url(#${clipId})`}>
      {contours.map((contour, index) => <path key={`${contour.elevation}-${index}`} d={contour.points.map((point, pointIndex) => {
        const projected = projector.project(point);
        return `${pointIndex === 0 ? "M" : "L"}${projected.x} ${projected.y}`;
      }).join(" ")} fill="none" stroke={premium ? "#e5f3ba" : "var(--foreground)"} strokeWidth={premium ? 1 : 1.2} strokeOpacity={showArrows ? premium ? .35 : .52 : .10} pointerEvents="none" />)}
    </g>
    {showArrows ? readableSamples.map((sample, index) => {
      const point = projector.project(sample.position);
      const severity = Math.min(1, sample.slope / .04);
      const magnitude = 8 + severity * 8;
      const end = { x: point.x + sample.downhillX * magnitude, y: point.y - sample.downhillY * magnitude };
      return <g key={index} opacity={premium ? ".64" : ".85"} pointerEvents="none">
        <circle cx={point.x} cy={point.y} r={1.4 + severity * 1.8} fill={premium ? "#b9dd9d" : "var(--info)"} opacity=".5" />
        {sample.slope > 0.002 ? <path d={`M${point.x} ${point.y} L${end.x} ${end.y}`} stroke={premium ? "#b9dd9d" : "var(--info)"} strokeWidth={1.25 + severity} markerEnd={`url(#downhill-arrow-${clipId})`} /> : null}
      </g>;
    }) : null}
    <defs><marker id={`downhill-arrow-${clipId}`} markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto"><path d="M0,0 L6,3 L0,6z" fill={premium ? "#b9dd9d" : "var(--info)"} /></marker></defs>
  </g>;
}

