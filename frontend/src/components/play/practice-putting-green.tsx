"use client";

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { PracticeFacilityHeader } from "@/components/play/practice-facility-nav";
import { PracticePuttingStage } from "@/components/play/practice-putting-stage";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { gqlRequest } from "@/lib/api/graphql-client";
import type { PracticePhysicalPuttMutation, PracticePuttingGreenQuery } from "@/lib/graphql/generated/graphql";
import { CollectPracticeBallsDocument, PlacePracticeBallDocument, PracticePhysicalPuttDocument,
  PracticePuttingGreenDocument } from "@/lib/graphql/operations";
import type { Point } from "@/lib/play/hole-geometry";
import { cupDirectedLine, currentPhysicalPuttControls, defaultPaceFeet, physicalPuttControlSource,
  validPaceFeet, type PhysicalPuttControls } from "@/lib/play/physical-putting";
import { golfAudio } from "@/lib/play/golf-audio";
import { shotSubmissionFeedback } from "@/lib/play/shot-feedback";

type GreenState = PracticePuttingGreenQuery["practicePuttingGreen"];
type PuttResult = PracticePhysicalPuttMutation["practicePhysicalPutt"];
type StateUpdate = Pick<GreenState, "shotRevision" | "puttsHit" | "startingPosition" | "readyPosition" | "restingBalls">;

export function PracticePuttingGreen({ id }: { id: string }) {
  const client = useQueryClient();
  const key = ["practicePuttingGreen", id];
  const green = useQuery({ queryKey: key, queryFn: () => gqlRequest(PracticePuttingGreenDocument, { id }), retry: false });
  const state = green.data?.practicePuttingGreen;
  const [selectedCup, setSelectedCup] = useState(3);
  const [placing, setPlacing] = useState(false);
  const [reading, setReading] = useState(false);
  const [focus, setFocus] = useState(true);
  const [controls, setControls] = useState<PhysicalPuttControls | null>(null);
  const [playback, setPlayback] = useState<PuttResult | null>(null);
  const [last, setLast] = useState<PuttResult | null>(null);
  const [error, setError] = useState<string | null>(null);

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

  const chosenCup = state?.cups.find((cup) => cup.number === selectedCup) ?? state?.cups[0];
  const ready = state?.readyPosition ?? { x: 0, y: 0 };
  const cupPoint = chosenCup?.position ?? ready;
  const controlSource = physicalPuttControlSource(state?.shotRevision ?? "0", ready, cupPoint);
  const activeControls = currentPhysicalPuttControls(controlSource, ready, cupPoint, controls);
  const defaultPace = defaultPaceFeet(ready, cupPoint);
  const paceMaximum = Math.max(20, Math.ceil(defaultPace * 1.5 / 5) * 5);
  const cupLine = cupDirectedLine(ready, cupPoint);
  const angle = Math.atan2(cupLine.x * activeControls.direction.y - cupLine.y * activeControls.direction.x,
    cupLine.x * activeControls.direction.x + cupLine.y * activeControls.direction.y) * 180 / Math.PI;
  const locked = green.isPending || green.isError || place.isPending || collect.isPending || putt.isPending || !!playback;
  const lastDistanceFeet = last ? Math.round(Math.hypot(
    last.outcome.finalPosition.x - last.outcome.trajectory[0].position.x,
    last.outcome.finalPosition.y - last.outcome.trajectory[0].position.y) * 3) : 0;
  const lastText = last ? last.holedCup ? `Holed cup ${last.holedCup}` : `Last putt · ${lastDistanceFeet} ft rolled`
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
    golfAudio.unlock();
    setError(null);
    try { await putt.mutateAsync({ direction: activeControls.direction, intendedRollFeet: activeControls.paceFeet,
      expectedShotRevision: state.shotRevision, targetCup: chosenCup.number }); }
    catch (cause) { setError(shotSubmissionFeedback(cause)); }
  }

  function rotateLine(degrees: number) {
    const radians = degrees * Math.PI / 180;
    const { x, y } = activeControls.direction;
    setControls({ ...activeControls, direction: { x: x * Math.cos(radians) - y * Math.sin(radians),
      y: x * Math.sin(radians) + y * Math.cos(radians) } });
  }

  function finishPlayback() {
    if (!playback) return;
    updateState(playback.state);
    setPlayback(null);
  }

  return <div role="dialog" aria-modal="true" aria-label="Practice Facility putting green"
    className="practice-facility-page practice-putting-page bg-background fixed inset-0 z-[var(--z-modal)] isolate flex h-dvh min-h-0 min-w-0 flex-col overflow-hidden">
    <PracticeFacilityHeader id={id} active="putting" title="Putting Green" subtitle="Five cups · Place anywhere · Unlimited balls" />
    {state && chosenCup ? <PracticePuttingStage green={state} selectedCup={selectedCup} direction={activeControls.direction}
      placing={placing} reading={reading} focus={focus} locked={locked} playback={playback}
      onSelectCup={setSelectedCup} onAim={(direction) => setControls({ ...activeControls, direction })}
      onPlace={placeAt} onFocusToggle={() => setFocus((current) => !current)} onPlaybackComplete={finishPlayback} />
      : <main className="bg-surface-elevated text-muted-foreground flex min-h-0 flex-1 items-center justify-center text-sm">
        {green.isError ? "Could not open the putting green" : "Preparing the putting green…"}
      </main>}
    <footer className="practice-putting-dock border-border bg-surface relative shrink-0 border-t">
      <div className="practice-putting-targets">
        <span className="text-subtle-foreground text-[9px] font-bold tracking-widest uppercase">Target cup</span>
        <div className="flex gap-1" role="group" aria-label="Select target cup">
          {[1, 2, 3, 4, 5].map((number) => <button key={number} type="button" aria-label={`Select cup ${number}`}
            aria-pressed={selectedCup === number} disabled={locked}
            onClick={() => setSelectedCup(number)}
            className={`border-border-strong h-7 w-7 rounded border text-xs font-bold ${selectedCup === number
              ? "bg-gold text-background" : "bg-surface-3 text-foreground hover:border-primary"}`}>{number}</button>)}
        </div>
        <span className="text-muted-foreground text-[10px]">{state?.restingBalls.length ?? 0} resting balls</span>
      </div>
      <div className="practice-putting-middle">
        <div className="practice-putting-readout text-muted-foreground text-[11px]" aria-live="polite">
          <span className="text-foreground truncate">{lastText}</span>
        </div>
        <div className="practice-putting-controls">
          <div className="practice-putting-place flex min-w-0 flex-col gap-1">
            <span className="text-subtle-foreground text-[9px] font-bold uppercase">Starting position</span>
            <Button variant="secondary" size="sm" onClick={() => setPlacing((current) => !current)}
              aria-pressed={placing} disabled={locked}>{placing ? "Tap green to place" : "Place ball"}</Button>
          </div>
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
        <Button variant="secondary" onClick={collectBalls} disabled={locked || !state?.restingBalls.length}>Collect balls</Button>
        <Button size="lg" onClick={rollPutt} disabled={locked || placing || !state}>{putt.isPending ? "Playing…" : "Roll putt"}</Button>
      </div>
      {error && <p role="alert" className="text-destructive absolute right-2 bottom-full mb-2 rounded-md bg-background px-3 py-2 text-xs">{error}</p>}
    </footer>
  </div>;
}
