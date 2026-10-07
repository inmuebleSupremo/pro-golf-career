import { humanize } from "@/lib/play/options";

/** Shared types, helpers, and presentational pieces used by both the play console and the end-of-event screen. */

export const ROUNDS_PER_EVENT = 4;

export type Situation = {
  holeNumber: number;
  par: number;
  shotNumber: number;
  strokesThisHole: number;
  distanceToPin: number;
  lie: string;
  pinLateral: number;
  minReach: number;
  maxReach: number;
  shotRevision: string;
  aimEnvelope?: { minX: number; maxX: number; minY: number; maxY: number } | null;
  guidance?: {
    safe: { x: number; y: number };
    primary: { x: number; y: number };
    aggressive: { x: number; y: number };
    clubs: { club: string; label: string; nominalCarry: number; normalReach: number }[];
  } | null;
};

export type Outcome = {
  finalSurface: string;
  carry: number;
  lateral: number;
  distanceRemaining: number;
  hazardEntered: boolean;
  penaltyStrokes: number;
  strokes: number;
  settlement?: {
    contact: { position: { x: number; y: number }; surface: string };
    recoveryPosition?: { x: number; y: number } | null;
    recoveryKind: string;
    ball: { position: { x: number; y: number }; lie: string };
  } | null;
};

export type LeaderboardRow = {
  position: number;
  golfer: { id: string; name: string };
  score: number;
  roundsPlayed: number;
};

export type HoleScore = { holeNumber: number; par: number; strokes: number };

export type Scorecard = {
  roundNumber: number;
  currentHole: number;
  toPar: number;
  totalStrokes: number;
  holes: HoleScore[];
};

export type CurrentEvent = { name: string; location: string; courseType: string };

export function formatScore(score: number): string {
  if (score === 0) return "E";
  return score > 0 ? `+${score}` : `${score}`;
}

/** Colour for a score relative to par: under reads as success, over as destructive, level as neutral. */
export function scoreToneClass(relToPar: number): string {
  if (relToPar < 0) return "text-success";
  if (relToPar > 0) return "text-destructive";
  return "text-foreground";
}

/** A leaderboard position as an ordinal, e.g. 1 → "1st", 12 → "12th". */
export function ordinal(position: number): string {
  const suffix =
    position % 100 >= 11 && position % 100 <= 13
      ? "th"
      : (["th", "st", "nd", "rd"][position % 10] ?? "th");
  return `${position}${suffix}`;
}

export function defaultClub(s: Situation): string {
  if (s.guidance?.clubs.length) {
    return s.guidance.clubs.reduce((best, candidate) =>
      Math.abs(candidate.nominalCarry - s.distanceToPin) < Math.abs(best.nominalCarry - s.distanceToPin) ? candidate : best,
    ).club;
  }
  if (s.lie.toUpperCase().includes("GREEN")) return "PUTTER";
  if (s.distanceToPin >= 220) return "DRIVER";
  if (s.distanceToPin >= 60) return "IRON";
  return "WEDGE";
}

export function clampTarget(s: Situation): number {
  return Math.round(Math.min(Math.max(s.distanceToPin, s.minReach), s.maxReach));
}

export function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-subtle-foreground text-xs tracking-[0.08em] uppercase">{label}</span>
      <span className="text-foreground font-mono text-lg tabular-nums">{value}</span>
    </div>
  );
}

export function OutcomeNote({ outcome }: { outcome: Outcome }) {
  const holed = outcome.distanceRemaining <= 0;
  return (
    <div className="border-border bg-background flex flex-wrap items-center gap-x-2 gap-y-1 rounded-md border px-4 py-2.5 text-sm">
      <span className="text-muted-foreground">
        Carried <span className="text-foreground font-mono tabular-nums">{Math.round(outcome.carry)}</span> yds to{" "}
      </span>
      <span className="font-medium">{humanize(outcome.finalSurface)}</span>
      {holed ? (
        <span className="text-success font-medium">· holed</span>
      ) : (
        <span className="text-muted-foreground">
          · <span className="text-foreground font-mono tabular-nums">{Math.round(outcome.distanceRemaining)}</span> to
          the pin
        </span>
      )}
      {outcome.hazardEntered ? <span className="text-destructive font-medium">· hazard</span> : null}
      {outcome.penaltyStrokes > 0 ? (
        <span className="text-destructive"> · +{outcome.penaltyStrokes} penalty</span>
      ) : null}
    </div>
  );
}

export function ScorecardStrip({ scorecard }: { scorecard: Scorecard }) {
  return (
    <div className="border-border bg-surface overflow-x-auto rounded-lg border px-4 py-3">
      <div className="flex items-stretch gap-1">
        {scorecard.holes.map((hole) => (
          <HoleCell key={hole.holeNumber} hole={hole} />
        ))}
      </div>
    </div>
  );
}

function HoleCell({ hole }: { hole: HoleScore }) {
  const relToPar = hole.strokes - hole.par;
  return (
    <div className="flex min-w-8 flex-col items-center gap-1">
      <span className="text-subtle-foreground font-mono text-xs tabular-nums">{hole.holeNumber}</span>
      <span
        className={`flex size-7 items-center justify-center rounded font-mono text-sm tabular-nums ${
          relToPar < 0
            ? "bg-success/[0.12] text-success font-medium"
            : relToPar > 0
              ? "bg-destructive/[0.10] text-destructive"
              : "text-foreground"
        }`}
      >
        {hole.strokes}
      </span>
    </div>
  );
}

export function Leaderboard({
  rows,
  playerGolferId,
  embedded = false,
}: {
  rows: LeaderboardRow[];
  playerGolferId: string | null;
  /** When embedded in the translucent end-of-event scorecard, drop the solid card chrome so the scene shows through. */
  embedded?: boolean;
}) {
  const top = rows.slice(0, 10);
  const playerRow = playerGolferId ? rows.find((r) => r.golfer.id === playerGolferId) : null;
  const showPlayerBelow = playerRow && !top.some((r) => r.golfer.id === playerGolferId);
  const listClass = embedded
    ? "divide-border/50 flex flex-col divide-y"
    : "divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border";

  return (
    <div className="flex flex-col gap-4">
      {embedded ? (
        <h2 className="text-subtle-foreground text-xs font-medium tracking-[0.12em] uppercase">Final standings</h2>
      ) : null}
      {rows.length === 0 ? (
        <p className="border-border text-muted-foreground rounded-lg border border-dashed px-5 py-8 text-center text-sm">
          No standings yet.
        </p>
      ) : (
        <ul className={listClass}>
          {top.map((row) => (
            <LeaderboardRowItem key={row.golfer.id} row={row} isPlayer={row.golfer.id === playerGolferId} />
          ))}
          {showPlayerBelow ? (
            <>
              <li className="text-subtle-foreground px-4 py-1 text-center text-xs">···</li>
              <LeaderboardRowItem row={playerRow} isPlayer />
            </>
          ) : null}
        </ul>
      )}
    </div>
  );
}

function LeaderboardRowItem({ row, isPlayer }: { row: LeaderboardRow; isPlayer: boolean }) {
  return (
    <li className={`flex items-center justify-between gap-3 px-4 py-2.5 ${isPlayer ? "bg-primary/[0.08]" : ""}`}>
      <span className="flex min-w-0 items-center gap-3">
        <span className="text-subtle-foreground w-5 text-right font-mono text-sm tabular-nums">{row.position}</span>
        <span className={`truncate text-sm ${isPlayer ? "text-foreground font-medium" : ""}`}>
          {row.golfer.name}
          {isPlayer ? " (you)" : ""}
        </span>
      </span>
      <span className="font-mono text-sm tabular-nums">{formatScore(row.score)}</span>
    </li>
  );
}
