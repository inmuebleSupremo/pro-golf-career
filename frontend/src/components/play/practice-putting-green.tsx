"use client";

import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { PracticeFacilityHeader } from "@/components/play/practice-facility-nav";
import { PracticePuttingStage } from "@/components/play/practice-putting-stage";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { gqlRequest } from "@/lib/api/graphql-client";
import type { TrainingScene } from "@/lib/api/career-training";
import type { PlayerTrainingPuttMutation, PracticePhysicalPuttMutation, PracticePuttingGreenQuery } from "@/lib/graphql/generated/graphql";
import { CollectPracticeBallsDocument, PlacePracticeBallDocument, PracticePhysicalPuttDocument,
  PracticePuttingGreenDocument, PlayerTrainingPuttDocument } from "@/lib/graphql/operations";
import type { Point } from "@/lib/play/hole-geometry";
import { cupDirectedLine, currentPhysicalPuttControls, defaultPaceFeet, physicalPuttControlSource,
  validPaceFeet, type PhysicalPuttControls } from "@/lib/play/physical-putting";
import { golfAudio } from "@/lib/play/golf-audio";
import { shotSubmissionFeedback } from "@/lib/play/shot-feedback";
import { visibleTrainingAttempt } from "@/lib/play/training-presentation";

type GreenState = PracticePuttingGreenQuery["practicePuttingGreen"];
type PuttResult = PracticePhysicalPuttMutation["practicePhysicalPutt"];
type TrainingPutt = PlayerTrainingPuttMutation["playerTrainingPutt"];
type TrainingAttempt = TrainingPutt["attempt"];
type StateUpdate = Pick<GreenState, "shotRevision" | "puttsHit" | "startingPosition" | "readyPosition" | "restingBalls">;

export function PracticePuttingGreen({ id, training }: { id: string; training?: TrainingScene }) {
  const client = useQueryClient();
  const key = ["practicePuttingGreen", id];
  const green = useQuery({ queryKey: key, queryFn: () => gqlRequest(PracticePuttingGreenDocument, { id }), retry: false });
  const baseState = green.data?.practicePuttingGreen;
  const [selectedCup, setSelectedCup] = useState(3);
  const [placing, setPlacing] = useState(false);
  const [reading, setReading] = useState(false);
  const [focus, setFocus] = useState(true);
  const [controls, setControls] = useState<PhysicalPuttControls | null>(null);
  const [playback, setPlayback] = useState<Pick<PuttResult, "outcome"> | null>(null);
  const [last, setLast] = useState<PuttResult | null>(null);
  const [trainingShot, setTrainingShot] = useState<TrainingPutt | null>(null);
  const [reconciledAttempt, setReconciledAttempt] = useState<TrainingAttempt | null>(null);
  const [error, setError] = useState<string | null>(null);
  const inFlight = useRef(false);
  const recordedAttempt = trainingShot?.attempt ?? reconciledAttempt;
  const visibleAttempt = visibleTrainingAttempt(recordedAttempt, !!playback);
  const state = baseState && training ? { ...baseState,
    startingPosition: training.challenge.start,
    readyPosition: visibleAttempt ? visibleAttempt.finalPosition : training.challenge.start,
    restingBalls: [], shotRevision: String(training.challenge.number - 1),
    puttsHit: training.session.attempts.length } : baseState;

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    golfAudio.startScene("parkland");
    return () => {
      document.body.style.overflow = previousOverflow;
      golfAudio.stopScene();
    };
  }, []);

  function updateState(next: StateUpdate) {
    client.setQueryData<PracticePuttingGreenQuery>(key, (old) => old
      ? { ...old, practicePuttingGreen: { ...old.practicePuttingGreen, ...next } } : old);
  }

  const place = useMutation({ mutationFn: (position: Point) => gqlRequest(PlacePracticeBallDocument,
    { id, position, revision: state!.shotRevision }), onSuccess: (data) => updateState(data.placePracticeBall) });
  const collect = useMutation({ mutationFn: () => gqlRequest(CollectPracticeBallsDocument,
    { id, revision: state!.shotRevision }), onSuccess: (data) => updateState(data.collectPracticeBalls) });
  const putt = useMutation({ mutationFn: (intent: { direction: Point; intendedRollFeet: number; expectedShotRevision: string; targetCup: number }) =>
    gqlRequest(PracticePhysicalPuttDocument, { id, targetCup: intent.targetCup, intent: {
      direction: intent.direction, intendedRollFeet: intent.intendedRollFeet, expectedShotRevision: intent.expectedShotRevision,
    } }), onSuccess: (data) => {
    const result = data.practicePhysicalPutt;
    setLast(result);
    if (result.outcome.trajectory.length > 1) setPlayback(result);
    else updateState(result.state);
  } });
  const trainingPutt = useMutation({ mutationFn: (intent: { direction: Point; intendedRollFeet: number; expectedShotRevision: string }) =>
    gqlRequest(PlayerTrainingPuttDocument, { id, trainingId: training!.session.id,
      challengeNumber: training!.challenge.number, intent }) });

  const chosenCupNumber = training?.challenge.cupNumber ?? selectedCup;
  const chosenCup = state?.cups.find((cup) => cup.number === chosenCupNumber) ?? state?.cups[0];
  const ready = state?.readyPosition ?? { x: 0, y: 0 };
  const cupPoint = chosenCup?.position ?? ready;
  const controlSource = physicalPuttControlSource(state?.shotRevision ?? "0", ready, cupPoint);
  const activeControls = currentPhysicalPuttControls(controlSource, ready, cupPoint, controls);
  const defaultPace = defaultPaceFeet(ready, cupPoint);
  const paceMaximum = Math.max(20, Math.ceil(defaultPace * 1.5 / 5) * 5);
  const cupLine = cupDirectedLine(ready, cupPoint);
  const angle = Math.atan2(cupLine.x * activeControls.direction.y - cupLine.y * activeControls.direction.x,
    cupLine.x * activeControls.direction.x + cupLine.y * activeControls.direction.y) * 180 / Math.PI;
  const locked = green.isPending || green.isError || place.isPending || collect.isPending || putt.isPending
    || trainingPutt.isPending || !!playback || !!recordedAttempt || !!training?.briefingActive
    || training?.challengePlayable === false;
  const lastDistanceFeet = last ? Math.round(Math.hypot(
    last.outcome.finalPosition.x - last.outcome.trajectory[0].position.x,
    last.outcome.finalPosition.y - last.outcome.trajectory[0].position.y) * 3) : 0;
  const lastText = training && visibleAttempt
    ? `Challenge ${visibleAttempt.number} recorded · Score ${Math.round(visibleAttempt.score * 100)}% · ${Math.round(visibleAttempt.proximity * 3)} ft from cup`
    : training && playback ? `Challenge ${training.challenge.number} · Putt rolling`
    : last ? last.holedCup ? `Holed cup ${last.holedCup}` : `Last putt · ${lastDistanceFeet} ft rolled`
    : "A fresh ball is ready";

  async function placeAt(position: Point) {
    setError(null);
    try { await place.mutateAsync(position); setPlacing(false); }
    catch (cause) { setError(shotSubmissionFeedback(cause)); }
  }

  async function collectBalls() {
    setError(null);
    try { await collect.mutateAsync(); }
    catch (cause) { setError(shotSubmissionFeedback(cause)); }
  }

  async function rollPutt() {
    if (!state || !chosenCup) return;
    if (training?.briefingActive || training?.challengePlayable === false || inFlight.current || recordedAttempt || playback) return;
    golfAudio.unlock();
    setError(null);
    if (!training) {
      try { await putt.mutateAsync({ direction: activeControls.direction, intendedRollFeet: activeControls.paceFeet,
        expectedShotRevision: state.shotRevision, targetCup: chosenCup.number }); }
      catch (cause) { setError(shotSubmissionFeedback(cause)); }
      return;
    }
    inFlight.current = true;
    try {
      const response = (await trainingPutt.mutateAsync({ direction: activeControls.direction,
        intendedRollFeet: activeControls.paceFeet, expectedShotRevision: String(training.challenge.number - 1) })).playerTrainingPutt;
      setTrainingShot(response);
      if (response.puttOutcome?.trajectory && response.puttOutcome.trajectory.length > 1)
        setPlayback({ outcome: response.puttOutcome });
    } catch (cause) {
      try {
        const latest = await training.onReconcile();
        const accepted = latest?.id === training.session.id
          ? latest.attempts.find((attempt) => attempt.number === training.challenge.number) : null;
        if (accepted) setReconciledAttempt(accepted);
        else setError(shotSubmissionFeedback(cause));
      } catch { setError("Connection interrupted. Refresh to recover the authoritative session status."); }
    } finally { inFlight.current = false; }
  }

  async function nextChallenge() {
    if (!training || !recordedAttempt || playback) return;
    setError(null);
    try {
      const latest = await training.onReconcile();
      if (!latest || latest.id !== training.session.id || latest.attempts.length < recordedAttempt.number)
        throw new Error("The recorded putt could not be confirmed. Try again.");
      if (latest.status === "COMPLETED") training.onReview();
      else if (latest.status === "ACTIVE" && latest.nextChallenge > recordedAttempt.number) {
        training.onAdvance(latest.nextChallenge);
        setTrainingShot(null);
        setReconciledAttempt(null);
      } else throw new Error("The next challenge is not available yet.");
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not load the next challenge."); }
  }

  function rotateLine(degrees: number) {
    const radians = degrees * Math.PI / 180;
    const { x, y } = activeControls.direction;
    setControls({ ...activeControls, direction: { x: x * Math.cos(radians) - y * Math.sin(radians),
      y: x * Math.sin(radians) + y * Math.cos(radians) } });
  }

  function finishPlayback() {
    if (!playback) return;
    if (!training && "state" in playback) updateState(playback.state as StateUpdate);
    setPlayback(null);
  }

  return <div role="dialog" aria-modal="true" aria-label={training ? "Career Training putting green" : "Practice Facility putting green"}
    className="practice-facility-page practice-putting-page bg-background fixed inset-0 z-[var(--z-modal)] isolate flex h-dvh min-h-0 min-w-0 flex-col overflow-hidden">
    <PracticeFacilityHeader id={id} active={training ? "training" : "putting"} showAudio={!!training}
      title={training ? "Putting training" : "Putting Green"}
      subtitle={training ? `Challenge ${training.challenge.number} of 8 · Fixed ball and cup` : "Five cups · Place anywhere · Unlimited balls"} />
    {state && chosenCup ? <PracticePuttingStage green={state} selectedCup={chosenCupNumber} direction={activeControls.direction}
      placing={!training && placing} reading={reading} focus={focus} locked={locked} playback={playback}
      training={training ? { number: training.challenge.number, distance: training.challenge.distance,
        slope: training.challenge.slope } : undefined}
      onSelectCup={setSelectedCup} onAim={(direction) => setControls({ ...activeControls, direction })}
      onPlace={placeAt} onFocusToggle={() => setFocus((current) => !current)} onPlaybackComplete={finishPlayback} />
      : <main className="bg-surface-elevated text-muted-foreground flex min-h-0 flex-1 items-center justify-center text-sm">
        {green.isError ? "Could not open the putting green" : "Preparing the putting green…"}
      </main>}
    <footer className="practice-putting-dock border-border bg-surface relative shrink-0 border-t">
      <div className="practice-putting-targets">
        <span className="text-subtle-foreground text-[9px] font-bold tracking-widest uppercase">Target cup</span>
        <div className="flex gap-1" role="group" aria-label={training ? "Fixed training cup" : "Select target cup"}>
          {[1, 2, 3, 4, 5].map((number) => <button key={number} type="button" aria-label={`Select cup ${number}`}
            aria-pressed={chosenCupNumber === number} disabled={locked || !!training}
            onClick={() => setSelectedCup(number)}
            className={`border-border-strong h-7 w-7 rounded border text-xs font-bold ${chosenCupNumber === number
              ? "bg-gold text-background" : "bg-surface-3 text-foreground hover:border-primary"}`}>{number}</button>)}
        </div>
        <span className="text-muted-foreground text-[10px]">{training ? `${training.session.attempts.length} of 8 recorded` : `${state?.restingBalls.length ?? 0} resting balls`}</span>
      </div>
      <div className="practice-putting-middle">
        <div className="practice-putting-readout text-muted-foreground text-[11px]" aria-live="polite">
          <span className="text-foreground truncate">{lastText}</span>
        </div>
        <div className="practice-putting-controls">
          {!training ? <div className="practice-putting-place flex min-w-0 flex-col gap-1">
            <span className="text-subtle-foreground text-[9px] font-bold uppercase">Starting position</span>
            <Button variant="secondary" size="sm" onClick={() => setPlacing((current) => !current)}
              aria-pressed={placing} disabled={locked}>{placing ? "Tap green to place" : "Place ball"}</Button>
          </div> : null}
          <div className="practice-putting-read flex min-w-0 flex-col gap-1">
            <span className="text-subtle-foreground text-[9px] font-bold uppercase">Green reading</span>
            <Button variant="secondary" size="sm" onClick={() => setReading((current) => !current)}
              aria-pressed={reading} disabled={locked}>{reading ? "Hide contours" : "Read green"}</Button>
          </div>
          <div className="practice-putting-line flex min-w-0 flex-col gap-1">
            <span className="text-subtle-foreground text-[9px] font-bold uppercase">Starting line</span>
            <div className="flex items-center gap-1"><Button variant="secondary" size="sm" className="px-2" onClick={() => rotateLine(-2)}
              aria-label="Aim two degrees left" disabled={locked}>−</Button>
              <output className="text-foreground w-12 text-center text-xs font-semibold tabular-nums">{angle > 0 ? "+" : ""}{angle.toFixed(1)}°</output>
              <Button variant="secondary" size="sm" className="px-2" onClick={() => rotateLine(2)}
                aria-label="Aim two degrees right" disabled={locked}>+</Button></div>
          </div>
          <div className="practice-putting-pace flex min-w-0 flex-1 flex-col gap-1">
            <label htmlFor="practice-putt-pace" className="text-subtle-foreground text-[9px] font-bold uppercase">Putt pace · {activeControls.paceFeet.toFixed(1)} ft</label>
            <div className="flex min-w-0 items-center gap-2">
              <Input id="practice-putt-pace" type="range" min="0.1" max={paceMaximum} step="0.1" value={Math.min(activeControls.paceFeet, paceMaximum)}
                disabled={locked} onChange={(event) => setControls({ ...activeControls, paceFeet: validPaceFeet(Number(event.target.value)) })}
                className="min-w-0 flex-1" aria-label="Intended rolling distance in feet" />
              <Input type="number" min="0.1" step="0.1" value={activeControls.paceFeet} disabled={locked}
                onChange={(event) => setControls({ ...activeControls, paceFeet: validPaceFeet(Number(event.target.value)) })}
                className="w-16 shrink-0 px-1 text-center" aria-label="Exact intended rolling distance in feet" />
            </div>
          </div>
        </div>
      </div>
      <div className="practice-putting-actions flex gap-2">
        {training ? <Button variant="secondary" onClick={training.onAbandon} disabled={trainingPutt.isPending || !!playback}>Abandon Session</Button>
          : <Button variant="secondary" onClick={collectBalls} disabled={locked || !state?.restingBalls.length}>Collect balls</Button>}
        {training && visibleAttempt ? <Button size="lg" onClick={nextChallenge}>
          {training.challenge.number === 8 ? "Review session" : "Next challenge"}</Button>
          : <Button size="lg" onClick={rollPutt} disabled={locked || placing || !state}>{trainingPutt.isPending || putt.isPending ? "Playing…" : training ? "Play challenge" : "Roll putt"}</Button>}
      </div>
      {error && <p role="alert" className="text-destructive absolute right-2 bottom-full mb-2 rounded-md bg-background px-3 py-2 text-xs">{error}</p>}
    </footer>
  </div>;
}
