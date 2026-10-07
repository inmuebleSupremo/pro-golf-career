"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import type { HoleGeom, ResolvedShot } from "@/lib/play/hole-geometry";
import { shotRouterFromCarry, type ShotPhysicsProfile } from "@/lib/play/shot-router";
import type { LeaderboardRow, Outcome, Scorecard, Situation } from "@/components/play/play-shared";

/** The player's raw shot inputs, present for a played shot and absent for a simmed one. */
export interface ShotInputs {
  readonly club: string;
  readonly targetDistance: number;
}

/**
 * The Play Mode sequence controller (spec: web-hole-visualization, Play Event polish step 4).
 *
 * The server advances the moment a shot resolves — the next `situation`/`hole` arrive right after a putt drops.
 * That is too soon for the between-hole ceremony, so this hook owns a client-side, timer-driven sequence that
 * decouples *what the stage shows* from *what the server reports*. Free play tracks the live hole; a holed shot
 * opens a ceremony that *freezes* the finishing hole (hole-out → score) before the wipe reveals the server's
 * next hole under a pre-hole intro. A single `locked` flag spans every non-interactive phase so the shot
 * controls can be disabled throughout, and it clears the last shot at the two points a hole actually changes
 * (the ceremony reveal, and a multi-hole sim jump) so a prior hole's ball never animates on the next hole.
 */
export type SeqPhase = "intro" | "play" | "holeout" | "score" | "wipe";

/** The post-hole summary shown on the score overlay, derived from the freshly-advanced scorecard/leaderboard. */
export interface PostHoleSummary {
  readonly holeNumber: number;
  readonly par: number;
  readonly strokes: number;
  readonly toParHole: number;
  readonly roundToPar: number;
  readonly roundNumber: number;
  /** Cumulative event score — shown from round 2 onward (a single round has nothing wider to total). */
  readonly eventToPar: number | null;
}

export interface PlaySequence {
  readonly phase: SeqPhase;
  /** The hole currently painted on the stage (frozen through the ceremony, else the live hole). */
  readonly displayHole: HoleGeom | null;
  /** The situation whose chrome (hole/par chip) belongs with the displayed hole. */
  readonly displaySituation: Situation;
  /** The shot to play back on the displayed hole, or null for a clean hole. */
  readonly playbackShot: ResolvedShot | null;
  /** The physics profile that shapes the playback flight, resolved by the shot router; null for a clean hole. */
  readonly playbackProfile: ShotPhysicsProfile | null;
  /** The post-hole summary, present only during the `score` phase. */
  readonly postHole: PostHoleSummary | null;
  /** The last shot's outcome for the hole being played, for the dock's note; cleared as a new hole comes in. */
  readonly lastOutcome: Outcome | null;
  /** True whenever the shot controls must be disabled (any phase but free play, and during a shot's flight). */
  readonly locked: boolean;
  /** Called by the shot controls the instant a single shot resolves; `inputs` is present for a played shot, absent for a simmed one. */
  readonly onShotResolved: (outcome: Outcome, inputs?: ShotInputs) => void;
  /** Called when a multi-hole sim (hole/round/event) jumps the state past one or more holes. */
  readonly onSimJump: () => void;
}

// Phase durations (ms). Reduced motion collapses movement but still lets the information land.
const FULL = { intro: 1600, holeout: 1500, score: 2200, wipe: 800, shot: 1600 } as const;
const REDUCED = { intro: 800, holeout: 400, score: 1600, wipe: 1, shot: 400 } as const;

function toResolved(o: Outcome): ResolvedShot {
  return { finalSurface: o.finalSurface, carry: o.carry, lateral: o.lateral, distanceRemaining: o.distanceRemaining, settlement: o.settlement };
}

function usePrefersReducedMotion(): boolean {
  const [reduced, setReduced] = useState(false);
  useEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => setReduced(query.matches);
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, []);
  return reduced;
}

export function usePlaySequence({
  situation,
  hole,
  scorecard,
  leaderboard,
  playerGolferId,
}: {
  situation: Situation;
  hole: HoleGeom | null;
  scorecard: Scorecard | null;
  leaderboard: LeaderboardRow[];
  playerGolferId: string | null;
}): PlaySequence {
  const reduced = usePrefersReducedMotion();
  const D = reduced ? REDUCED : FULL;

  const [phase, setPhase] = useState<SeqPhase>("intro");
  const [playbackShot, setPlaybackShot] = useState<ResolvedShot | null>(null);
  const [playbackProfile, setPlaybackProfile] = useState<ShotPhysicsProfile | null>(null);
  const [lastOutcome, setLastOutcome] = useState<Outcome | null>(null);
  const [shotAnimating, setShotAnimating] = useState(false);
  // The finishing hole, snapshotted while the ceremony holds over the server's advance.
  const [frozen, setFrozen] = useState(false);
  const [frozenHole, setFrozenHole] = useState<HoleGeom | null>(null);
  const [frozenSit, setFrozenSit] = useState<Situation | null>(null);

  // Live server values, mirrored into refs so event callbacks capture the pre-advance hole. Synced in an effect
  // (never during render) so React's ref rules hold.
  const holeRef = useRef(hole);
  const sitRef = useRef(situation);
  useEffect(() => {
    holeRef.current = hole;
    sitRef.current = situation;
  });

  // A single resolved shot (played or simmed). A holed shot opens the between-hole ceremony, snapshotting the
  // hole it finished; any other shot just plays back while the controls stay locked for the flight.
  const onShotResolved = useCallback((outcome: Outcome, inputs?: ShotInputs) => {
    setPlaybackShot(toResolved(outcome));
    // Route the shot to a physics profile: from the player's real inputs when they played it, otherwise inferred
    // from the carry when the server simmed it.
    setPlaybackProfile(
      inputs
        ? shotRouterFromCarry(outcome.carry)
        : shotRouterFromCarry(outcome.carry),
    );
    setLastOutcome(outcome);
    if (outcome.distanceRemaining <= 0) {
      setFrozenHole(holeRef.current);
      setFrozenSit(sitRef.current);
      setFrozen(true);
      setPhase("holeout");
    } else {
      setShotAnimating(true);
    }
  }, []);

  // A multi-hole sim jumped the state forward — skip the ceremony, drop the finished hole's ball, and greet the
  // landed hole with its intro.
  const onSimJump = useCallback(() => {
    setPlaybackShot(null);
    setPlaybackProfile(null);
    setLastOutcome(null);
    setShotAnimating(false);
    setFrozen(false);
    setPhase("intro");
  }, []);

  // Drive the timed progression through each non-interactive phase.
  useEffect(() => {
    if (phase === "intro") {
      const t = setTimeout(() => setPhase("play"), D.intro);
      return () => clearTimeout(t);
    }
    if (phase === "holeout") {
      const t = setTimeout(() => setPhase("score"), D.holeout);
      return () => clearTimeout(t);
    }
    if (phase === "score") {
      const t = setTimeout(() => setPhase("wipe"), D.score);
      return () => clearTimeout(t);
    }
    if (phase === "wipe") {
      // Under the wipe's cover (mid-sweep) drop the freeze so the live next hole reveals, then — at the end of
      // the sweep — move on to its pre-hole intro.
      const swap = setTimeout(() => {
        setFrozen(false);
        setPlaybackShot(null);
        setPlaybackProfile(null);
        setLastOutcome(null);
      }, Math.max(1, D.wipe / 2));
      const done = setTimeout(() => setPhase("intro"), D.wipe);
      return () => {
        clearTimeout(swap);
        clearTimeout(done);
      };
    }
    return;
  }, [phase, D.intro, D.holeout, D.score, D.wipe]);

  // Release the flight lock after a non-holing shot has played out.
  useEffect(() => {
    if (!shotAnimating) return;
    const t = setTimeout(() => setShotAnimating(false), D.shot);
    return () => clearTimeout(t);
  }, [shotAnimating, D.shot]);

  const displayHole = frozen ? frozenHole : hole;
  const displaySituation = frozen ? frozenSit ?? situation : situation;

  const postHole: PostHoleSummary | null = useMemo(() => {
    if (phase !== "score" || !frozenSit || !scorecard) return null;
    const finishing = frozenSit.holeNumber;
    const holeRow = scorecard.holes.find((h) => h.holeNumber === finishing);
    const par = holeRow?.par ?? frozenSit.par;
    const strokes = holeRow?.strokes ?? frozenSit.strokesThisHole;
    const playerScore = playerGolferId ? leaderboard.find((r) => r.golfer.id === playerGolferId)?.score ?? null : null;
    return {
      holeNumber: finishing,
      par,
      strokes,
      toParHole: strokes - par,
      roundToPar: scorecard.toPar,
      roundNumber: scorecard.roundNumber,
      eventToPar: scorecard.roundNumber >= 2 ? playerScore : null,
    };
  }, [phase, frozenSit, scorecard, leaderboard, playerGolferId]);

  const locked = phase !== "play" || shotAnimating;

  return {
    phase,
    displayHole,
    displaySituation,
    playbackShot,
    playbackProfile,
    postHole,
    lastOutcome,
    locked,
    onShotResolved,
    onSimJump,
  };
}
