"use client";

import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { BallStrikeDock, Detail, type BallStrikeInput } from "@/components/play/ball-strike-dock";
import { PracticeRangeScenery } from "@/components/play/practice-range-scenery";
import { PracticeFacilityHeader } from "@/components/play/practice-facility-nav";
import { ShotMotionOverlay } from "@/components/play/shot-motion-overlay";
import { Button } from "@/components/ui/button";
import { validShotSelection, type ShotSelection, type Situation } from "@/components/play/play-shared";
import { gqlRequest } from "@/lib/api/graphql-client";
import type { TrainingScene } from "@/lib/api/career-training";
import type { PlayerTrainingRangeShotMutation } from "@/lib/graphql/generated/graphql";
import { PlayerTrainingRangeShotDocument, PracticeRangeDocument, PracticeShotDocument } from "@/lib/graphql/operations";
import type { Point } from "@/lib/play/hole-geometry";
import { golfAudio } from "@/lib/play/golf-audio";
import { projectRangePoint, projectRangeShotPoint, rangeBayMat, rangeBayOrigin, RANGE_BAY_CENTERS,
  rangeFieldPath, rangeScaleAt, RANGE_MOBILE_VIEWBOX, RANGE_SCENE, unprojectRangePoint } from "@/lib/play/practice-range-geometry";
import { trainingRestingBalls, visibleTrainingAttempt } from "@/lib/play/training-presentation";

const INITIAL_AIM: Point = { x: 95, y: 116 };
const markers = [50, 100, 150, 200, 250, 300];
const MAX_RESTING_BALLS = 30;

type TrainingShot = PlayerTrainingRangeShotMutation["playerTrainingRangeShot"];
type TrainingAttempt = TrainingShot["attempt"];
const TRAINING_CLUBS: Record<string, readonly string[]> = {
  DRIVING: ["DRIVER"],
  IRONS: ["FOUR_IRON", "FIVE_IRON", "SIX_IRON", "SEVEN_IRON", "EIGHT_IRON", "NINE_IRON"],
  WEDGES: ["PITCHING_WEDGE", "GAP_WEDGE", "SAND_WEDGE"],
};

export function DrivingRange({ id, training }: { id: string; training?: TrainingScene }) {
  type PracticeResult = Awaited<ReturnType<typeof hitRequest>>["practiceShot"];
  type PlaybackShot = { ballNumber: number; outcome: { trace: PracticeResult["outcome"]["trace"] } };
  const [mobileScene, setMobileScene] = useState(false);
  const [selectedBay, setSelectedBay] = useState(4);
  const queryClient = useQueryClient();
  const range = useQuery({ queryKey: ["practiceRange", id], queryFn: () => gqlRequest(PracticeRangeDocument, { id }), retry: false });
  const [aim, setAim] = useState<Point>(INITIAL_AIM);
  const [hasAimed, setHasAimed] = useState(!training);
  const [requestedSelection, setRequestedSelection] = useState<ShotSelection>({
    club: training?.challenge.suggestedClub ?? "SEVEN_IRON",
    technique: training?.challenge.suggestedFamily ?? "FULL", shape: "STRAIGHT",
  });
  const [result, setResult] = useState<PracticeResult | null>(null);
  const [playbackShot, setPlaybackShot] = useState<PlaybackShot | null>(null);
  const [restingBalls, setRestingBalls] = useState<{ number: number; position: Point }[]>([]);
  const [trainingShot, setTrainingShot] = useState<TrainingShot | null>(null);
  const [reconciledAttempt, setReconciledAttempt] = useState<TrainingAttempt | null>(null);
  const [trainingError, setTrainingError] = useState<string | null>(null);
  const inFlight = useRef(false);
  const mutation = useMutation({ mutationFn: hitRequest, onSuccess: (data) => {
    setResult(data.practiceShot);
    setPlaybackShot(data.practiceShot.outcome.trace ? { ballNumber: data.practiceShot.ballNumber,
      outcome: { trace: data.practiceShot.outcome.trace } } : null);
    queryClient.setQueryData(["practiceRange", id], (old: typeof range.data) => old ? {
      ...old, practiceRange: { ...old.practiceRange, ballsHit: data.practiceShot.ballNumber,
        shotRevision: String(data.practiceShot.ballNumber) },
    } : old);
  } });
  const trainingMutation = useMutation({ mutationFn: (intent: BallStrikeInput) => gqlRequest(
    PlayerTrainingRangeShotDocument, { id, trainingId: training!.session.id,
      challengeNumber: training!.challenge.number, intent }) });

  const isTraining = !!training;
  function completePlayback(shot: PlaybackShot) {
    const finalPoint = shot.outcome.trace?.finalPoint;
    if (!isTraining && finalPoint) setRestingBalls((balls) => balls.some((ball) => ball.number === shot.ballNumber) ? balls
      : [...balls, { number: shot.ballNumber, position: finalPoint }].slice(-MAX_RESTING_BALLS));
    setPlaybackShot((current) => current?.ballNumber === shot.ballNumber ? null : current);
  }

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
      if (reduced.matches) frame = requestAnimationFrame(() => {
        const finalPoint = playbackShot.outcome.trace?.finalPoint;
        if (!isTraining && finalPoint) setRestingBalls((balls) => balls.some((ball) => ball.number === playbackShot.ballNumber)
          ? balls : [...balls, { number: playbackShot.ballNumber, position: finalPoint }].slice(-MAX_RESTING_BALLS));
        setPlaybackShot((current) => current?.ballNumber === playbackShot.ballNumber ? null : current);
      });
    };
    reduced.addEventListener("change", finishReducedPlayback);
    finishReducedPlayback();
    return () => {
      reduced.removeEventListener("change", finishReducedPlayback);
      cancelAnimationFrame(frame);
    };
  }, [playbackShot, isTraining]);

  async function hitRequest(intent: BallStrikeInput) { return gqlRequest(PracticeShotDocument, { id, bayNumber: selectedBay, intent }); }

  const activeBay = training ? 4 : selectedBay;
  const bayOrigin = rangeBayOrigin(activeBay);
  const bayMat = rangeBayMat(activeBay);
  const targetPoint = projectRangePoint(aim);
  const trace = playbackShot?.outcome.trace;
  const contact = trace ? projectRangeShotPoint(trace.contact.position) : null;
  const finish = trace ? projectRangeShotPoint(trace.finalPoint) : null;
  const aimHalfWidth = range.data?.practiceRange.aimHalfWidth ?? 220;
  const fieldPath = rangeFieldPath(range.data?.practiceRange.fieldHalfWidth ?? 250);
  const targetGreenRadius = range.data?.practiceRange.targetGreenRadius ?? 11;
  const recordedAttempt = trainingShot?.attempt ?? reconciledAttempt;
  const visibleAttempt = visibleTrainingAttempt(recordedAttempt, !!playbackShot);
  const visualBalls = training ? trainingRestingBalls(training.session.attempts, recordedAttempt,
    training.challenge.number, !!playbackShot).map((ball) => ({ number: ball.number, position: ball.finalPosition }))
    : restingBalls;
  const situation: Situation = {
    holeNumber: 1, par: 0, shotNumber: training?.challenge.number ?? (range.data?.practiceRange.ballsHit ?? 0) + 1,
    strokesThisHole: 0, distanceToPin: Math.hypot(aim.x - bayOrigin.x, aim.y - bayOrigin.y), lie: "TEE_BOX", pinLateral: 0,
    minReach: 0, maxReach: 320, shotRevision: training ? String(training.challenge.number - 1)
      : range.data?.practiceRange.shotRevision ?? "0",
    aimEnvelope: { minX: -aimHalfWidth, maxX: aimHalfWidth, minY: 15, maxY: 320 },
    guidance: range.data ? {
      safe: INITIAL_AIM, primary: INITIAL_AIM, aggressive: INITIAL_AIM,
      clubs: training ? range.data.practiceRange.clubs.filter((club) =>
        TRAINING_CLUBS[training.session.focus]?.includes(club.club)) : range.data.practiceRange.clubs,
      strategicOptions: [],
    } : null,
  };
  const selection = validShotSelection(situation, requestedSelection);

  function selectAim(point: Point) {
    if (training?.briefingActive || playbackShot || mutation.isPending || trainingMutation.isPending || recordedAttempt) return;
    setAim(point);
    setHasAimed(true);
  }

  async function hit(intent: BallStrikeInput) {
    if (!training) { await mutation.mutateAsync(intent); return; }
    if (training.briefingActive || training.challengePlayable === false || !hasAimed
        || inFlight.current || recordedAttempt || playbackShot) return;
    inFlight.current = true;
    setTrainingError(null);
    try {
      const response = (await trainingMutation.mutateAsync(intent)).playerTrainingRangeShot;
      setTrainingShot(response);
      setPlaybackShot(response.rangeOutcome?.trace ? { ballNumber: response.attempt.number,
        outcome: { trace: response.rangeOutcome.trace } } : null);
    } catch (error) {
      try {
        const latest = await training.onReconcile();
        const accepted = latest?.id === training.session.id
          ? latest.attempts.find((attempt) => attempt.number === training.challenge.number) : null;
        if (accepted) setReconciledAttempt(accepted);
        else setTrainingError(error instanceof Error ? error.message : "Could not resolve this challenge. Try again.");
      } catch { setTrainingError("Connection interrupted. Refresh to recover the authoritative session status."); }
    } finally { inFlight.current = false; }
  }

  async function nextChallenge() {
    if (!training || !recordedAttempt || playbackShot) return;
    setTrainingError(null);
    try {
      const latest = await training.onReconcile();
      if (!latest || latest.id !== training.session.id || latest.attempts.length < recordedAttempt.number)
        throw new Error("The recorded attempt could not be confirmed. Try again.");
      if (latest.status === "COMPLETED") training.onReview();
      else if (latest.status === "ACTIVE" && latest.nextChallenge > recordedAttempt.number) {
        training.onAdvance(latest.nextChallenge);
        setTrainingShot(null);
        setReconciledAttempt(null);
      } else throw new Error("The next challenge is not available yet.");
    } catch (error) { setTrainingError(error instanceof Error ? error.message : "Could not load the next challenge."); }
  }

  return <div role="dialog" aria-modal="true" aria-label={training ? "Career Training driving range" : "Practice Facility driving range"}
    className="practice-facility-page practice-range-page bg-background fixed inset-0 z-[var(--z-modal)] isolate flex h-dvh min-h-0 min-w-0 flex-col overflow-hidden">
    <PracticeFacilityHeader id={id} active={training ? "training" : "range"} showAudio={!!training}
      title={training ? `${training.session.focus[0]}${training.session.focus.slice(1).toLowerCase()} training` : "Driving Range"}
      subtitle={training ? `Challenge ${training.challenge.number} of 8 · Fixed bay 4` : "Unlimited balls · Seven targets"} />

    <div className="border-border-strong relative min-h-0 min-w-0 flex-1 overflow-hidden bg-surface">
      <svg viewBox={mobileScene ? `${RANGE_MOBILE_VIEWBOX.x} 0 ${RANGE_MOBILE_VIEWBOX.width} ${RANGE_SCENE.height}` : `0 0 ${RANGE_SCENE.width} ${RANGE_SCENE.height}`}
        preserveAspectRatio="none" role="img"
        aria-label="Driving range with seven target flags, distance markers, and seven selectable hitting bays"
        className="practice-range block h-full w-full">
        <PracticeRangeScenery fieldPath={fieldPath} selectedBay={activeBay} />
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
        <path d={fieldPath} fill="transparent" role="button"
          tabIndex={training?.briefingActive || playbackShot || mutation.isPending || trainingMutation.isPending || recordedAttempt ? -1 : 0}
          aria-label="Aim on the range. Use arrow keys to adjust direction."
          className="cursor-crosshair focus-visible:outline-2 focus-visible:outline-ring"
          onClick={(event) => {
            if (training?.briefingActive || playbackShot || mutation.isPending || trainingMutation.isPending || recordedAttempt) return;
            const matrix = event.currentTarget.ownerSVGElement?.getScreenCTM();
            if (!matrix) return;
            const point = new DOMPoint(event.clientX, event.clientY).matrixTransform(matrix.inverse());
            selectAim(unprojectRangePoint(point, aimHalfWidth));
          }} onKeyDown={(event) => {
            const shift = event.key === "ArrowLeft" ? { x: -5, y: 0 }
              : event.key === "ArrowRight" ? { x: 5, y: 0 }
                : event.key === "ArrowUp" ? { x: 0, y: 5 }
                  : event.key === "ArrowDown" ? { x: 0, y: -5 } : null;
            if (!shift) return;
            event.preventDefault();
            selectAim({ x: Math.max(-aimHalfWidth, Math.min(aimHalfWidth, aim.x + shift.x)),
              y: Math.max(15, Math.min(320, aim.y + shift.y)) });
          }} />
        {RANGE_BAY_CENTERS.map((center, index) => <g key={index} role="button" tabIndex={training || playbackShot || mutation.isPending ? -1 : 0}
          aria-label={`Select hitting bay ${index + 1}`} aria-pressed={activeBay === index + 1}
          aria-disabled={!!training}
          onClick={() => { if (!training && !playbackShot && !mutation.isPending) setSelectedBay(index + 1); }}
          onKeyDown={(event) => { if ((event.key === "Enter" || event.key === " ") && !training && !playbackShot && !mutation.isPending) {
            event.preventDefault(); setSelectedBay(index + 1);
          } }} className={training ? "cursor-default" : "cursor-pointer focus-visible:outline-2 focus-visible:outline-ring"}>
          <rect x={center - 80} y="625" width="160" height="75" fill="transparent" />
        </g>)}
        {(range.data?.practiceRange.targets ?? []).map((target) => <TargetGreen key={target.label}
          distanceLabel={`${Math.round(Math.hypot(target.position.x - bayOrigin.x, target.position.y - bayOrigin.y))} yd`}
          point={projectRangePoint(target.position)} radiusX={targetGreenRadius * rangeScaleAt(target.position.y)}
          radiusY={targetGreenRadius * (RANGE_SCENE.bayY - RANGE_SCENE.farY) / RANGE_SCENE.farDistance}
          />)}
        {training && <g pointerEvents="none" aria-label={`${training.challenge.targetLabel} training target`}>
          <circle cx={projectRangePoint(training.challenge.target).x}
            cy={projectRangePoint(training.challenge.target).y} r={training.challenge.landingZone ? 20 : 14}
            fill={training.challenge.landingZone ? "var(--range-label-back)" : "none"}
            stroke="var(--range-active)" strokeWidth="3" />
          <circle cx={projectRangePoint(training.challenge.target).x} cy={projectRangePoint(training.challenge.target).y}
            r="3" fill="var(--range-active)" />
          {training.challenge.landingZone ? <g transform={`translate(${projectRangePoint(training.challenge.target).x} ${projectRangePoint(training.challenge.target).y - 28})`}>
            <rect x="-45" y="-20" width="90" height="22" rx="5" fill="var(--range-label-back)" stroke="var(--range-active)" />
            <text y="-5" textAnchor="middle" fill="var(--range-label)" fontSize="12" fontWeight="700">{training.challenge.targetLabel}</text>
          </g> : null}
        </g>}
        <g pointerEvents="none">{visualBalls.map((ball) => {
          const point = projectRangeShotPoint(ball.position);
          return <circle key={ball.number} cx={point.x} cy={point.y} r="3.5" fill="var(--range-label)" stroke="var(--range-edge)" strokeWidth="1" opacity=".9" />;
        })}</g>
        {!trace && <>{hasAimed ? <><path d={`M${bayMat.x} ${bayMat.y} L${targetPoint.x} ${targetPoint.y}`} stroke="var(--range-aim)" strokeDasharray="8 10" strokeWidth="2" opacity=".65" pointerEvents="none" />
          <circle cx={targetPoint.x} cy={targetPoint.y} r="9" fill="none" stroke="var(--range-aim)" strokeWidth="2" pointerEvents="none" />
          <text x={targetPoint.x + 13} y={targetPoint.y - 11} fill="var(--range-label)" stroke="var(--range-edge)"
            strokeWidth="3" paintOrder="stroke" fontSize="12" fontWeight="700" pointerEvents="none">
            Aim {Math.round(Math.hypot(aim.x - bayOrigin.x, aim.y - bayOrigin.y))} yd
          </text></> : null}
          <circle cx={bayMat.x} cy={bayMat.y} r="5" fill="var(--range-label)" stroke="var(--range-edge)" strokeWidth="1.5" pointerEvents="none" /></>}
        {trace && contact && finish && playbackShot && <ShotMotionOverlay trace={trace} project={projectRangeShotPoint} contact={contact} finalPoint={finish}
          contactEffect="var(--range-active)" ballStroke="var(--range-edge)" statusY={45} shadowFill="var(--range-edge)" tracerShadow
          onPlaybackComplete={() => completePlayback(playbackShot)} />}
      </svg>
      {training ? <div className="border-border-strong bg-surface/90 text-foreground absolute top-2 left-1/2 max-w-[80vw] -translate-x-1/2 rounded-md border px-3 py-2 text-center text-xs sm:top-3">
        <strong>Challenge {training.challenge.number} of 8 · {training.session.focus.toLowerCase()}</strong>
        <span className="ml-2">Finish at {training.challenge.targetLabel} · {Math.round(training.challenge.distance)} yd · Allow for rollout</span>
      </div> : <p className="border-border-strong bg-surface/85 text-foreground pointer-events-none absolute top-2 left-2 rounded-md border px-2 py-1 text-[10px] sm:top-3 sm:left-3">Click or tap the range to aim. Select a bay mat to change your hitting position.</p>}
    </div>

    <BallStrikeDock situation={situation} origin={bayOrigin} aimPoint={aim}
        className="practice-range-dock border-border-strong"
        onAimPoint={() => {}}
        directAim
        aimSelected={hasAimed}
        expectedReaches={range.data?.practiceRange.expectedReaches}
        selection={selection} onSelection={(update) => setRequestedSelection((current) => validShotSelection(situation, { ...current, ...update }))}
        onSubmit={hit} locked={range.isPending || range.isError || mutation.isPending || trainingMutation.isPending || !!playbackShot || !!recordedAttempt || !!training?.briefingActive || training?.challengePlayable === false || !!training && !hasAimed}
        submitting={training ? trainingMutation.isPending : mutation.isPending}
        onNextBall={training && visibleAttempt ? nextChallenge : undefined}
        nextLabel={training?.challenge.number === 8 ? "Review session" : "Next challenge"}
        externalError={trainingError}
        referenceLabel="Aim" actionLabel={training ? "Play challenge" : "Hit ball"}
        statusText={range.isError ? "Could not open the range" : training && visibleAttempt
          ? `Challenge ${visibleAttempt.number} recorded · Score ${Math.round(visibleAttempt.score * 100)}% · ${visibleAttempt.proximity.toFixed(1)} yd from target`
          : playbackShot
          ? `Ball ${playbackShot.ballNumber} · Shot in flight`
          : mutation.isPending || trainingMutation.isPending ? `Ball ${situation.shotNumber} · Resolving shot`
            : training && !hasAimed ? "Click or tap the range to select an aim point before this challenge"
              : training ? `Challenge ${training.challenge.number} · Bay 4 · ${Math.round(situation.distanceToPin)} yd intended carry · Ready to hit`
              : `Ball ${situation.shotNumber} · Bay ${selectedBay} · ${Math.round(situation.distanceToPin)} yd aim · Ready to hit`}
        extraDetails={<Detail label="Conditions" value="Calm range" />}
        auxiliary={training ? <Button variant="secondary" size="sm" onClick={training.onAbandon}
          disabled={trainingMutation.isPending || !!playbackShot}>Abandon Session</Button> : null}
        review={<div className="practice-range-review border-border grid grid-cols-3 items-center gap-3 border-t text-center" aria-live="polite">
          {training && visibleAttempt ? <>
            <Metric label="Score" value={`${Math.round(visibleAttempt.score * 100)}%`} />
            <Metric label="Total" value={`${Math.round(visibleAttempt.totalDistance)} yd`} />
            <Metric label="Proximity" value={`${visibleAttempt.proximity.toFixed(1)} yd`} />
          </> : result && !training && !playbackShot ? <>
            <Metric label="Carry" value={`${Math.round(result.outcome.carry)} yd`} />
            <Metric label="Total" value={`${Math.round(result.totalDistance)} yd`} />
            <Metric label="Proximity" value={`${Math.round(result.proximity)} yd`} />
          </> : null}
        </div>} />
  </div>;
}

function TargetGreen({ distanceLabel, point, radiusX, radiusY }: {
  distanceLabel: string; point: Point; radiusX: number; radiusY: number;
}) {
  const outline = `M${-radiusX * 1.18} 1 Q${-radiusX * 1.12} ${-radiusY * .91} ${-radiusX * .46} ${-radiusY * 1.23} Q${radiusX * .36} ${-radiusY * 1.32} ${radiusX * 1.04} ${-radiusY * .47} Q${radiusX * 1.37} ${radiusY * .31} ${radiusX * .55} ${radiusY * .99} Q${-radiusX * .38} ${radiusY * 1.31} ${-radiusX * 1.18} 1Z`;
  const flagHeight = 29;
  const labelX = radiusX * .75 + 8;
  return <g transform={`translate(${point.x} ${point.y})`} pointerEvents="none" aria-label={`${distanceLabel} flag`}>
    <path d={outline} transform="translate(4 5) scale(1.15 1.18)" fill="var(--range-green-collar)" opacity=".24" />
    <path d={outline} transform="scale(1.23 1.23)" fill="var(--range-green-edge)" opacity=".92" />
    <path d={outline} fill="url(#range-green-fill)" stroke="var(--range-green-light)" strokeWidth="1.1" />
    <path d={outline} transform="scale(.8 .74)" fill="none" stroke="var(--range-line)" strokeWidth="1.2" opacity=".36" />
    <path d={`M${-radiusX * .78} ${-radiusY * .18} Q0 ${-radiusY * .95} ${radiusX * .72} ${-radiusY * .32}`}
      stroke="var(--range-line)" strokeWidth="1" fill="none" opacity=".39" />
    <path d={`M0 0V${-flagHeight}`} stroke="var(--range-flagpole)" strokeWidth="2" />
    <path d={`M0 ${-flagHeight}l17 5-17 5Z`} fill="var(--range-flag)" />
    <circle r="2" fill="var(--range-edge)" />
    <rect x={labelX} y={-flagHeight - 6} width="58" height="22" rx="5" fill="var(--range-label-back)" opacity=".92"
      stroke="var(--range-green-light)" strokeWidth=".6" />
    <text x={labelX + 29} y={-flagHeight + 9} textAnchor="middle" fill="var(--range-label)"
      fontSize="13" fontWeight="760">{distanceLabel}</text>
    <path d={outline} transform="scale(1.5 1.66)" fill="transparent" />
  </g>;
}

function Metric({ label, value }: { label: string; value: string }) {
  return <div><p className="text-subtle-foreground text-xs">{label}</p><p className="text-foreground font-mono text-base font-semibold tabular-nums">{value}</p></div>;
}
