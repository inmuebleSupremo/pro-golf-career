"use client";

import { useCallback, useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { BallStrikeDock, Detail, type BallStrikeInput } from "@/components/play/ball-strike-dock";
import { PracticeRangeScenery } from "@/components/play/practice-range-scenery";
import { PracticeFacilityHeader } from "@/components/play/practice-facility-nav";
import { ShotMotionOverlay } from "@/components/play/shot-motion-overlay";
import { validShotSelection, type ShotSelection, type Situation } from "@/components/play/play-shared";
import { gqlRequest } from "@/lib/api/graphql-client";
import { PracticeRangeDocument, PracticeShotDocument } from "@/lib/graphql/operations";
import type { Point } from "@/lib/play/hole-geometry";
import { golfAudio } from "@/lib/play/golf-audio";
import { projectRangePoint, projectRangeShotPoint, rangeBayMat, rangeBayOrigin, RANGE_BAY_CENTERS,
  rangeFieldPath, rangeScaleAt, RANGE_MOBILE_VIEWBOX, RANGE_SCENE, unprojectRangePoint } from "@/lib/play/practice-range-geometry";

const INITIAL_AIM: Point = { x: 95, y: 116 };
const markers = [50, 100, 150, 200, 250, 300];
const MAX_RESTING_BALLS = 30;

export function DrivingRange({ id }: { id: string }) {
  type PracticeResult = Awaited<ReturnType<typeof hitRequest>>["practiceShot"];
  const [mobileScene, setMobileScene] = useState(false);
  const [selectedBay, setSelectedBay] = useState(4);
  const queryClient = useQueryClient();
  const range = useQuery({ queryKey: ["practiceRange", id], queryFn: () => gqlRequest(PracticeRangeDocument, { id }), retry: false });
  const [aim, setAim] = useState<Point>(INITIAL_AIM);
  const [requestedSelection, setRequestedSelection] = useState<ShotSelection>({ club: "SEVEN_IRON", technique: "FULL", shape: "STRAIGHT" });
  const [selectedFlag, setSelectedFlag] = useState("150 yd");
  const [result, setResult] = useState<PracticeResult | null>(null);
  const [playbackShot, setPlaybackShot] = useState<PracticeResult | null>(null);
  const [restingBalls, setRestingBalls] = useState<{ number: number; position: Point }[]>([]);
  const mutation = useMutation({ mutationFn: hitRequest, onSuccess: (data) => {
    setResult(data.practiceShot);
    setPlaybackShot(data.practiceShot.outcome.trace ? data.practiceShot : null);
    queryClient.setQueryData(["practiceRange", id], (old: typeof range.data) => old ? {
      ...old, practiceRange: { ...old.practiceRange, ballsHit: data.practiceShot.ballNumber,
        shotRevision: String(data.practiceShot.ballNumber) },
    } : old);
  } });

  const completePlayback = useCallback((shot: PracticeResult) => {
    const finalPoint = shot.outcome.trace?.finalPoint;
    if (finalPoint) setRestingBalls((balls) => balls.some((ball) => ball.number === shot.ballNumber) ? balls
      : [...balls, { number: shot.ballNumber, position: finalPoint }].slice(-MAX_RESTING_BALLS));
    setPlaybackShot((current) => current?.ballNumber === shot.ballNumber ? null : current);
  }, []);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    golfAudio.startScene("parkland");
    return () => {
      document.body.style.overflow = previousOverflow;
      golfAudio.stopScene();
    };
  }, []);

  useEffect(() => {
    const mobile = window.matchMedia("(max-width: 650px)");
    const update = () => setMobileScene(mobile.matches);
    mobile.addEventListener("change", update);
    update();
    return () => {
      mobile.removeEventListener("change", update);
    };
  }, []);

  useEffect(() => {
    if (!playbackShot) return;
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");
    let frame = 0;
    const finishReducedPlayback = () => {
      if (reduced.matches) frame = requestAnimationFrame(() => completePlayback(playbackShot));
    };
    reduced.addEventListener("change", finishReducedPlayback);
    finishReducedPlayback();
    return () => {
      reduced.removeEventListener("change", finishReducedPlayback);
      cancelAnimationFrame(frame);
    };
  }, [playbackShot, completePlayback]);

  async function hitRequest(intent: BallStrikeInput) { return gqlRequest(PracticeShotDocument, { id, bayNumber: selectedBay, intent }); }

  const bayOrigin = rangeBayOrigin(selectedBay);
  const bayMat = rangeBayMat(selectedBay);
  const targetPoint = projectRangePoint(aim);
  const trace = playbackShot?.outcome.trace;
  const contact = trace ? projectRangeShotPoint(trace.contact.position) : null;
  const finish = trace ? projectRangeShotPoint(trace.finalPoint) : null;
  const aimHalfWidth = range.data?.practiceRange.aimHalfWidth ?? 220;
  const fieldPath = rangeFieldPath(range.data?.practiceRange.fieldHalfWidth ?? 250);
  const targetGreenRadius = range.data?.practiceRange.targetGreenRadius ?? 11;
  const situation: Situation = {
    holeNumber: 1, par: 0, shotNumber: (range.data?.practiceRange.ballsHit ?? 0) + 1,
    strokesThisHole: 0, distanceToPin: Math.hypot(aim.x - bayOrigin.x, aim.y - bayOrigin.y), lie: "TEE_BOX", pinLateral: 0,
    minReach: 0, maxReach: 320, shotRevision: range.data?.practiceRange.shotRevision ?? "0",
    aimEnvelope: { minX: -aimHalfWidth, maxX: aimHalfWidth, minY: 15, maxY: 320 },
    guidance: range.data ? {
      safe: INITIAL_AIM, primary: INITIAL_AIM, aggressive: INITIAL_AIM,
      clubs: range.data.practiceRange.clubs, strategicOptions: [],
    } : null,
  };
  const selection = validShotSelection(situation, requestedSelection);

  function selectAim(point: Point, label: string) {
    if (playbackShot || mutation.isPending) return;
    setAim(point);
    setSelectedFlag(label);
  }

  async function hit(intent: BallStrikeInput) { await mutation.mutateAsync(intent); }

  return <div role="dialog" aria-modal="true" aria-label="Practice Facility driving range"
    className="practice-facility-page practice-range-page bg-background fixed inset-0 z-[var(--z-modal)] isolate flex h-dvh min-h-0 min-w-0 flex-col overflow-hidden">
    <PracticeFacilityHeader id={id} active="range" title="Driving Range" subtitle="Unlimited balls · Seven targets" />

    <div className="border-border-strong relative min-h-0 min-w-0 flex-1 overflow-hidden bg-surface">
      <svg viewBox={mobileScene ? `${RANGE_MOBILE_VIEWBOX.x} 0 ${RANGE_MOBILE_VIEWBOX.width} ${RANGE_SCENE.height}` : `0 0 ${RANGE_SCENE.width} ${RANGE_SCENE.height}`}
        preserveAspectRatio="none" role="img"
        aria-label="Driving range with seven selectable target flags, distance markers, and seven selectable hitting bays"
        className="practice-range block h-full w-full">
        <PracticeRangeScenery fieldPath={fieldPath} selectedBay={selectedBay} />
        {markers.map((distance) => {
          const left = projectRangePoint({ x: -(range.data?.practiceRange.fieldHalfWidth ?? 250) + 14, y: distance });
          const right = projectRangePoint({ x: (range.data?.practiceRange.fieldHalfWidth ?? 250) - 14, y: distance });
          return <g key={distance} pointerEvents="none">
            <path d={`M${left.x - 22} ${left.y}l25-2 M${right.x + 22} ${right.y}l-25-2`}
              stroke="var(--range-line)" strokeWidth="1.5" opacity=".38" />
            <text x={left.x} y={left.y - 11} fill="var(--range-label)" stroke="var(--range-edge)" strokeWidth="3"
              paintOrder="stroke" fontSize="12" fontWeight="700" opacity=".8">{distance}</text>
            <text x={right.x} y={right.y - 11} textAnchor="end" fill="var(--range-label)" stroke="var(--range-edge)"
              strokeWidth="3" paintOrder="stroke" fontSize="12" fontWeight="700" opacity=".8">{distance}</text>
          </g>;
        })}
        <path d={fieldPath} fill="transparent" className="cursor-crosshair"
          onClick={(event) => {
            if (playbackShot || mutation.isPending) return;
            const matrix = event.currentTarget.ownerSVGElement?.getScreenCTM();
            if (!matrix) return;
            const point = new DOMPoint(event.clientX, event.clientY).matrixTransform(matrix.inverse());
            selectAim(unprojectRangePoint(point, aimHalfWidth), "Free aim");
          }} />
        {RANGE_BAY_CENTERS.map((center, index) => <g key={index} role="button" tabIndex={playbackShot || mutation.isPending ? -1 : 0}
          aria-label={`Select hitting bay ${index + 1}`} aria-pressed={selectedBay === index + 1}
          onClick={() => { if (!playbackShot && !mutation.isPending) setSelectedBay(index + 1); }}
          onKeyDown={(event) => { if ((event.key === "Enter" || event.key === " ") && !playbackShot && !mutation.isPending) {
            event.preventDefault(); setSelectedBay(index + 1);
          } }} className="cursor-pointer focus-visible:outline-2 focus-visible:outline-ring">
          <rect x={center - 80} y="625" width="160" height="75" fill="transparent" />
        </g>)}
        {(range.data?.practiceRange.targets ?? []).map((target) => <TargetGreen key={target.label}
          distanceLabel={`${Math.round(Math.hypot(target.position.x - bayOrigin.x, target.position.y - bayOrigin.y))} yd`}
          point={projectRangePoint(target.position)} radiusX={targetGreenRadius * rangeScaleAt(target.position.y)}
          radiusY={targetGreenRadius * (RANGE_SCENE.bayY - RANGE_SCENE.farY) / RANGE_SCENE.farDistance}
          selected={selectedFlag === target.label} disabled={!!playbackShot || mutation.isPending} onSelect={() => selectAim(target.position, target.label)} />)}
        <g pointerEvents="none">{restingBalls.map((ball) => {
          const point = projectRangeShotPoint(ball.position);
          return <circle key={ball.number} cx={point.x} cy={point.y} r="3.5" fill="var(--range-label)" stroke="var(--range-edge)" strokeWidth="1" opacity=".9" />;
        })}</g>
        {!trace && <><path d={`M${bayMat.x} ${bayMat.y} L${targetPoint.x} ${targetPoint.y}`} stroke="var(--range-aim)" strokeDasharray="8 10" strokeWidth="2" opacity=".65" pointerEvents="none" />
          <circle cx={targetPoint.x} cy={targetPoint.y} r="9" fill="none" stroke="var(--range-aim)" strokeWidth="2" pointerEvents="none" />
          <circle cx={bayMat.x} cy={bayMat.y} r="5" fill="var(--range-label)" stroke="var(--range-edge)" strokeWidth="1.5" pointerEvents="none" /></>}
        {trace && contact && finish && playbackShot && <ShotMotionOverlay trace={trace} project={projectRangeShotPoint} contact={contact} finalPoint={finish}
          contactEffect="var(--range-active)" ballStroke="var(--range-edge)" statusY={45} shadowFill="var(--range-edge)" tracerShadow
          onPlaybackComplete={() => completePlayback(playbackShot)} />}
      </svg>
      <p className="border-border-strong bg-surface/85 text-foreground pointer-events-none absolute top-2 left-2 rounded-md border px-2 py-1 text-[10px] sm:top-3 sm:left-3">Tap a bay mat, choose a flag, or tap the grass to aim</p>
    </div>

    <BallStrikeDock situation={situation} origin={bayOrigin} aimPoint={aim}
        className="practice-range-dock border-border-strong"
        onAimPoint={(point) => { if (point.x >= -aimHalfWidth && point.x <= aimHalfWidth && point.y >= 15 && point.y <= 320) selectAim(point, "Free aim"); }}
        selection={selection} onSelection={(update) => setRequestedSelection((current) => validShotSelection(situation, { ...current, ...update }))}
        onSubmit={hit} locked={range.isPending || range.isError || mutation.isPending || !!playbackShot} submitting={mutation.isPending}
        referenceLabel={selectedFlag === "Free aim" ? "Aim" : "Flag"} actionLabel="Hit ball"
        statusText={range.isError ? "Could not open the range" : playbackShot
          ? `Ball ${playbackShot.ballNumber} · Shot in flight`
          : mutation.isPending ? `Ball ${situation.shotNumber} · Resolving shot`
            : `Ball ${situation.shotNumber} · Bay ${selectedBay} · ${selectedFlag === "Free aim" ? "Free aim" : `${Math.round(situation.distanceToPin)} yd flag`} · Ready to hit`}
        extraDetails={<Detail label="Conditions" value="Calm range" />}
        review={<div className="practice-range-review border-border grid grid-cols-3 items-center gap-3 border-t text-center" aria-live="polite">
          {result && <>
            <Metric label="Carry" value={`${Math.round(result.outcome.carry)} yd`} />
            <Metric label="Total" value={`${Math.round(result.totalDistance)} yd`} />
            <Metric label="Proximity" value={`${Math.round(result.proximity)} yd`} />
          </>}
        </div>} />
  </div>;
}

function TargetGreen({ distanceLabel, point, radiusX, radiusY, selected, disabled, onSelect }: {
  distanceLabel: string; point: Point; radiusX: number; radiusY: number; selected: boolean; disabled: boolean; onSelect: () => void;
}) {
  const outline = `M${-radiusX * 1.18} 1 Q${-radiusX * 1.12} ${-radiusY * .91} ${-radiusX * .46} ${-radiusY * 1.23} Q${radiusX * .36} ${-radiusY * 1.32} ${radiusX * 1.04} ${-radiusY * .47} Q${radiusX * 1.37} ${radiusY * .31} ${radiusX * .55} ${radiusY * .99} Q${-radiusX * .38} ${radiusY * 1.31} ${-radiusX * 1.18} 1Z`;
  const flagHeight = 29;
  const labelX = radiusX * .75 + 8;
  return <g transform={`translate(${point.x} ${point.y})`} role="button" tabIndex={disabled ? -1 : 0}
    aria-label={`Aim at ${distanceLabel} flag`} aria-pressed={selected} onClick={onSelect}
    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); onSelect(); } }}
    className="cursor-pointer focus-visible:outline-2 focus-visible:outline-ring">
    <path d={outline} transform="translate(4 5) scale(1.15 1.18)" fill="var(--range-green-collar)" opacity=".24" />
    <path d={outline} transform="scale(1.23 1.23)" fill="var(--range-green-edge)" opacity=".92" />
    <path d={outline} fill="url(#range-green-fill)" stroke={selected ? "var(--range-active)" : "var(--range-green-light)"} strokeWidth={selected ? 2.5 : 1.1} />
    <path d={outline} transform="scale(.8 .74)" fill="none" stroke={selected ? "var(--range-active)" : "var(--range-line)"} strokeWidth={selected ? 2.5 : 1.2} opacity={selected ? ".95" : ".36"} />
    <path d={`M${-radiusX * .78} ${-radiusY * .18} Q0 ${-radiusY * .95} ${radiusX * .72} ${-radiusY * .32}`}
      stroke="var(--range-line)" strokeWidth="1" fill="none" opacity=".39" />
    <path d={`M0 0V${-flagHeight}`} stroke="var(--range-flagpole)" strokeWidth="2" />
    <path d={`M0 ${-flagHeight}l17 5-17 5Z`} fill="var(--range-flag)" />
    <circle r="2" fill="var(--range-edge)" />
    <rect x={labelX} y={-flagHeight - 6} width="58" height="22" rx="5" fill="var(--range-label-back)" opacity=".92"
      stroke={selected ? "var(--range-active)" : "var(--range-green-light)"} strokeWidth=".6" />
    <text x={labelX + 29} y={-flagHeight + 9} textAnchor="middle" fill={selected ? "var(--range-active)" : "var(--range-label)"}
      fontSize="13" fontWeight="760">{distanceLabel}</text>
    <path d={outline} transform="scale(1.5 1.66)" fill="transparent" />
  </g>;
}

function Metric({ label, value }: { label: string; value: string }) {
  return <div><p className="text-subtle-foreground text-xs">{label}</p><p className="text-foreground font-mono text-base font-semibold tabular-nums">{value}</p></div>;
}
