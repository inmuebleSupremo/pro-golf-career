"use client";

import { useState, type ReactNode } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { OutcomeNote, type Outcome, type ShotSelection, type Situation } from "@/components/play/play-shared";
import { buildBallStrikeIntent } from "@/lib/play/intent";
import { expectedRangeReach, formatShotDistance, shotDistanceReadout, type ExpectedRangeReach } from "@/lib/play/distance";
import type { Point } from "@/lib/play/hole-geometry";
import { golfAudio } from "@/lib/play/golf-audio";
import { humanize } from "@/lib/play/options";
import { shotSubmissionFeedback } from "@/lib/play/shot-feedback";
import { cn } from "@/lib/utils";

export type BallStrikeInput = ReturnType<typeof buildBallStrikeIntent>;

/** The non-putting command dock shared by a live hole and the driving range. */
export function BallStrikeDock({ situation, origin, aimPoint, onAimPoint, selection, onSelection,
  onSubmit, locked, submitting, lastOutcome, externalError, referenceLabel = "Pin", statusText, extraDetails,
  auxiliary, review, onNextBall, nextLabel = "Next ball", directAim = false, aimSelected = true, expectedReaches = [], floating = false, actionLabel = "Play shot", className, allowFringePutt = true }: {
  situation: Situation;
  origin: Point;
  aimPoint: Point;
  onAimPoint: (point: Point) => void;
  selection: ShotSelection;
  onSelection: (update: Partial<ShotSelection>) => void;
  onSubmit: (intent: BallStrikeInput) => Promise<void>;
  locked: boolean;
  submitting: boolean;
  lastOutcome?: Outcome | null;
  externalError?: string | null;
  referenceLabel?: string;
  statusText?: string;
  extraDetails?: ReactNode;
  auxiliary?: ReactNode;
  review?: ReactNode;
  onNextBall?: () => void;
  nextLabel?: string;
  directAim?: boolean;
  aimSelected?: boolean;
  expectedReaches?: readonly ExpectedRangeReach[];
  floating?: boolean;
  actionLabel?: string;
  className?: string;
  allowFringePutt?: boolean;
}) {
  const [formError, setFormError] = useState<string | null>(null);
  const { club, technique, shape } = selection;
  const clubs = situation.guidance?.clubs ?? [];
  const selectedClub = clubs.find((candidate) => candidate.club === club);
  const playableClubs = clubs.filter((candidate) => candidate.families.some((family) =>
    family.available && family.shapes.some((candidateShape) => candidateShape.available)))
    .sort((a, b) => b.normalReach - a.normalReach);
  const techniques = [
    ...(allowFringePutt && situation.lie === "FRINGE" ? [{ family: "PUTT", available: true, reason: null }] : []),
    ...(selectedClub?.families.filter((candidate) => candidate.available
      && candidate.shapes.some((candidateShape) => candidateShape.available)) ?? []),
  ];
  const selectedTechnique = selectedClub?.families.find((candidate) => candidate.family === technique);
  const shapes = selectedTechnique?.shapes.filter((candidate) => candidate.available)
    ?? [{ shape: "STRAIGHT", available: true, reason: null }];
  const distances = shotDistanceReadout(situation, origin, aimPoint, selection);
  const selectedReach = directAim ? expectedRangeReach(expectedReaches, club, technique) : null;
  const landingTarget = technique === "PITCH" || technique === "CHIP";
  const displayStatus = statusText ?? (lastOutcome
    ? `Last shot: ${lastOutcome.holed ? "Holed" : formatShotDistance(lastOutcome.carry)} · ${humanize(lastOutcome.finalSurface)}`
    : "Tap the course to aim");

  async function play() {
    golfAudio.unlock();
    setFormError(null);
    if (onNextBall) { onNextBall(); return; }
    try { await onSubmit(buildBallStrikeIntent(club, aimPoint, technique, shape, situation.shotRevision)); }
    catch (error) {
      setFormError(error instanceof Error && error.message === "That shot plan is stale. Choose the target again."
        ? error.message : shotSubmissionFeedback(error));
    }
  }

  return <footer className={cn("border-border relative shrink-0 px-4 py-2.5 md:px-6", floating
    ? "bg-background/80 mx-3 mb-3 rounded-xl border shadow-lg backdrop-blur-md md:mx-6"
    : "bg-surface border-t", className)}>
    <div className="mx-auto grid max-w-6xl gap-2">
      {formError || externalError ? <p role="alert" className="text-destructive text-sm">{formError || externalError}</p> : null}
      <div className="flex min-w-0 items-center gap-2 lg:grid lg:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]">
        {directAim ? <p className="text-foreground min-w-0 flex-1 text-center text-sm font-semibold lg:col-start-2 lg:justify-self-center" aria-live="polite">
          {aimSelected ? `Selected aim ${Math.round(Math.hypot(aimPoint.x - origin.x, aimPoint.y - origin.y))} yd · Intended carry, club limit applies`
            : "Click or tap the range to choose carry and direction"}
        </p> : <div className="grid min-w-0 flex-1 grid-cols-3 gap-1 sm:gap-3 md:mx-auto md:max-w-xl md:gap-6 lg:col-start-2 lg:mx-0 lg:flex lg:w-max lg:max-w-none lg:justify-self-center lg:gap-5" aria-live="polite">
          <DistanceMeasure mobileLabel={referenceLabel} label={`To ${referenceLabel.toLowerCase()}`} value={distances.pin} />
          <DistanceMeasure mobileLabel="Target" label={landingTarget ? "Landing target" : "Target"} value={distances.target} />
          <DistanceMeasure mobileLabel="Reach" label="Normal reach" value={distances.reach} />
        </div>}
        <details name="shot-dock-panel" className="group relative shrink-0 lg:col-start-3 lg:justify-self-end">
          <summary className="text-muted-foreground hover:text-foreground cursor-pointer select-none text-xs font-medium">Shot details</summary>
          <div className="border-border bg-surface absolute right-0 bottom-full z-[var(--z-popover)] mb-3 max-h-[30dvh] w-80 max-w-[calc(100vw-2rem)] space-y-3 overflow-y-auto rounded-lg border p-3 shadow-lg sm:w-[28rem]">
            <div className="grid grid-cols-2 gap-2 text-xs">
              <Detail label="Lie" value={humanize(situation.lie)} />
              {extraDetails}
            </div>
            {!directAim ? <div className="flex flex-wrap items-end gap-2">
              <DockField label={landingTarget ? "Landing X" : "Target X"}>
                <Input type="number" value={aimPoint.x} min={situation.aimEnvelope?.minX} max={situation.aimEnvelope?.maxX}
                  disabled={locked || !!onNextBall} onChange={(event) => onAimPoint({ ...aimPoint, x: Number(event.target.value) })} className="w-24" />
              </DockField>
              <DockField label={landingTarget ? "Landing Y" : "Target Y"}>
                <Input type="number" value={aimPoint.y} min={situation.aimEnvelope?.minY} max={situation.aimEnvelope?.maxY}
                  disabled={locked || !!onNextBall} onChange={(event) => onAimPoint({ ...aimPoint, y: Number(event.target.value) })} className="w-24" />
              </DockField>
            </div> : null}
            {lastOutcome ? <div className="border-border border-t pt-3"><p className="text-subtle-foreground mb-2 text-xs">Previous shot</p><OutcomeNote outcome={lastOutcome} /></div> : null}
          </div>
        </details>
      </div>
      <p className="text-subtle-foreground min-h-4 min-w-0 truncate text-xs" title={displayStatus}>{displayStatus}</p>
      {directAim ? <p className="text-muted-foreground text-xs" aria-live="polite">{selectedClub?.label} · {humanize(technique)} · Approx. maximum carry: <strong className="text-foreground font-mono font-semibold tabular-nums">{selectedReach == null ? "—" : formatShotDistance(selectedReach)}</strong></p> : null}
      {review}
      <div className="grid min-w-0 grid-cols-[minmax(0,1fr)_auto] items-end gap-2 md:flex lg:grid lg:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]">
        <div className="contents lg:col-start-2 lg:flex lg:items-end lg:gap-2">
          <DockField label="Club" className="md:w-52 md:shrink-0"><Select value={club} disabled={locked || !!onNextBall} onChange={(event) => onSelection({ club: event.target.value })}>
            {playableClubs.map((candidate) => {
              const family = candidate.families.some((entry) => entry.family === technique && entry.available)
                ? technique : candidate.families.find((entry) => entry.available)?.family;
              const reach = family ? expectedRangeReach(expectedReaches, candidate.club, family) : null;
              return <option key={candidate.club} value={candidate.club}>{candidate.label}{directAim && reach != null ? ` · max ~${formatShotDistance(reach)}` : ""}</option>;
            })}
          </Select></DockField>
          <div className="hidden md:flex md:shrink-0 md:gap-2">
            <div className="w-32"><ShotTechniqueControl technique={technique} techniques={techniques} onTechnique={(value) => onSelection({ technique: value })} disabled={locked || !!onNextBall} /></div>
            <div className="w-32"><ShotShapeControl shape={shape} shapes={shapes} onShape={(value) => onSelection({ shape: value })} disabled={locked || !!onNextBall} /></div>
          </div>
        </div>
        <details name="shot-dock-panel" className="relative col-start-1 row-start-2 md:hidden">
          <summary className="border-border bg-background text-foreground flex h-[var(--control-lg)] cursor-pointer items-center rounded-md border px-3 text-sm"><span className="truncate">Shot setup · {humanize(technique)} · {humanize(shape)}</span></summary>
          <div className="border-border bg-surface absolute bottom-full left-0 z-[var(--z-popover)] mb-3 grid max-h-[30dvh] w-[calc(100vw-2rem)] grid-cols-2 gap-2 overflow-y-auto rounded-lg border p-3 shadow-lg">
            <ShotTechniqueControl technique={technique} techniques={techniques} onTechnique={(value) => onSelection({ technique: value })} disabled={locked || !!onNextBall} />
            <ShotShapeControl shape={shape} shapes={shapes} onShape={(value) => onSelection({ shape: value })} disabled={locked || !!onNextBall} />
          </div>
        </details>
        <div className="contents lg:col-start-3 lg:flex lg:items-end lg:justify-self-end lg:gap-2">
          {auxiliary ? <div className="col-start-2 row-start-2 md:ml-auto">{auxiliary}</div> : null}
          <Button className="col-start-2 row-start-1 min-w-32 md:shrink-0" size="lg" onClick={play} disabled={submitting || locked && !onNextBall || playableClubs.length === 0}>
            {submitting ? "Playing…" : onNextBall ? nextLabel : actionLabel}
          </Button>
        </div>
      </div>
    </div>
  </footer>;
}

function DistanceMeasure({ mobileLabel, label, value }: { mobileLabel: string; label: string; value: string }) {
  return <div className="min-w-0 whitespace-nowrap md:flex md:items-baseline md:gap-2">
    <span className="text-muted-foreground block text-[10px] font-medium uppercase sm:text-xs md:normal-case"><span className="md:hidden">{mobileLabel}</span><span className="hidden md:inline">{label}</span></span>
    <strong className="text-foreground block font-mono text-sm font-semibold tabular-nums md:inline">{value}</strong>
  </div>;
}

function Detail({ label, value }: { label: string; value: string }) {
  return <div className="min-w-0"><span className="text-subtle-foreground block">{label}</span><span className="text-foreground break-words font-medium tabular-nums">{value}</span></div>;
}

type Technique = { family: string; reason?: string | null };
type Shape = { shape: string; reason?: string | null };

function ShotTechniqueControl({ technique, techniques, onTechnique, disabled }: {
  technique: string; techniques: Technique[]; onTechnique: (value: string) => void; disabled: boolean;
}) {
  return <DockField label="Technique"><Select value={technique} disabled={disabled} onChange={(event) => onTechnique(event.target.value)}>
    {techniques.map((candidate) => <option key={candidate.family} value={candidate.family}>{humanize(candidate.family)}{candidate.reason ? ` — ${candidate.reason}` : ""}</option>)}
  </Select></DockField>;
}

function ShotShapeControl({ shape, shapes, onShape, disabled }: {
  shape: string; shapes: Shape[]; onShape: (value: string) => void; disabled: boolean;
}) {
  return <DockField label="Flight shape"><Select value={shape} disabled={disabled} onChange={(event) => onShape(event.target.value)}>
    {shapes.map((candidate) => <option key={candidate.shape} value={candidate.shape}>{humanize(candidate.shape)}{candidate.reason ? ` — ${candidate.reason}` : ""}</option>)}
  </Select></DockField>;
}

function DockField({ label, children, className }: { label: string; children: ReactNode; className?: string }) {
  return <label className={cn("flex min-w-0 flex-col gap-1", className)}>
    <span className="text-subtle-foreground text-[10px] tracking-[0.08em] uppercase">{label}</span>{children}
  </label>;
}

export { Detail, DistanceMeasure, DockField, ShotTechniqueControl, ShotShapeControl };
