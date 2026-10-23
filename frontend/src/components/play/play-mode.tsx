"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Eye, EyeOff, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useForegroundOperation } from "@/components/loading/foreground-loading-provider";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { humanize } from "@/lib/play/options";
import { StageHole } from "@/components/play/hole-transition";
import { usePlaySequence } from "@/components/play/use-play-sequence";
import type { HoleGeom } from "@/lib/play/hole-geometry";
import { acceptCourseAim, aimWithinEnvelope, buildBallStrikeIntent, currentAimPoint } from "@/lib/play/intent";
import { currentPhysicalPuttControls, cupDirectedLine, physicalPuttControlSource, validPaceFeet } from "@/lib/play/physical-putting";
import { shotSubmissionFeedback } from "@/lib/play/shot-feedback";
import { formatShotDistance, shotDistanceReadout } from "@/lib/play/distance";
import {
  formatScore,
  Leaderboard,
  OutcomeNote,
  ROUNDS_PER_EVENT,
  Stat,
  scoreToneClass,
  ScorecardStrip,
  type CurrentEvent,
  type LeaderboardRow,
  type Outcome,
  type Scorecard,
  validShotSelection,
  type Situation,
} from "@/components/play/play-shared";
import {
  usePlayShot,
  usePlayPutt,
  usePlayPhysicalPutt,
  useSimEvent,
  useSimHole,
  useSimRound,
  useSimShot,
} from "@/lib/api/play";

type DrawerTab = "leaderboard" | "scorecard" | "details";
const TABS: readonly DrawerTab[] = ["leaderboard", "scorecard", "details"];

/**
 * Immersive Play Mode (spec: web-hole-visualization): a full-viewport surface that covers the management shell
 * for the duration of a round. The hole owns the stage; the HUD carries identity + score; the dock carries the
 * shot controls; and the leaderboard / scorecard / details live in a right drawer you summon. Body scroll is
 * locked while it's mounted; leaving returns to the career hub with the event still pending.
 */
export function PlayMode({
  id,
  event,
  situation,
  hole,
  scorecard,
  leaderboard,
  pressure,
  playerGolferId,
  courseGeneratorVersion,
}: {
  id: string;
  event: CurrentEvent | null;
  situation: Situation;
  hole: HoleGeom | null;
  scorecard: Scorecard | null;
  leaderboard: LeaderboardRow[];
  pressure: number | null;
  playerGolferId: string | null;
  courseGeneratorVersion: number;
}) {
  const router = useRouter();
  const [tab, setTab] = useState<DrawerTab | null>(null);
  const [aimState, setAimState] = useState(() => ({
    selection: { revision: situation.shotRevision, point: situation.guidance?.primary ?? { x: 0, y: 0 } },
    usedCourseAim: false,
  }));
  const aimPoint = currentAimPoint(aimState.selection, situation.shotRevision, situation.guidance?.primary ?? { x: 0, y: 0 });
  const setAimPoint = (point: { x: number; y: number }) => {
    if (aimWithinEnvelope(point, situation.aimEnvelope)) {
      setAimState((current) => ({ ...current, selection: { revision: situation.shotRevision, point } }));
    }
  };
  const setCourseAimPoint = (point: { x: number; y: number }) =>
    setAimState((current) => acceptCourseAim(current, situation.shotRevision, aimPoint, point, situation.aimEnvelope));
  const physicalAimBall = hole?.ball?.position ?? hole?.geometry?.tee ?? { x: 0, y: 0 };
  const physicalAimCup = hole?.geometry?.cup ?? physicalAimBall;
  const physicalAimSource = physicalPuttControlSource(situation.shotRevision, physicalAimBall, physicalAimCup);
  const [physicalAimState, setPhysicalAimState] = useState(() => ({ revision: situation.shotRevision, source: physicalAimSource, direction: cupDirection(hole) }));
  const physicalAimDirection = physicalAimState.revision === situation.shotRevision && physicalAimState.source === physicalAimSource
    ? physicalAimState.direction : cupDirection(hole);
  const setPhysicalAimDirection = (direction: { x: number; y: number }) =>
    setPhysicalAimState({ revision: situation.shotRevision, source: physicalAimSource, direction });
  const [showGreenReading, setShowGreenReading] = useState(false);

  // The client-side sequence: it owns what the stage shows and when the shot controls are locked, so the
  // between-hole ceremony (hole-out → score → wipe → intro) can play out over the server's instant advance.
  const seq = usePlaySequence({ situation, hole, scorecard, leaderboard, playerGolferId });

  // Lock body scroll while the immersive surface is up; restore on exit.
  useEffect(() => {
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previous;
    };
  }, []);

  const position = playerGolferId ? (leaderboard.find((r) => r.golfer.id === playerGolferId)?.position ?? null) : null;
  const toggle = (t: DrawerTab) => setTab((prev) => (prev === t ? null : t));

  return (
    <div
      role="dialog"
      aria-label={`Playing ${event?.name ?? "your event"}`}
      className="bg-background fixed inset-0 z-[var(--z-modal)] flex flex-col"
    >
      <PlayHud
        event={event}
        round={scorecard?.roundNumber ?? null}
        toPar={scorecard?.toPar ?? null}
        position={position}
        tab={tab}
        onToggle={toggle}
        onLeave={() => router.push(`/career/${id}`)}
      />

      <div className="relative flex min-h-0 flex-1 overflow-hidden">
        <section className="relative flex min-w-0 flex-1 items-center justify-center overflow-hidden p-4">
          <StageHole
            displayHole={seq.displayHole}
            situation={seq.displaySituation}
            pressure={pressure}
            playbackShot={seq.playbackShot}
            phase={seq.phase}
            postHole={seq.postHole}
            aimPoint={aimPoint}
            onAimPoint={seq.locked ? undefined : setCourseAimPoint}
            physicalAimDirection={physicalAimDirection}
            onPhysicalAimDirection={setPhysicalAimDirection}
            showGreenReading={showGreenReading}
            onPhysicalPlaybackComplete={seq.onPhysicalPlaybackComplete}
          />
        </section>

        <InfoDrawer
          tab={tab}
          onTab={setTab}
          onClose={() => setTab(null)}
          event={event}
          scorecard={scorecard}
          leaderboard={leaderboard}
          playerGolferId={playerGolferId}
          courseGeneratorVersion={courseGeneratorVersion}
          hole={hole}
        />
      </div>

      <ActionDock
        key={`${situation.holeNumber}-${situation.shotNumber}-${situation.shotRevision}`}
        id={id}
        situation={situation}
        hole={hole}
        aimPoint={aimPoint}
        onAimPoint={setAimPoint}
        usedCourseAim={aimState.usedCourseAim}
        onShot={seq.onShotResolved}
        onSimJump={seq.onSimJump}
        lastOutcome={seq.lastOutcome}
        locked={seq.locked}
        physicalAimDirection={physicalAimDirection}
        showGreenReading={showGreenReading}
        onToggleGreenReading={() => setShowGreenReading((current) => !current)}
      />
    </div>
  );
}

function cupDirection(hole: HoleGeom | null): { x: number; y: number } {
  const ball = hole?.ball?.position ?? hole?.geometry?.tee;
  const cup = hole?.geometry?.cup;
  if (!ball || !cup) return { x: 0, y: 1 };
  return cupDirectedLine(ball, cup);
}

function PlayHud({
  event,
  round,
  toPar,
  position,
  tab,
  onToggle,
  onLeave,
}: {
  event: CurrentEvent | null;
  round: number | null;
  toPar: number | null;
  position: number | null;
  tab: DrawerTab | null;
  onToggle: (t: DrawerTab) => void;
  onLeave: () => void;
}) {
  return (
    <header className="border-border bg-surface/70 flex items-center gap-4 border-b px-4 py-2.5 backdrop-blur-md md:px-6">
      <button
        onClick={onLeave}
        className="text-muted-foreground hover:text-foreground inline-flex shrink-0 items-center gap-1.5 text-sm transition-colors"
      >
        <ArrowLeft className="size-4" aria-hidden="true" />
        <span className="hidden sm:inline">Leave round</span>
      </button>
      {event ? (
        <span className="min-w-0 truncate font-serif text-base font-medium tracking-[-0.01em]">
          {event.name}
          <span className="text-subtle-foreground ml-2 hidden text-xs font-normal md:inline">{event.location}</span>
        </span>
      ) : null}
      <div className="ml-auto flex shrink-0 items-center gap-3 md:gap-4">
        <HudStat k="Round" v={round ? `${round}` : "—"} />
        <HudStat k="Score" v={toPar == null ? "—" : formatScore(toPar)} tone={toPar == null ? undefined : scoreToneClass(toPar)} />
        <HudStat k="Pos" v={position ? `${position}` : "—"} />
        <div className="ml-1 flex items-center gap-1.5">
          {TABS.map((t) => (
            <button
              key={t}
              onClick={() => onToggle(t)}
              aria-pressed={tab === t}
              className={cn(
                "rounded-md border px-2.5 py-1 text-xs capitalize transition-colors",
                tab === t
                  ? "border-primary text-primary"
                  : "border-border text-muted-foreground hover:text-foreground",
              )}
            >
              <span className="hidden sm:inline">{t}</span>
              <span className="sm:hidden">{t.charAt(0).toUpperCase()}</span>
            </button>
          ))}
        </div>
      </div>
    </header>
  );
}

function HudStat({ k, v, tone }: { k: string; v: string; tone?: string }) {
  return (
    <div className="flex items-baseline gap-1.5">
      <span className="text-subtle-foreground text-[10px] tracking-[0.08em] uppercase">{k}</span>
      <span className={cn("font-mono text-sm font-semibold tabular-nums", tone)}>{v}</span>
    </div>
  );
}

function ActionDock({
  id,
  situation,
  hole,
  aimPoint,
  onAimPoint,
  usedCourseAim,
  onShot,
  onSimJump,
  lastOutcome,
  locked,
  physicalAimDirection,
  showGreenReading,
  onToggleGreenReading,
}: {
  id: string;
  situation: Situation;
  hole: HoleGeom | null;
  aimPoint: { x: number; y: number };
  onAimPoint: (point: { x: number; y: number }) => void;
  usedCourseAim: boolean;
  onShot: (outcome: Outcome) => void;
  onSimJump: () => void;
  lastOutcome: Outcome | null;
  /** True through every transitional phase — the controls are disabled so the sequence can't be broken. */
  locked: boolean;
  physicalAimDirection: { x: number; y: number };
  showGreenReading: boolean;
  onToggleGreenReading: () => void;
}) {
  const play = usePlayShot(id);
  const putt = usePlayPutt(id);
  const physicalPutt = usePlayPhysicalPutt(id);
  const foreground = useForegroundOperation();
  const [requestedSelection, setRequestedSelection] = useState(() => validShotSelection(situation));
  const [formError, setFormError] = useState<string | null>(null);
  const clubs = situation.guidance?.clubs ?? [];
  // Re-derive on every server situation so a stale browser selection is never submitted.
  const selection = validShotSelection(situation, requestedSelection);
  const { club, technique, shape } = selection;
  const selectedClub = clubs.find((candidate) => candidate.club === club);
  const playableClubs = clubs.filter((candidate) => candidate.families.some((family) =>
    family.available && family.shapes.some((candidateShape) => candidateShape.available)))
    .sort((a, b) => b.normalReach - a.normalReach);
  const puttingAvailable = situation.lie === "GREEN" || situation.lie === "FRINGE";
  const puttRoute = situation.lie === "GREEN" || technique === "PUTT";
  const physicalRoute = hole?.puttingPolicy === "PHYSICAL_V1" && situation.lie === "GREEN" && hole.physicalGreen != null;
  const techniques = puttRoute && situation.lie === "GREEN"
    ? [{ family: "PUTT", available: true, reason: null }]
    : [
      ...(puttingAvailable ? [{ family: "PUTT", available: true, reason: null }] : []),
      ...(selectedClub?.families.filter((candidate) => candidate.available
        && candidate.shapes.some((candidateShape) => candidateShape.available)) ?? []),
    ];
  const origin = hole?.ball?.position ?? hole?.geometry?.tee ?? { x: 0, y: 0 };
  const distances = shotDistanceReadout(situation, origin, aimPoint, selection);
  const cup = hole?.geometry?.cup ?? origin;
  const physicalControlSource = physicalPuttControlSource(situation.shotRevision, origin, cup);
  const [physicalControls, setPhysicalControls] = useState(() => currentPhysicalPuttControls(physicalControlSource, origin, cup, null));
  const activePhysicalControls = currentPhysicalPuttControls(physicalControlSource, origin, cup, physicalControls);
  const physicalDefaultPace = currentPhysicalPuttControls(physicalControlSource, origin, cup, null).paceFeet;
  const paceFeet = activePhysicalControls.paceFeet;
  const paceSliderMaximum = Math.max(20, Math.ceil(physicalDefaultPace * 1.5 / 5) * 5);
  const landingTarget = technique === "PITCH" || technique === "CHIP";
  const selectedTechnique = selectedClub?.families.find((candidate) => candidate.family === technique);
  const shapes = selectedTechnique?.shapes.filter((candidate) => candidate.available)
    ?? [{ shape: "STRAIGHT", available: true, reason: null }];
  const controlsDisabled = locked || foreground.hasPendingForegroundOperation;
  const lastShotText = lastOutcome
    ? `${lastOutcome.physicalPutt ? "Last putt" : "Last shot"}: ${lastOutcome.holed ? "Holed" : formatShotDistance(lastOutcome.carry, lastOutcome.physicalPutt != null)} · ${humanize(lastOutcome.finalSurface)}`
    : physicalRoute ? "Tap the green to set a starting line" : usedCourseAim ? "" : "Tap the course to aim";

  function updateSelection(update: Partial<typeof selection>) {
    setRequestedSelection((current) => validShotSelection(situation, { ...current, ...update }));
  }

  async function onPlay() {
    setFormError(null);
    try {
      const submission = physicalRoute
        ? (await physicalPutt.mutateAsync({ direction: physicalAimDirection, intendedRollFeet: paceFeet,
          expectedShotRevision: situation.shotRevision })).playPhysicalPutt
        : puttRoute
        ? (await putt.mutateAsync(situation.shotRevision)).playPutt
        : (await play.mutateAsync(buildBallStrikeIntent(club, aimPoint, technique, shape, situation.shotRevision))).playShot;
      if (submission.stale || !submission.outcome) {
        setFormError("That shot plan is stale. Choose the target again.");
        return;
      }
      onShot(submission.outcome);
    } catch (error) {
      setFormError(shotSubmissionFeedback(error));
    }
  }

  return (
    <footer className="border-border bg-surface relative z-20 shrink-0 border-t px-4 py-2.5 md:px-6">
      <div className="mx-auto grid max-w-6xl gap-2">
        {formError ? <p role="alert" className="text-destructive text-sm">{formError}</p> : null}
        <div className={cn("flex min-w-0 items-center gap-2", !puttRoute && "lg:grid lg:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]")}>
          <div className={cn("min-w-0 flex-1", !puttRoute && "grid grid-cols-3 gap-1 sm:gap-3 md:mx-auto md:max-w-xl md:gap-6 lg:col-start-2 lg:mx-0 lg:flex lg:w-max lg:max-w-none lg:justify-self-center lg:gap-5")} aria-live="polite">
            {!puttRoute ? <>
              <DistanceMeasure mobileLabel="Pin" label="To pin" value={distances.pin} />
              <DistanceMeasure mobileLabel="Target" label={landingTarget ? "Landing target" : "Target"} value={distances.target} />
              <DistanceMeasure mobileLabel="Reach" label="Normal reach" value={distances.reach} />
            </> : <span className="text-muted-foreground whitespace-nowrap text-xs">To pin <strong className="text-foreground font-mono font-medium">{formatShotDistance(situation.distanceToPin, physicalRoute)}</strong></span>}
          </div>
          <details name="shot-dock-panel" className={cn("group relative shrink-0", !puttRoute && "lg:col-start-3 lg:justify-self-end")}>
            <summary className="text-muted-foreground hover:text-foreground cursor-pointer select-none text-xs font-medium">Shot details</summary>
            <div className="border-border bg-surface absolute right-0 bottom-full z-[var(--z-popover)] mb-3 max-h-[30dvh] w-80 max-w-[calc(100vw-2rem)] space-y-3 overflow-y-auto rounded-lg border p-3 shadow-lg sm:w-[28rem]">
              <div className="grid grid-cols-2 gap-2 text-xs">
                <Detail label="Lie" value={humanize(situation.lie)} />
                {physicalRoute ? <Detail label="Neutral pace" value={`${physicalDefaultPace.toFixed(1)} ft`} /> : null}
                {hole?.effectiveWind ? <Detail label="Wind speed" value={`${hole.effectiveWind.magnitude} ${hole.effectiveWind.unit}`} /> : null}
              </div>
              {!puttRoute ? <div className="flex flex-wrap items-end gap-2">
                <DockField label={landingTarget ? "Landing X" : "Target X"}>
                  <Input type="number" value={aimPoint.x} min={situation.aimEnvelope?.minX} max={situation.aimEnvelope?.maxX}
                    disabled={controlsDisabled} onChange={(e) => onAimPoint({ ...aimPoint, x: Number(e.target.value) })} className="w-24" />
                </DockField>
                <DockField label={landingTarget ? "Landing Y" : "Target Y"}>
                  <Input type="number" value={aimPoint.y} min={situation.aimEnvelope?.minY} max={situation.aimEnvelope?.maxY}
                    disabled={controlsDisabled} onChange={(e) => onAimPoint({ ...aimPoint, y: Number(e.target.value) })} className="w-24" />
                </DockField>
              </div> : null}
              {lastOutcome ? <div className="border-border border-t pt-3"><p className="text-subtle-foreground mb-2 text-xs">Previous shot</p><OutcomeNote outcome={lastOutcome} /></div> : null}
            </div>
          </details>
        </div>
        <p className="text-subtle-foreground min-h-4 min-w-0 truncate text-xs" title={lastShotText}>{lastShotText}</p>
        {physicalRoute ? <div className="grid min-w-0 gap-2 lg:grid-cols-[minmax(7rem,1fr)_minmax(12rem,2fr)_auto] lg:items-end">
          <p className="text-muted-foreground hidden text-xs lg:block">Starting line: tap the green</p>
          <DockField label={`Pace · ${paceFeet.toFixed(1)} ft`}>
            <div className="flex min-w-0 items-center gap-2">
              <Input type="range" min="0.1" max={paceSliderMaximum} step="0.1" value={Math.min(paceFeet, paceSliderMaximum)} disabled={controlsDisabled}
                onChange={(event) => setPhysicalControls({ ...activePhysicalControls, paceFeet: validPaceFeet(Number(event.target.value)) })}
                className="min-w-0 flex-1" aria-label="Intended rolling distance in feet" />
              <Input type="number" min="0.1" step="0.1" value={paceFeet} disabled={controlsDisabled}
                onChange={(event) => setPhysicalControls({ ...activePhysicalControls, paceFeet: validPaceFeet(Number(event.target.value)) })}
                className="w-20 shrink-0" aria-label="Exact intended rolling distance in feet" />
            </div>
          </DockField>
          <div className="flex flex-wrap items-center gap-2">
            <Button variant="secondary" size="sm" onClick={onToggleGreenReading} disabled={controlsDisabled} aria-label="Green reading" aria-pressed={showGreenReading}>
              {showGreenReading ? <EyeOff className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
              Read green
            </Button>
            <SimMenu id={id} onShot={onShot} onSimJump={onSimJump} onError={setFormError} disabled={controlsDisabled} />
            <Button size="lg" onClick={onPlay} disabled={play.isPending || putt.isPending || physicalPutt.isPending || controlsDisabled}>
              {physicalPutt.isPending ? "Playing…" : "Roll putt"}
            </Button>
          </div>
        </div> : puttRoute ? <div className="grid min-w-0 grid-cols-[minmax(0,1fr)_auto] items-end gap-2 sm:grid-cols-[minmax(0,1fr)_auto_auto]">
          {situation.lie === "FRINGE" ? <ShotTechniqueControl technique={technique} techniques={techniques}
            onTechnique={(value) => updateSelection({ technique: value })} disabled={controlsDisabled} /> : <span className="text-muted-foreground text-xs">Putting from the green</span>}
          <div className="col-start-2 row-start-2 sm:row-start-1"><SimMenu id={id} onShot={onShot} onSimJump={onSimJump} onError={setFormError} disabled={controlsDisabled} /></div>
          <Button className="col-start-2 row-start-1 sm:col-start-3" size="lg" onClick={onPlay} disabled={putt.isPending || controlsDisabled}>{putt.isPending ? "Playing…" : "Putt"}</Button>
        </div> : <div className="grid min-w-0 grid-cols-[minmax(0,1fr)_auto] items-end gap-2 md:flex lg:grid lg:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]">
          <div className="contents lg:col-start-2 lg:flex lg:items-end lg:gap-2">
            <DockField label="Club" className="md:w-40 md:shrink-0"><Select value={club} disabled={controlsDisabled} onChange={(e) => updateSelection({ club: e.target.value })}>
              {playableClubs.map((candidate) => <option key={candidate.club} value={candidate.club}>{candidate.label}</option>)}
            </Select></DockField>
            <div className="hidden md:flex md:shrink-0 md:gap-2">
              <div className="w-32"><ShotTechniqueControl technique={technique} techniques={techniques} onTechnique={(value) => updateSelection({ technique: value })} disabled={controlsDisabled || situation.lie === "GREEN"} /></div>
              <div className="w-32"><ShotShapeControl shape={shape} shapes={shapes} onShape={(value) => updateSelection({ shape: value })} disabled={controlsDisabled} /></div>
            </div>
          </div>
          <details name="shot-dock-panel" className="relative col-start-1 row-start-2 md:hidden">
            <summary className="border-border bg-background text-foreground flex h-[var(--control-lg)] cursor-pointer items-center rounded-md border px-3 text-sm"><span className="truncate">Shot setup · {humanize(technique)} · {humanize(shape)}</span></summary>
            <div className="border-border bg-surface absolute bottom-full left-0 z-[var(--z-popover)] mb-3 grid max-h-[30dvh] w-[calc(100vw-2rem)] grid-cols-2 gap-2 overflow-y-auto rounded-lg border p-3 shadow-lg">
              <ShotTechniqueControl technique={technique} techniques={techniques} onTechnique={(value) => updateSelection({ technique: value })} disabled={controlsDisabled} />
              <ShotShapeControl shape={shape} shapes={shapes} onShape={(value) => updateSelection({ shape: value })} disabled={controlsDisabled} />
            </div>
          </details>
          <div className="contents lg:col-start-3 lg:flex lg:items-end lg:justify-self-end lg:gap-2">
            <div className="col-start-2 row-start-2 md:ml-auto"><SimMenu id={id} onShot={onShot} onSimJump={onSimJump} onError={setFormError} disabled={controlsDisabled} /></div>
            <Button className="col-start-2 row-start-1 min-w-32 md:shrink-0" size="lg" onClick={onPlay} disabled={play.isPending || putt.isPending || controlsDisabled}>
              {play.isPending ? "Playing…" : "Play shot"}
            </Button>
          </div>
        </div>}
      </div>
    </footer>
  );
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

function DockField({ label, children, className }: { label: string; children: React.ReactNode; className?: string }) {
  return (
    <label className={cn("flex min-w-0 flex-col gap-1", className)}>
      <span className="text-subtle-foreground text-[10px] tracking-[0.08em] uppercase">{label}</span>
      {children}
    </label>
  );
}

function SimMenu({
  id,
  onShot,
  onSimJump,
  onError,
  disabled,
}: {
  id: string;
  onShot: (outcome: Outcome) => void;
  onSimJump: () => void;
  onError: (message: string) => void;
  disabled?: boolean;
}) {
  const simShot = useSimShot(id);
  const simHole = useSimHole(id);
  const simRound = useSimRound(id);
  const simEvent = useSimEvent(id);
  const foreground = useForegroundOperation();
  const busy = simShot.isPending || simHole.isPending || simRound.isPending || simEvent.isPending || foreground.isPending || (disabled ?? false);

  async function simulate<T>(
    label: string,
    presentation: "immediate" | "none",
    work: () => Promise<T>,
    onSuccess: (result: T) => void,
  ) {
    onError("");
    try {
      const result = await foreground.run({ label, presentation }, work);
      if (result !== undefined) onSuccess(result);
    } catch {
      onError("The simulation couldn't be completed. Your career state is unchanged; please try again.");
    }
  }

  // A single simmed shot plays back like a manual one; simming a hole/round/event jumps past whole holes, so it
  // skips the between-hole ceremony and greets the landed hole with its intro.
  const items: { label: string; run: () => void }[] = [
    { label: "Sim this shot", run: () => void simulate("Simulating shot", "none", () => simShot.mutateAsync(), (result) => onShot(result.simShot)) },
    { label: "Sim to end of hole", run: () => void simulate("Simulating hole", "none", () => simHole.mutateAsync(), onSimJump) },
    { label: "Sim to end of round", run: () => void simulate("Simulating round", "immediate", () => simRound.mutateAsync(), onSimJump) },
    { label: "Sim to end of event", run: () => void simulate("Simulating event", "immediate", () => simEvent.mutateAsync(), onSimJump) },
  ];

  return (
    <div className="w-24 shrink-0">
      <Select aria-label="Simulation actions" value="" disabled={busy} className="h-[var(--control-lg)]"
        onChange={(event) => items[Number(event.target.value)]?.run()}>
        <option value="" disabled>Sim</option>
        {items.map((item, index) => <option key={item.label} value={index}>{item.label}</option>)}
      </Select>
    </div>
  );
}

function InfoDrawer({
  tab,
  onTab,
  onClose,
  event,
  scorecard,
  leaderboard,
  playerGolferId,
  courseGeneratorVersion,
  hole,
}: {
  tab: DrawerTab | null;
  onTab: (t: DrawerTab) => void;
  onClose: () => void;
  event: CurrentEvent | null;
  scorecard: Scorecard | null;
  leaderboard: LeaderboardRow[];
  playerGolferId: string | null;
  courseGeneratorVersion: number;
  hole: HoleGeom | null;
}) {
  const open = tab !== null;
  return (
    <>
      {open ? (
        <div className="bg-background/40 absolute inset-0 z-[var(--z-backdrop)]" onClick={onClose} aria-hidden="true" />
      ) : null}
      <aside
        aria-hidden={!open}
        className={cn(
          "border-border bg-surface absolute top-0 right-0 bottom-0 z-[var(--z-drawer)] flex w-full max-w-sm flex-col border-l shadow-2xl transition-transform duration-200 ease-out motion-reduce:transition-none",
          open ? "translate-x-0" : "translate-x-full",
        )}
      >
        <div className="border-border flex items-center gap-1 border-b px-3 py-2.5">
          {TABS.map((t) => (
            <button
              key={t}
              onClick={() => onTab(t)}
              className={cn(
                "rounded-md px-3 py-1.5 text-sm capitalize transition-colors",
                tab === t ? "bg-background text-foreground" : "text-muted-foreground hover:text-foreground",
              )}
            >
              {t}
            </button>
          ))}
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground ml-auto p-1.5" aria-label="Close panel">
            <X className="size-4" aria-hidden="true" />
          </button>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto p-4">
          {tab === "leaderboard" ? <Leaderboard rows={leaderboard} playerGolferId={playerGolferId} /> : null}
          {tab === "scorecard" ? <ScorecardTab scorecard={scorecard} /> : null}
          {tab === "details" ? <DetailsTab event={event} scorecard={scorecard} courseGeneratorVersion={courseGeneratorVersion} hole={hole} /> : null}
        </div>
      </aside>
    </>
  );
}

function ScorecardTab({ scorecard }: { scorecard: Scorecard | null }) {
  if (!scorecard) {
    return <p className="text-muted-foreground text-sm">No round in progress.</p>;
  }
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-baseline justify-between">
        <span className="text-sm font-medium">
          Round {scorecard.roundNumber} of {ROUNDS_PER_EVENT}
          <span className="text-muted-foreground font-normal"> · Hole {scorecard.currentHole} of 18</span>
        </span>
        <span className="text-subtle-foreground text-sm">
          <span className={cn("font-mono tabular-nums", scoreToneClass(scorecard.toPar))}>{formatScore(scorecard.toPar)}</span>
        </span>
      </div>
      {scorecard.holes.length > 0 ? (
        <ScorecardStrip scorecard={scorecard} />
      ) : (
        <p className="text-muted-foreground text-sm">No holes played yet this round.</p>
      )}
    </div>
  );
}

function DetailsTab({ event, scorecard, courseGeneratorVersion, hole }: {
  event: CurrentEvent | null;
  scorecard: Scorecard | null;
  courseGeneratorVersion: number;
  hole: HoleGeom | null;
}) {
  return (
    <div className="grid grid-cols-2 gap-5">
      {event ? <Stat label="Event" value={event.name} /> : null}
      {event ? <Stat label="Location" value={event.location} /> : null}
      {event ? <Stat label="Scene biome" value={humanize(event.courseType)} /> : null}
      {scorecard ? <Stat label="Round" value={`${scorecard.roundNumber} of ${ROUNDS_PER_EVENT}`} /> : null}
      {scorecard ? <Stat label="Hole" value={`${scorecard.currentHole} of 18`} /> : null}
      <Stat label="Saved world generator" value={`${courseGeneratorVersion}`} />
      {hole ? <Stat label="Active course generator" value={hole.courseGeneratorVersion == null ? "Unavailable" : `${hole.courseGeneratorVersion}`} /> : null}
      {hole ? <div className="col-span-2"><Stat label="Canonical course biome" value={humanize(hole.canonicalBiome ?? event?.courseType ?? "unknown")} /></div> : null}
      {hole ? <div className="col-span-2"><Stat label="Hole layout seed" value={hole.layoutSeed} /></div> : null}
      {hole ? <div className="col-span-2"><Stat label="Geometry source" value={hole.geometry ? `${hole.canonicalGeometrySource ?? "Backend canonical"} · ${hole.geometry.regions.length} regions` : "Unavailable"} /></div> : null}
    </div>
  );
}
