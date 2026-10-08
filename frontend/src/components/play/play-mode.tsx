"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, ChevronUp, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { humanize } from "@/lib/play/options";
import { StageHole } from "@/components/play/hole-transition";
import { usePlaySequence } from "@/components/play/use-play-sequence";
import type { HoleGeom } from "@/lib/play/hole-geometry";
import { buildBallStrikeIntent } from "@/lib/play/intent";
import {
  defaultClub,
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
  type Situation,
} from "@/components/play/play-shared";
import {
  usePlayShot,
  usePlayPutt,
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
}: {
  id: string;
  event: CurrentEvent | null;
  situation: Situation;
  hole: HoleGeom | null;
  scorecard: Scorecard | null;
  leaderboard: LeaderboardRow[];
  pressure: number | null;
  playerGolferId: string | null;
}) {
  const router = useRouter();
  const [tab, setTab] = useState<DrawerTab | null>(null);
  const [aimState, setAimState] = useState(() => ({
    revision: situation.shotRevision,
    point: situation.guidance?.primary ?? { x: 0, y: 0 },
  }));
  const aimPoint = aimState.revision === situation.shotRevision
    ? aimState.point
    : situation.guidance?.primary ?? { x: 0, y: 0 };
  const setAimPoint = (point: { x: number; y: number }) => setAimState({ revision: situation.shotRevision, point });

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
            onAimPoint={setAimPoint}
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
        />
      </div>

      <ActionDock
        key={`${situation.holeNumber}-${situation.shotNumber}`}
        id={id}
        situation={situation}
        hole={hole}
        aimPoint={aimPoint}
        onAimPoint={setAimPoint}
        onShot={seq.onShotResolved}
        onSimJump={seq.onSimJump}
        lastOutcome={seq.lastOutcome}
        locked={seq.locked}
      />
    </div>
  );
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
  onShot,
  onSimJump,
  lastOutcome,
  locked,
}: {
  id: string;
  situation: Situation;
  hole: HoleGeom | null;
  aimPoint: { x: number; y: number };
  onAimPoint: (point: { x: number; y: number }) => void;
  onShot: (outcome: Outcome) => void;
  onSimJump: () => void;
  lastOutcome: Outcome | null;
  /** True through every transitional phase — the controls are disabled so the sequence can't be broken. */
  locked: boolean;
}) {
  const play = usePlayShot(id);
  const putt = usePlayPutt(id);
  const [club, setClub] = useState(() => defaultClub(situation));
  const [technique, setTechnique] = useState("FULL");
  const [shape, setShape] = useState("STRAIGHT");
  const [formError, setFormError] = useState<string | null>(null);
  const clubs = situation.guidance?.clubs ?? [];
  const selectedClub = clubs.find((candidate) => candidate.club === club);
  const playableClubs = clubs.filter((candidate) => candidate.families.some((family) => family.available));
  const puttingAvailable = situation.lie === "GREEN" || situation.lie === "FRINGE";
  const puttRoute = situation.lie === "GREEN" || technique === "PUTT";
  const techniques = puttRoute && situation.lie === "GREEN"
    ? [{ family: "PUTT", available: true, reason: null }]
    : [
      ...(puttingAvailable ? [{ family: "PUTT", available: true, reason: null }] : []),
      ...(selectedClub?.families ?? []),
    ];
  const origin = hole?.ball?.position ?? hole?.geometry?.tee ?? { x: 0, y: 0 };
  const targetDistance = Math.hypot(aimPoint.x - origin.x, aimPoint.y - origin.y);
  const landingTarget = technique === "PITCH" || technique === "CHIP";
  const selectedTechnique = selectedClub?.families.find((candidate) => candidate.family === technique);
  const shapes = selectedTechnique?.shapes ?? [{ shape: "STRAIGHT", available: true, reason: null }];

  async function onPlay() {
    setFormError(null);
    try {
      const submission = puttRoute
        ? (await putt.mutateAsync(situation.shotRevision)).playPutt
        : (await play.mutateAsync(buildBallStrikeIntent(club, aimPoint, technique, shape, situation.shotRevision))).playShot;
      if (submission.stale || !submission.outcome) {
        setFormError("That shot plan is stale. Choose the target again.");
        return;
      }
      onShot(submission.outcome);
    } catch {
      setFormError("Couldn't play that shot. Try again.");
    }
  }

  return (
    <footer className="border-border bg-surface/70 border-t px-4 py-3 backdrop-blur-md md:px-6">
      <div className="mx-auto flex max-w-5xl flex-wrap items-end gap-x-3 gap-y-2.5">
        {!puttRoute ? <DockField label="Club">
          <Select value={club} onChange={(e) => setClub(e.target.value)}>
            {playableClubs.map((candidate) => (
              <option key={candidate.club} value={candidate.club}>
                {candidate.label}
              </option>
            ))}
          </Select>
        </DockField> : null}
        <DockField label="Technique">
          <Select value={puttRoute && situation.lie === "GREEN" ? "PUTT" : technique} onChange={(e) => setTechnique(e.target.value)} disabled={situation.lie === "GREEN"}>
            {techniques.map((candidate) => (
              <option key={candidate.family} value={candidate.family} disabled={!candidate.available}>
                {humanize(candidate.family)}{candidate.reason ? ` — ${candidate.reason}` : ""}
              </option>
            ))}
          </Select>
        </DockField>
        {!puttRoute ? <DockField label="Flight shape">
          <Select value={shape} onChange={(e) => setShape(e.target.value)}>
            {shapes.map((candidate) => <option key={candidate.shape} value={candidate.shape} disabled={!candidate.available}>
              {humanize(candidate.shape)}{candidate.reason ? ` — ${candidate.reason}` : ""}
            </option>)}
          </Select>
        </DockField> : null}
        {!puttRoute ? <DockField label={landingTarget ? "Landing X" : "Target X"}>
          <Input
            type="number"
            value={aimPoint.x}
            onChange={(e) => onAimPoint({ ...aimPoint, x: Number(e.target.value) })}
            className="w-24"
          />
        </DockField> : null}
        {!puttRoute ? <DockField label={landingTarget ? "Landing Y" : "Target Y"}>
          <Input type="number" value={aimPoint.y} onChange={(e) => onAimPoint({ ...aimPoint, y: Number(e.target.value) })} className="w-24" />
        </DockField> : null}
        {!puttRoute ? <div className="flex gap-1" aria-label="Nudge target">
          <Button variant="secondary" size="sm" onClick={() => onAimPoint({ ...aimPoint, x: aimPoint.x - 5 })}>←</Button>
          <Button variant="secondary" size="sm" onClick={() => onAimPoint({ ...aimPoint, y: aimPoint.y + 5 })}>↑</Button>
          <Button variant="secondary" size="sm" onClick={() => onAimPoint({ ...aimPoint, y: aimPoint.y - 5 })}>↓</Button>
          <Button variant="secondary" size="sm" onClick={() => onAimPoint({ ...aimPoint, x: aimPoint.x + 5 })}>→</Button>
        </div> : null}
        <div className="text-muted-foreground text-xs" aria-live="polite">
          {puttRoute ? "Putting uses the existing make-and-leave model" : `${landingTarget ? "Landing target" : "Target"} ${Math.round(targetDistance)} yds · ${selectedClub ? `${Math.round(selectedClub.normalReach)} yd normal reach` : "club guidance unavailable"}`}
        </div>
        {situation.guidance ? <div className="flex gap-1"><Button variant="secondary" size="sm" onClick={() => onAimPoint(situation.guidance!.safe)}>Safe</Button><Button variant="secondary" size="sm" onClick={() => onAimPoint(situation.guidance!.primary)}>Primary</Button><Button variant="secondary" size="sm" onClick={() => onAimPoint(situation.guidance!.aggressive)}>Aggressive</Button></div> : null}

        <div className="order-last w-full min-w-0 sm:order-none sm:w-auto sm:flex-1">
          {formError ? (
            <p role="alert" className="text-destructive text-sm">
              {formError}
            </p>
          ) : lastOutcome ? (
            <OutcomeNote outcome={lastOutcome} />
          ) : null}
        </div>

        <SimMenu id={id} onShot={onShot} onSimJump={onSimJump} disabled={locked} />
        <Button size="lg" onClick={onPlay} disabled={play.isPending || putt.isPending || locked}>
          {play.isPending || putt.isPending ? "Playing…" : puttRoute ? "Putt" : "Play shot"}
        </Button>
      </div>
    </footer>
  );
}

function DockField({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-subtle-foreground text-[10px] tracking-[0.08em] uppercase">{label}</span>
      {children}
    </label>
  );
}

function SimMenu({
  id,
  onShot,
  onSimJump,
  disabled,
}: {
  id: string;
  onShot: (outcome: Outcome) => void;
  onSimJump: () => void;
  disabled?: boolean;
}) {
  const simShot = useSimShot(id);
  const simHole = useSimHole(id);
  const simRound = useSimRound(id);
  const simEvent = useSimEvent(id);
  const busy = simShot.isPending || simHole.isPending || simRound.isPending || simEvent.isPending || (disabled ?? false);
  const [open, setOpen] = useState(false);

  // A single simmed shot plays back like a manual one; simming a hole/round/event jumps past whole holes, so it
  // skips the between-hole ceremony and greets the landed hole with its intro.
  const items: { label: string; run: () => void }[] = [
    { label: "Sim this shot", run: () => simShot.mutateAsync().then((r) => onShot(r.simShot)).catch(() => {}) },
    { label: "Sim to end of hole", run: () => simHole.mutateAsync().then(onSimJump).catch(() => {}) },
    { label: "Sim to end of round", run: () => simRound.mutateAsync().then(onSimJump).catch(() => {}) },
    { label: "Sim to end of event", run: () => simEvent.mutateAsync().then(onSimJump).catch(() => {}) },
  ];

  return (
    <div className="relative">
      <Button variant="secondary" size="lg" disabled={busy} onClick={() => setOpen((o) => !o)} aria-haspopup="menu" aria-expanded={open}>
        Sim
        <ChevronUp className={cn("size-4 transition-transform", open ? "" : "rotate-180")} aria-hidden="true" />
      </Button>
      {open ? (
        <>
          <div className="fixed inset-0 z-[var(--z-popover)]" onClick={() => setOpen(false)} aria-hidden="true" />
          <div
            role="menu"
            className="border-border bg-surface absolute right-0 bottom-full z-[var(--z-popover)] mb-2 w-52 overflow-hidden rounded-lg border shadow-lg"
          >
            {items.map((it) => (
              <button
                key={it.label}
                role="menuitem"
                disabled={busy}
                onClick={() => {
                  setOpen(false);
                  it.run();
                }}
                className="hover:bg-background block w-full px-3.5 py-2.5 text-left text-sm transition-colors disabled:opacity-50"
              >
                {it.label}
              </button>
            ))}
          </div>
        </>
      ) : null}
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
}: {
  tab: DrawerTab | null;
  onTab: (t: DrawerTab) => void;
  onClose: () => void;
  event: CurrentEvent | null;
  scorecard: Scorecard | null;
  leaderboard: LeaderboardRow[];
  playerGolferId: string | null;
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
          {tab === "details" ? <DetailsTab event={event} scorecard={scorecard} /> : null}
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

function DetailsTab({ event, scorecard }: { event: CurrentEvent | null; scorecard: Scorecard | null }) {
  return (
    <div className="grid grid-cols-2 gap-5">
      {event ? <Stat label="Event" value={event.name} /> : null}
      {event ? <Stat label="Location" value={event.location} /> : null}
      {event ? <Stat label="Course" value={humanize(event.courseType)} /> : null}
      {scorecard ? <Stat label="Round" value={`${scorecard.roundNumber} of ${ROUNDS_PER_EVENT}`} /> : null}
      {scorecard ? <Stat label="Hole" value={`${scorecard.currentHole} of 18`} /> : null}
    </div>
  );
}
