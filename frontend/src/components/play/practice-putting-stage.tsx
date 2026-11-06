"use client";

import { useId, useLayoutEffect, useMemo, useRef, useState, type PointerEvent } from "react";

import { Button } from "@/components/ui/button";
import { GreenReading } from "@/components/play/physical-putting-green";
import { usePhysicalPuttPlayback } from "@/components/play/use-physical-putt-playback";
import { pointerWorldTarget } from "@/lib/play/canonical-geometry";
import type { PracticePhysicalPuttMutation, PracticePuttingGreenQuery } from "@/lib/graphql/generated/graphql";
import type { Point } from "@/lib/play/hole-geometry";
import { PHYSICAL_BALL_RADIUS_YARDS, PHYSICAL_CUP_RADIUS_YARDS, physicalAimGuideEndpoint,
  physicalAimGuideVisible, physicalTrajectoryPoint, physicalTraversedTrajectory,
  physicalWorldMarkerRadius } from "@/lib/play/physical-putting";
import { pointInPolygon, puttingPolygonPath } from "@/lib/play/premium-putting";
import { PRACTICE_PUTTING_SCENE, practicePuttingAimDirection, practicePuttingEnvironmentViewBox, practicePuttingFocusViewBox, practicePuttingProjector,
  practicePuttingWideViewBox } from "@/lib/play/practice-putting-geometry";

type GreenState = PracticePuttingGreenQuery["practicePuttingGreen"];
type PracticePutt = PracticePhysicalPuttMutation["practicePhysicalPutt"];
const landscapeTrees = [
  [38, 95, 34], [121, 47, 28], [248, 36, 25], [393, 34, 25], [1112, 55, 30], [1230, 105, 39],
  [22, 256, 35], [1251, 284, 40], [16, 441, 38], [1245, 472, 43], [63, 648, 37], [1166, 654, 40],
] as const;

export function PracticePuttingStage({ green, selectedCup, direction, placing, reading, focus, locked,
  playback, training, onSelectCup, onAim, onPlace, onFocusToggle, onPlaybackComplete }: {
  green: GreenState;
  selectedCup: number;
  direction: Point;
  placing: boolean;
  reading: boolean;
  focus: boolean;
  locked: boolean;
  playback: Pick<PracticePutt, "outcome"> | null;
  training?: { number: number; distance: number; slope: number };
  onSelectCup: (number: number) => void;
  onAim: (direction: Point) => void;
  onPlace: (position: Point) => void;
  onFocusToggle: () => void;
  onPlaybackComplete: () => void;
}) {
  const stageRef = useRef<HTMLDivElement>(null);
  const svgRef = useRef<SVGSVGElement>(null);
  const [size, setSize] = useState({ width: 1280, height: 720 });
  const id = useId().replace(/:/g, "");
  const cup = green.cups.find((candidate) => candidate.number === selectedCup) ?? green.cups[0];
  const playbackBall = useMemo(() => playback ? { physicalPutt: playback.outcome, holed: playback.outcome.holed } : null, [playback]);
  const { trajectory, elapsed } = usePhysicalPuttPlayback(playbackBall, onPlaybackComplete);
  const moving = trajectory.length ? physicalTrajectoryPoint(trajectory, elapsed) : green.readyPosition;
  const traversed = physicalTraversedTrajectory(trajectory, elapsed);
  const projector = practicePuttingProjector;
  const greenPath = puttingPolygonPath(green.footprint, projector);
  const collarPath = puttingPolygonPath(green.footprint.map((point) => ({ x: point.x * 1.055, y: point.y * 1.055 })), projector);
  const view = size.width <= 700 && focus
    ? practicePuttingFocusViewBox(green.readyPosition, cup.position, size.width, size.height)
    : size.width <= 700
      ? { x: 0, y: 0, width: PRACTICE_PUTTING_SCENE.width, height: PRACTICE_PUTTING_SCENE.height }
      : practicePuttingWideViewBox(size.width, size.height);
  const scene = practicePuttingEnvironmentViewBox(view, size.width, size.height);
  const visibleTrees = useMemo(() => {
    const trees: { key: string; x: number; y: number; radius: number }[] = [];
    for (const offsetX of [-1280, 0, 1280]) for (const offsetY of [-720, 0, 720]) {
      landscapeTrees.forEach(([baseX, baseY, radius], index) => {
        const x = baseX + offsetX;
        const y = baseY + offsetY;
        if (x + radius >= scene.x && x - radius <= scene.x + scene.width
          && y + radius >= scene.y && y - radius <= scene.y + scene.height) {
          trees.push({ key: `${offsetX}-${offsetY}-${index}`, x, y, radius });
        }
      });
    }
    return trees;
  }, [scene.x, scene.y, scene.width, scene.height]);
  const ready = projector.project(green.readyPosition);
  const aimEnd = projector.project(physicalAimGuideEndpoint(green.readyPosition, direction,
    Math.hypot(cup.position.x - green.readyPosition.x, cup.position.y - green.readyPosition.y)));
  const cupDistanceFeet = Math.round(Math.hypot(cup.position.x - green.readyPosition.x,
    cup.position.y - green.readyPosition.y) * 3);
  const cupRadius = physicalWorldMarkerRadius(PHYSICAL_CUP_RADIUS_YARDS, projector.pixelsPerYard);
  const ballRadius = physicalWorldMarkerRadius(PHYSICAL_BALL_RADIUS_YARDS, projector.pixelsPerYard);

  useLayoutEffect(() => {
    const stage = stageRef.current;
    if (!stage) return;
    const measure = () => {
      const rect = stage.getBoundingClientRect();
      if (rect.width && rect.height) setSize((prior) => prior.width === rect.width && prior.height === rect.height
        ? prior : { width: rect.width, height: rect.height });
    };
    const observer = new ResizeObserver(measure);
    observer.observe(stage);
    measure();
    return () => observer.disconnect();
  }, []);

  function choosePoint(event: PointerEvent<SVGSVGElement>) {
    if (locked || !svgRef.current || (!training && (event.target as Element).closest("[data-practice-cup]"))) return;
    const matrix = svgRef.current.getScreenCTM();
    if (!matrix) return;
    const world = pointerWorldTarget({ x: event.clientX, y: event.clientY }, matrix.inverse(), projector.unproject);
    if (!pointInPolygon(world, green.footprint)) return;
    if (placing && !training) { onPlace(world); return; }
    const direction = practicePuttingAimDirection(green.readyPosition, world);
    if (direction) onAim(direction);
  }

  return <main ref={stageRef} className="relative min-h-0 min-w-0 flex-1 overflow-hidden bg-[var(--practice-green-ground-mid)]">
    <svg ref={svgRef} viewBox={`${scene.x} ${scene.y} ${scene.width} ${scene.height}`} preserveAspectRatio="xMidYMid meet"
      role="img" aria-label="Five-cup practice putting green; tap a cup or choose a putting line"
      className="absolute inset-0 h-full w-full touch-none" onPointerDown={choosePoint}>
      <defs>
        <linearGradient id={`${id}-ground`} x1="0" y1="0" x2="1" y2="1">
          <stop stopColor="var(--practice-green-ground-light)" /><stop offset=".55" stopColor="var(--practice-green-ground-mid)" />
          <stop offset="1" stopColor="var(--practice-green-ground-dark)" />
        </linearGradient>
        <radialGradient id={`${id}-leaf`} cx="32%" cy="26%" r="80%">
          <stop stopColor="var(--range-leaf-light)" /><stop offset=".52" stopColor="var(--range-leaf-mid)" />
          <stop offset="1" stopColor="var(--range-leaf-dark)" />
        </radialGradient>
        <linearGradient id={`${id}-turf`} x1="0" y1="0" x2=".85" y2="1">
          <stop stopColor="var(--practice-green-turf-light)" /><stop offset=".43" stopColor="var(--practice-green-turf-mid)" />
          <stop offset="1" stopColor="var(--practice-green-turf-dark)" />
        </linearGradient>
        <radialGradient id={`${id}-light`} cx="37%" cy="28%" r="78%">
          <stop stopColor="var(--practice-green-sun)" stopOpacity=".18" />
          <stop offset="1" stopColor="var(--practice-green-sun)" stopOpacity="0" />
        </radialGradient>
        <pattern id={`${id}-grain`} width="57" height="49" patternUnits="userSpaceOnUse">
          <path d="M8 16l2-3 M35 29l2-4 M47 9l1-2" stroke="var(--practice-green-grain)" strokeWidth=".8" opacity=".18" />
        </pattern>
        <pattern id={`${id}-rough`} width="53" height="45" patternUnits="userSpaceOnUse">
          <path d="M7 11l2-5 M29 33l2-4 M43 17l1-4" stroke="var(--range-grain-light)" strokeWidth=".8" opacity=".2" />
        </pattern>
        <clipPath id={`${id}-clip`}><path d={greenPath} /></clipPath>
        <mask id={`${id}-outside`} maskUnits="userSpaceOnUse" x={scene.x} y={scene.y} width={scene.width} height={scene.height}>
          <rect x={scene.x} y={scene.y} width={scene.width} height={scene.height} fill="white" /><path d={greenPath} fill="black" />
        </mask>
      </defs>
      <rect x={scene.x} y={scene.y} width={scene.width} height={scene.height} fill={`url(#${id}-ground)`} />
      <g mask={`url(#${id}-outside)`} pointerEvents="none">
        <rect x={scene.x} y={scene.y} width={scene.width} height={scene.height} fill={`url(#${id}-rough)`} />
        {visibleTrees.map(({ key, x, y, radius }) => <g key={key} transform={`translate(${x} ${y})`}>
          <ellipse cy={radius * .65} rx={radius * 1.08} ry={radius * .46} fill="var(--range-edge-dark)" opacity=".24" />
          <path d={`M${-radius} 0Q${-radius * 1.15} ${-radius * .75} ${-radius * .35} ${-radius * .78}Q0 ${-radius * 1.2} ${radius * .42} ${-radius * .55}Q${radius * 1.2} ${-radius * .38} ${radius * .9} ${radius * .38}Q0 ${radius * .83} ${-radius} ${radius * .35}Z`}
            fill={`url(#${id}-leaf)`} />
        </g>)}
        <path d="M0 635Q120 601 170 653L190 720H0Z" fill="var(--practice-green-path)" opacity=".8" />
        <rect x={scene.x} y={scene.y} width={scene.width} height={scene.height} fill={`url(#${id}-light)`} opacity=".35" />
      </g>
      <path d={collarPath} fill="var(--practice-green-collar)" stroke="var(--practice-green-collar-edge)" strokeWidth="10" strokeLinejoin="round" />
      <path d={greenPath} fill={`url(#${id}-turf)`} stroke="var(--practice-green-rim)" strokeWidth="4" strokeLinejoin="round" />
      <g clipPath={`url(#${id}-clip)`} pointerEvents="none">
        {Array.from({ length: 18 }, (_, index) => <path key={index}
          d={`M${index * 88 - 250} -250L${index * 88 - 60} 970`} stroke={index % 2 ? "var(--practice-green-mow-light)" : "var(--practice-green-mow-dark)"}
          strokeWidth="88" opacity=".045" />)}
        <ellipse cx="290" cy="245" rx="260" ry="230" fill="var(--practice-green-sun)" opacity=".045" />
        <ellipse cx="805" cy="235" rx="240" ry="220" fill="var(--practice-green-mow-dark)" opacity=".055" />
        <ellipse cx="635" cy="490" rx="280" ry="160" fill="var(--practice-green-sun)" opacity=".035" />
        <rect width="1280" height="720" fill={`url(#${id}-light)`} />
        <rect width="1280" height="720" fill={`url(#${id}-grain)`} />
      </g>
      <GreenReading samples={green.physicalGreen.slopeSamples} contours={green.physicalGreen.contours}
        projector={projector} clipId={`${id}-clip`} premium showArrows={reading} />
      {physicalAimGuideVisible(trajectory.length > 0) && <g pointerEvents="none">
        <line x1={ready.x} y1={ready.y} x2={aimEnd.x} y2={aimEnd.y} stroke="var(--practice-green-guide-shadow)" strokeWidth="4" strokeLinecap="round" />
        <line x1={ready.x} y1={ready.y} x2={aimEnd.x} y2={aimEnd.y} stroke="var(--practice-green-guide)" strokeWidth="2" strokeDasharray="9 8" strokeLinecap="round" />
      </g>}
      {traversed.length > 1 && <path d={traversed.map((sample, index) => {
        const point = projector.project(sample.position);
        return `${index ? "L" : "M"}${point.x} ${point.y}`;
      }).join(" ")} fill="none" stroke="var(--practice-green-guide)" strokeWidth="1.6" opacity=".8" pointerEvents="none" />}
      {green.restingBalls.map((ball) => <BallMarker key={ball.number} number={ball.number} point={projector.project(ball.position)} radius={ballRadius} />)}
      <BallMarker point={projector.project(moving)} radius={ballRadius} ready={!playback} />
      {green.cups.map((target) => {
        const point = projector.project(target.position);
        const selected = target.number === selectedCup;
        return <g key={target.number} data-practice-cup={target.number} transform={`translate(${point.x} ${point.y})`}
          pointerEvents={training ? "none" : undefined}
          role="button" tabIndex={locked || training ? -1 : 0} aria-label={`Select cup ${target.number}`} aria-pressed={selected}
          aria-disabled={!!training}
          onPointerDown={training ? undefined : (event) => event.stopPropagation()}
          onClick={() => { if (!locked && !training) onSelectCup(target.number); }}
          onKeyDown={(event) => { if ((event.key === "Enter" || event.key === " ") && !locked && !training) {
            event.preventDefault(); onSelectCup(target.number);
          } }} className="cursor-pointer focus-visible:outline-2 focus-visible:outline-ring">
          {selected && <circle r="25" fill="none" stroke="var(--gold)" strokeWidth="1.2" strokeDasharray="4 5" />}
          <circle r="8" fill="var(--practice-green-cup-rim)" pointerEvents="none" />
          <circle r={cupRadius} fill="var(--practice-green-cup)" stroke="var(--practice-green-cup-rim)" strokeWidth=".8" pointerEvents="none" />
          <path d="M0 0L1.5 -25" stroke="var(--foreground)" strokeWidth="2.2" pointerEvents="none" />
          <path d="M1 -24L17 -20L1 -15Z" fill={selected ? "var(--gold)" : "var(--practice-green-flag)"} pointerEvents="none" />
          <rect x="17" y="-40" width="62" height="23" rx="5" fill="var(--practice-green-label)" opacity=".9" pointerEvents="none" />
          <text x="48" y="-24" textAnchor="middle" fill={selected ? "var(--gold)" : "var(--foreground)"} fontSize="12" fontWeight="700" pointerEvents="none">CUP {target.number}</text>
          <circle r="26" fill="transparent" />
        </g>;
      })}
    </svg>
    <div className={`pointer-events-none absolute top-2 flex flex-wrap gap-2 text-[10px] sm:top-3 sm:text-xs ${training
      ? "left-1/2 max-w-[80vw] -translate-x-1/2 justify-center text-center" : "left-2 sm:left-3"}`}>
      <span className="border-border-strong bg-background/85 text-foreground rounded-md border px-2 py-1.5">
        {training ? `Challenge ${training.number} of 8 · Cup ${selectedCup} · ${Math.round(training.distance * 3)} ft · Slope ${Math.round(training.slope * 100)}%` : placing ? "Place ball: tap a clear point on the green" : "Tap a cup to select · Tap green to set your line"}
      </span>
      {!training ? <span className="border-border-strong bg-background/85 text-foreground rounded-md border px-2 py-1.5">Cup {selectedCup} · {cupDistanceFeet} ft</span> : null}
    </div>
    <Button variant="secondary" size="sm" onClick={onFocusToggle}
      className={`absolute right-2 hidden max-[700px]:inline-flex ${training ? "top-12" : "top-2"}`} aria-pressed={focus}>
      {focus ? "Overview" : "Focus view"}
    </Button>
    <p className="border-border-strong bg-background/85 text-muted-foreground pointer-events-none absolute bottom-2 left-2 hidden rounded-md border px-2 py-1 text-[10px] sm:block">
      {training ? "Target cup and starting ball are fixed for this challenge. Tap the green to set your line." : "Switch to Place ball to choose a starting position. Resting balls stay where they stop."}
    </p>
  </main>;
}

function BallMarker({ number, point, radius, ready = false }: { number?: number; point: Point; radius: number; ready?: boolean }) {
  return <g pointerEvents="none" data-practice-ball={number ?? "ready"}>
    <ellipse cx={point.x + 1} cy={point.y + 2} rx="7" ry="4" fill="var(--practice-green-ball-shadow)" opacity=".5" />
    <circle cx={point.x} cy={point.y} r={ready ? 7 : 6} fill="var(--foreground)" stroke="var(--practice-green-ball-edge)" strokeWidth=".8" />
    <circle cx={point.x} cy={point.y} r={radius} fill="var(--foreground)" />
  </g>;
}
