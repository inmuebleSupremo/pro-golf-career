"use client";

import { useId, useLayoutEffect, useMemo, useRef, useState, type PointerEvent } from "react";

import type { HoleGeom, Point, ResolvedShot } from "../../lib/play/hole-geometry";
import { PHYSICAL_BALL_RADIUS_YARDS, PHYSICAL_CUP_RADIUS_YARDS, PHYSICAL_PUTTING_PRESENTATION, PHYSICAL_PUTTING_SVG_STYLE, physicalAimGuideEndpoint, physicalAimGuideVisible, physicalBallLocatorRadius, physicalCupLocatorRadius, physicalPuttPlaybackDurationMs, physicalPuttingProjector, physicalTrajectoryPoint, physicalTraversedTrajectory, physicalWorldMarkerRadius, type PhysicalPuttingProjector, visibleSlopeSamples } from "../../lib/play/physical-putting";

/** A green-local SVG: all terrain facts, slope arrows and ball motion come from backend physical data. */
export function PhysicalPuttingGreen({
  hole,
  ball,
  aimDirection,
  onAimDirection,
  showReading,
  onPlaybackComplete,
}: {
  hole: HoleGeom;
  ball: ResolvedShot | null;
  aimDirection: Point;
  onAimDirection: (direction: Point) => void;
  showReading: boolean;
  onPlaybackComplete?: () => void;
}) {
  const svgRef = useRef<SVGSVGElement>(null);
  const clipId = useId().replace(/:/g, "");
  const trajectory = useMemo(() => ball?.physicalPutt?.trajectory ?? [], [ball?.physicalPutt?.trajectory]);
  const origin = trajectory[0]?.position ?? hole.ball?.position ?? hole.geometry!.tee;
  const green = useMemo(() => hole.geometry!.regions.find((region) => region.surface === "GREEN")?.boundary ?? [], [hole.geometry]);
  const projector = useMemo(() => physicalPuttingProjector(origin, hole.geometry!.cup, green), [green, origin, hole.geometry]);
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
    const start = performance.now();
    // Preserve solver time ordering while keeping a decision readable in the existing shot sequence.
    const duration = physicalPuttPlaybackDurationMs(trajectory);
    const tick = (now: number) => {
      const next = Math.min(1, (now - start) / duration);
      setAnimation({ trajectory, elapsed: next });
      if (next < 1) frame = requestAnimationFrame(tick);
      else if (completedTrajectory.current !== trajectory) {
        completedTrajectory.current = trajectory;
        playbackCompleteRef.current?.();
      }
    };
    frame = requestAnimationFrame((now) => {
      setAnimation({ trajectory, elapsed: 0 });
      frame = requestAnimationFrame(() => tick(now));
    });
    return () => cancelAnimationFrame(frame);
  }, [trajectory]);

  const traversedTrajectory = physicalTraversedTrajectory(trajectory, elapsed);
  const moving = trajectory.length ? physicalTrajectoryPoint(trajectory, elapsed) : origin;
  const ballPoint = projector.project(moving);
  const cup = projector.project(hole.geometry!.cup);
  const originPoint = projector.project(origin);
  const path = traversedTrajectory.length > 1 ? traversedTrajectory.map((sample, index) => `${index === 0 ? "M" : "L"}${projector.project(sample.position).x} ${projector.project(sample.position).y}`).join(" ") : null;
  const greenPath = polygonPath(green, projector);
  const aimEnd = projector.project(physicalAimGuideEndpoint(origin, aimDirection,
    Math.hypot(hole.geometry!.cup.x - origin.x, hole.geometry!.cup.y - origin.y)));
  const cupMarkerRadius = physicalWorldMarkerRadius(PHYSICAL_CUP_RADIUS_YARDS, projector.pixelsPerYard);
  const ballMarkerRadius = physicalWorldMarkerRadius(PHYSICAL_BALL_RADIUS_YARDS, projector.pixelsPerYard);
  const cupLocatorRadius = physicalCupLocatorRadius(cupMarkerRadius);
  const ballLocatorRadius = physicalBallLocatorRadius(ballMarkerRadius);

  function chooseLine(event: PointerEvent<SVGSVGElement>) {
    if (trajectory.length > 0 || !svgRef.current) return;
    const rect = svgRef.current.getBoundingClientRect();
    const target = projector.unproject({ x: (event.clientX - rect.left) * PHYSICAL_PUTTING_PRESENTATION.width / rect.width,
      y: (event.clientY - rect.top) * PHYSICAL_PUTTING_PRESENTATION.height / rect.height });
    const dx = target.x - origin.x;
    const dy = target.y - origin.y;
    const length = Math.hypot(dx, dy);
    if (length > 0.01) onAimDirection({ x: dx / length, y: dy / length });
  }

  return <svg ref={svgRef} viewBox={`0 0 ${PHYSICAL_PUTTING_PRESENTATION.width} ${PHYSICAL_PUTTING_PRESENTATION.height}`} className="block max-h-full max-w-full" role="img"
    aria-label="Physical putting green: choose a starting line by clicking the green" onPointerDown={chooseLine}>
    <rect width={PHYSICAL_PUTTING_PRESENTATION.width} height={PHYSICAL_PUTTING_PRESENTATION.height} fill={PHYSICAL_PUTTING_SVG_STYLE.canvasFill} />
    <defs><clipPath id={clipId}><path d={greenPath} /></clipPath></defs>
    <path d={greenPath} fill={PHYSICAL_PUTTING_SVG_STYLE.greenFill} fillOpacity=".62" stroke="var(--border-strong)" strokeWidth="2" />
    <path d={greenPath} fill="none" stroke="var(--foreground)" strokeOpacity=".18" strokeWidth=".8" />
    {showReading ? <GreenReading samples={hole.physicalGreen?.slopeSamples ?? []}
      contours={hole.physicalGreen?.contours ?? []} projector={projector} clipId={clipId} /> : null}
    {path ? <path d={path} fill="none" stroke="var(--foreground)" strokeWidth="1.5" strokeOpacity=".72" /> : null}
    {physicalAimGuideVisible(trajectory.length > 0) ? <g aria-label="Submitted starting direction">
      <line x1={originPoint.x} y1={originPoint.y} x2={aimEnd.x} y2={aimEnd.y} stroke={PHYSICAL_PUTTING_SVG_STYLE.guideOutline} strokeWidth="4" strokeLinecap="round" strokeOpacity=".7" />
      <line x1={originPoint.x} y1={originPoint.y} x2={aimEnd.x} y2={aimEnd.y} stroke={PHYSICAL_PUTTING_SVG_STYLE.guideStroke} strokeWidth="2" strokeDasharray="6 4" strokeLinecap="round" />
    </g> : null}
    {cupLocatorRadius ? <g aria-label="Cup location aid, not to scale" pointerEvents="none">
      <circle cx={cup.x} cy={cup.y} r={cupLocatorRadius} fill="none" stroke="var(--gold)" strokeWidth="1.2" strokeDasharray="2 2" />
    </g> : null}
    <g aria-label="Physical cup opening, 4.25 inches">
      <circle cx={cup.x} cy={cup.y} r={cupMarkerRadius} fill={PHYSICAL_PUTTING_SVG_STYLE.cupFill} stroke="var(--gold)" strokeWidth={Math.max(.6, cupMarkerRadius * .22)} />
    </g>
    {ballLocatorRadius ? <g aria-label="Ball location aid, not to scale" pointerEvents="none">
      <circle cx={ballPoint.x} cy={ballPoint.y} r={ballLocatorRadius} fill="none" stroke="var(--foreground)" strokeWidth="1.2" strokeOpacity=".88" />
    </g> : null}
    <g aria-label="Golf ball, 1.68 inches">
      <circle cx={ballPoint.x} cy={ballPoint.y} r={ballMarkerRadius} fill={PHYSICAL_PUTTING_SVG_STYLE.ballFill} />
    </g>
  </svg>;
}

function GreenReading({ samples, contours, projector, clipId }: {
  samples: NonNullable<HoleGeom["physicalGreen"]>["slopeSamples"];
  contours: NonNullable<HoleGeom["physicalGreen"]>["contours"];
  projector: PhysicalPuttingProjector;
  clipId: string;
}) {
  const readableSamples = visibleSlopeSamples(samples, projector);
  return <g aria-label="Green reading overlay">
    <g aria-label="Physical elevation contours" clipPath={`url(#${clipId})`}>
      {contours.map((contour, index) => <path key={`${contour.elevation}-${index}`} d={contour.points.map((point, pointIndex) => {
        const projected = projector.project(point);
        return `${pointIndex === 0 ? "M" : "L"}${projected.x} ${projected.y}`;
      }).join(" ")} fill="none" stroke="var(--foreground)" strokeWidth="1.2" strokeOpacity=".52" />)}
    </g>
    {readableSamples.map((sample, index) => {
      const point = projector.project(sample.position);
      const severity = Math.min(1, sample.slope / .04);
      const magnitude = 8 + severity * 8;
      const end = { x: point.x + sample.downhillX * magnitude, y: point.y - sample.downhillY * magnitude };
      return <g key={index} opacity=".85">
        <circle cx={point.x} cy={point.y} r={1.4 + severity * 1.8} fill="var(--info)" opacity=".5" />
        {sample.slope > 0.002 ? <path d={`M${point.x} ${point.y} L${end.x} ${end.y}`} stroke="var(--info)" strokeWidth={1.25 + severity} markerEnd={`url(#downhill-arrow-${clipId})`} /> : null}
      </g>;
    })}
    <defs><marker id={`downhill-arrow-${clipId}`} markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto"><path d="M0,0 L6,3 L0,6z" fill="var(--info)" /></marker></defs>
  </g>;
}

function polygonPath(points: readonly Point[], projector: PhysicalPuttingProjector): string {
  return points.map((point, index) => {
    const projected = projector.project(point);
    return `${index === 0 ? "M" : "L"}${projected.x} ${projected.y}`;
  }).join(" ") + " Z";
}

