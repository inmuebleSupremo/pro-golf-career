"use client";

import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";

import { BallStrikeDock, type BallStrikeInput } from "@/components/play/ball-strike-dock";
import { PhysicalPuttingGreen } from "@/components/play/physical-putting-green";
import { ReadGreenButton } from "@/components/play/play-mode";
import { PracticeFacilityHeader } from "@/components/play/practice-facility-nav";
import { validShotSelection, type ShotSelection, type Situation } from "@/components/play/play-shared";
import { Button } from "@/components/ui/button";
import { gqlRequest } from "@/lib/api/graphql-client";
import type { TrainingScene } from "@/lib/api/career-training";
import type { PlayerTrainingRangeShotMutation } from "@/lib/graphql/generated/graphql";
import { PlayerTrainingGreensideHoleDocument, PlayerTrainingRangeShotDocument } from "@/lib/graphql/operations";
import { aroundGreenEligible } from "@/lib/play/around-green";
import { golfAudio } from "@/lib/play/golf-audio";
import type { HoleGeom, Point, ResolvedShot } from "@/lib/play/hole-geometry";
import { aimWithinEnvelope } from "@/lib/play/intent";
import { visibleTrainingAttempt } from "@/lib/play/training-presentation";
import { useTrainingResultTransition } from "@/lib/play/use-training-result-transition";

type TrainingShot = PlayerTrainingRangeShotMutation["playerTrainingRangeShot"];

/** One Career Training challenge mounted in the existing competitive Around the Green stage and shot dock. */
export function GreensideTraining({ id, training }: { id: string; training: TrainingScene }) {
  const query = useQuery({ queryKey: ["playerTrainingGreensideHole", id, training.session.id, training.challenge.number],
    queryFn: () => gqlRequest(PlayerTrainingGreensideHoleDocument, { id }), retry: false,
    staleTime: Infinity, refetchOnWindowFocus: false });
  const hole = query.data?.playerTrainingGreensideHole as HoleGeom | null | undefined;
  const origin = hole?.ball?.position ?? training.challenge.start;
  const [aim, setAim] = useState<Point>(training.challenge.target);
  const [hasAimed, setHasAimed] = useState(false);
  const [requestedSelection, setRequestedSelection] = useState<ShotSelection>({
    club: training.challenge.suggestedClub ?? "GAP_WEDGE",
    technique: training.challenge.suggestedFamily ?? "CHIP", shape: "STRAIGHT",
  });
  const [reading, setReading] = useState(false);
  const [shot, setShot] = useState<TrainingShot | null>(null);
  const [reconciledAttempt, setReconciledAttempt] = useState<TrainingShot["attempt"] | null>(null);
  const [playback, setPlayback] = useState<ResolvedShot | null>(null);
  const [error, setError] = useState<string | null>(null);
  const inFlight = useRef(false);
  const mutation = useMutation({ mutationFn: (intent: BallStrikeInput) => gqlRequest(PlayerTrainingRangeShotDocument,
    { id, trainingId: training.session.id, challengeNumber: training.challenge.number, intent }) });
  const geometry = hole?.geometry;
  const xs = geometry?.playableBoundary.map((point) => point.x) ?? [origin.x];
  const ys = geometry?.playableBoundary.map((point) => point.y) ?? [origin.y];
  const aimEnvelope = { minX: Math.min(...xs) - 100, maxX: Math.max(...xs) + 100,
    minY: Math.min(...ys) - 100, maxY: Math.max(...ys) + 100 };
  const clubs = query.data?.playerTrainingGreensideClubs.filter((club) =>
    ["PITCHING_WEDGE", "GAP_WEDGE", "SAND_WEDGE"].includes(club.club)) ?? [];
  const situation: Situation = { holeNumber: hole?.holeNumber ?? training.challenge.number,
    par: hole?.par ?? 4, shotNumber: training.challenge.number, strokesThisHole: 0,
    distanceToPin: Math.hypot(training.challenge.target.x - origin.x, training.challenge.target.y - origin.y),
    lie: hole?.ball?.lie ?? "FIRST_CUT", pinLateral: hole?.pinLateral ?? 0, minReach: 0, maxReach: 320,
    shotRevision: String(training.challenge.number - 1), aimEnvelope,
    guidance: { safe: training.challenge.target, primary: training.challenge.target,
      aggressive: training.challenge.target, clubs, strategicOptions: [] } };
  const selection = validShotSelection(situation, requestedSelection, false);
  const recordedAttempt = shot?.attempt ?? reconciledAttempt;
  const visibleAttempt = visibleTrainingAttempt(recordedAttempt, !!playback);
  const settledHole = hole && visibleAttempt ? { ...hole, ball: { position: visibleAttempt.finalPosition,
    lie: shot?.rangeOutcome?.finalSurface ?? hole.ball?.lie ?? "FIRST_CUT" } } : hole;
  const stageReady = !!hole && aroundGreenEligible(hole, situation.lie, "premium");
  const locked = !stageReady || query.isPending || query.isError || mutation.isPending || !!playback
    || !!recordedAttempt || !hasAimed || !!training.briefingActive || training.challengePlayable === false;
  const ambience = hole?.courseFamily === "LINKS" ? "links" : hole?.courseFamily === "DESERT" ? "desert" : "parkland";

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    golfAudio.startScene(ambience);
    return () => { document.body.style.overflow = previousOverflow; golfAudio.stopScene(); };
  }, [ambience]);

  useEffect(() => {
    if (!playback || !window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const frame = requestAnimationFrame(() => setPlayback(null));
    return () => cancelAnimationFrame(frame);
  }, [playback]);

  function chooseAim(point: Point) {
    if (!stageReady || query.isPending || query.isError || mutation.isPending || !!playback || !!recordedAttempt
        || !!training.briefingActive || training.challengePlayable === false || !aimWithinEnvelope(point, aimEnvelope)) return;
    setAim(point);
    setHasAimed(true);
  }

  async function play(intent: BallStrikeInput) {
    if (locked || inFlight.current) return;
    inFlight.current = true;
    setError(null);
    try {
      const result = (await mutation.mutateAsync(intent)).playerTrainingRangeShot;
      setShot(result);
      if (result.rangeOutcome?.trace) setPlayback(result.rangeOutcome as ResolvedShot);
    } catch (cause) {
      try {
        const latest = await training.onReconcile();
        const accepted = latest?.id === training.session.id
          ? latest.attempts.find((attempt) => attempt.number === training.challenge.number) : null;
        if (accepted) setReconciledAttempt(accepted);
        else setError(cause instanceof Error ? cause.message : "Could not resolve this challenge. Try again.");
      } catch { setError("Connection interrupted. Refresh to recover the authoritative session status."); }
    } finally { inFlight.current = false; }
  }

  async function nextChallenge() {
    if (!recordedAttempt || playback) return;
    setError(null);
    try {
      const latest = await training.onReconcile();
      if (!latest || latest.id !== training.session.id || latest.attempts.length < recordedAttempt.number)
        throw new Error("The recorded attempt could not be confirmed. Try again.");
      if (latest.status === "COMPLETED") training.onReview();
      else if (latest.status === "ACTIVE" && latest.nextChallenge > recordedAttempt.number)
        training.onAdvance(latest.nextChallenge);
      else throw new Error("The next challenge is not available yet.");
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not load the next challenge."); }
  }

  useTrainingResultTransition(visibleAttempt?.number ?? null, !!playback, nextChallenge);

  return <div role="dialog" aria-modal="true" aria-label="Career Training greenside wedge challenge"
    className="practice-facility-page bg-background fixed inset-0 z-[var(--z-modal)] isolate flex h-dvh min-h-0 min-w-0 flex-col overflow-hidden">
    <PracticeFacilityHeader id={id} active="training" showAudio title="Wedges training"
      subtitle={`Challenge ${training.challenge.number} of 8 · ${training.challenge.targetLabel}`} />
    <main className="bg-surface relative min-h-0 min-w-0 flex-1 overflow-hidden">
      {stageReady && settledHole ? <PhysicalPuttingGreen hole={settledHole} ball={playback} aimDirection={{ x: 0, y: 1 }}
        onAimDirection={() => {}} showReading={reading} presentation="premium" aroundGreen
        aimPoint={aim} onAimPoint={chooseAim} showAimMarker={hasAimed}
        onTracePlaybackComplete={() => setPlayback(null)} />
        : <p className="text-muted-foreground flex h-full items-center justify-center text-sm">
          {query.isError || hole && !stageReady ? "Greenside training scene is unavailable" : "Preparing the greenside hole…"}
        </p>}
    </main>
    <BallStrikeDock situation={situation} origin={origin} aimPoint={aim} onAimPoint={chooseAim}
      selection={selection} onSelection={(update) => setRequestedSelection((current) =>
        validShotSelection(situation, { ...current, ...update }, false))}
      onSubmit={play} locked={locked} submitting={mutation.isPending} allowFringePutt={false}
      responsiveAuxiliary
      onNextBall={visibleAttempt && error ? nextChallenge : undefined} nextLabel="Retry loading result"
      actionLabel={visibleAttempt ? "Next briefing…" : "Play challenge"} externalError={error}
      statusText={visibleAttempt ? `Challenge ${visibleAttempt.number} recorded · Score ${Math.round(visibleAttempt.score * 100)}% · ${visibleAttempt.proximity.toFixed(1)} yd from cup`
        : playback ? "Shot in flight" : !hasAimed ? "Click or tap the green surroundings to select a landing point"
          : `Challenge ${training.challenge.number} · Aim selected · Ready to hit`}
      auxiliary={<div className="flex flex-nowrap items-center gap-2"><ReadGreenButton active={reading}
        onToggle={() => setReading((current) => !current)} disabled={!stageReady || !!playback} aroundGreen />
        <Button variant="secondary" size="sm" onClick={training.onAbandon} disabled={mutation.isPending || !!playback}>
          Abandon Session</Button></div>} />
  </div>;
}
