"use client";

import { useEffect, useLayoutEffect, useRef, useState } from "react";

import type { Point } from "@/lib/play/hole-geometry";
import { startShotPlayback } from "@/lib/play/shot-playback-clock";
import { golfAudio } from "@/lib/play/golf-audio";
import { authoritativeGroundPath, traceBallProjection, tracePlaybackDurationMs, tracePlaybackFrame,
  tracePlaybackStatus, tracePlaybackTiming,
  transitionLabel, traversedAirbornePoints, type ShotTrace } from "@/lib/play/trace-presentation";

function usePrefersReducedMotion(): boolean {
  const [reduced, setReduced] = useState(false);
  useLayoutEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => setReduced(query.matches);
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, []);
  return reduced;
}

interface ShotMotionProps {
  trace: ShotTrace;
  project: (point: Point) => Point;
  contact: Point;
  finalPoint: Point;
  contactEffect: string;
  ballStroke: string;
  statusY: number;
  statusX?: number;
  shadowFill: string;
  tracerShadow?: boolean;
  holed?: boolean;
  onPlaybackComplete?: () => void;
}

/** Only this small SVG subtree updates on animation frames; premium scenery stays mounted and still. */
export function ShotMotionOverlay(props: ShotMotionProps) {
  const { trace, holed, onPlaybackComplete } = props;
  const completeRef = useRef(onPlaybackComplete);
  useLayoutEffect(() => { completeRef.current = onPlaybackComplete; }, [onPlaybackComplete]);
  const reduced = usePrefersReducedMotion();
  const [clock, setClock] = useState(() => ({ trace, progress: 0 }));
  const progress = reduced ? 1 : clock.trace === trace ? clock.progress : 0;

  useEffect(() => {
    if (reduced) {
      golfAudio.impact(trace, trace.club);
      golfAudio.cup(trace, !!holed);
      return;
    }
    let finished = false;
    const timing = tracePlaybackTiming(trace);
    const cancel = startShotPlayback(tracePlaybackDurationMs(trace),
      (next) => {
        golfAudio.movement(trace, next, trace.club);
        if (next >= timing.flightMs / timing.totalMs) {
          golfAudio.landing(trace, trace.contact.surface, trace.club);
        }
        setClock({ trace, progress: next });
      }, () => {
        finished = true;
        golfAudio.cup(trace, !!holed);
        completeRef.current?.();
      });
    return () => { cancel(); if (!finished) golfAudio.stopEffects(); };
  }, [trace, reduced, holed]);

  return <ShotMotionFrame {...props} progress={progress} />;
}

/** The production shot marks at one clock position, also renderable with deterministic trace fixtures. */
export function ShotMotionFrame({ trace, project, contact, finalPoint, contactEffect, ballStroke,
  statusY, statusX = 16, shadowFill, tracerShadow, progress }: ShotMotionProps & { progress: number }) {
  const playback = tracePlaybackFrame(trace, progress);
  const motion = !playback.finalVisible ? traceBallProjection(playback, project) : null;
  const traversed = traversedAirbornePoints(trace, progress);
  const tracerPath = traversed.length > 1
    ? `M${traversed.map((point) => { const screen = project(point); return `${screen.x} ${screen.y}`; }).join(" L")}`
    : null;
  const finalMatchesContact = trace.finalPoint.x === trace.contact.position.x
    && trace.finalPoint.y === trace.contact.position.y;
  const transitionFrom = trace.transition ? project(trace.transition.from) : null;
  const transitionTo = trace.transition ? project(trace.transition.to) : null;
  const rollFrom = trace.roll ? project(trace.roll.from) : null;
  const rollTo = trace.roll ? project(trace.roll.to) : null;
  const groundPath = authoritativeGroundPath(trace);
  const releasePath = rollFrom && rollTo
    ? groundPath
      ? `M${[trace.contact.position, ...groundPath.map((point) => point.position)]
        .map((point) => { const screen = project(point); return `${screen.x} ${screen.y}`; }).join(" L")}`
      : `M${rollFrom.x} ${rollFrom.y} L${rollTo.x} ${rollTo.y}`
    : null;
  const recoveryLabel = transitionLabel(trace);
  const playbackStatus = tracePlaybackStatus(trace, playback);

  return <>
    {tracerPath && tracerShadow ? <path d={tracerPath} stroke={shadowFill} strokeWidth="2.8" strokeDasharray="3 3" opacity=".5" /> : null}
    {tracerPath ? <path data-shot-tracer="true" d={tracerPath} stroke="#ffffff" strokeWidth="1.1" strokeDasharray="3 3" opacity=".8" /> : null}
    {motion && motion.lift > 0 ? <ellipse data-shot-shadow="true" cx={motion.ground.x} cy={motion.ground.y}
      rx={3 + motion.lift * .16} ry="1.5" fill={shadowFill}
      opacity={motion.shadowOpacity} pointerEvents="none" /> : null}
    {motion ? <circle data-shot-ball="true" cx={motion.ball.x} cy={motion.ball.y}
      r={3 * motion.scale} fill="#fff" stroke={ballStroke} strokeWidth=".6" /> : null}
    {playback.phase !== "airborne" ? <>
      <circle data-shot-contact="true" cx={contact.x} cy={contact.y} r="6" fill="none" stroke={contactEffect} strokeWidth="1.5" opacity=".9" />
      <circle cx={contact.x} cy={contact.y} r="2.4" fill="#fff" />
      <text x={contact.x + 5} y={contact.y - 5} fill="#fff" fontSize="5">{finalMatchesContact ? "Contact / ball" : "Contact"}</text>
    </> : null}
    {transitionFrom && transitionTo && recoveryLabel && (playback.phase === "transition" || playback.phase === "final") ? <>
      <path d={`M${transitionFrom.x} ${transitionFrom.y} L${transitionTo.x} ${transitionTo.y}`} stroke="#fff" strokeWidth="1.2" strokeDasharray="3 2" opacity=".9" />
      <text x={(transitionFrom.x + transitionTo.x) / 2 + 3} y={(transitionFrom.y + transitionTo.y) / 2 - 3} fill="#fff" fontSize="5">{recoveryLabel}</text>
    </> : null}
    {releasePath && rollFrom && rollTo && (playback.phase === "bounce" || playback.phase === "roll" || playback.phase === "final") ? <>
      <path d={releasePath} stroke="var(--accent)" strokeWidth="1.3" strokeDasharray="2 1" opacity=".95" />
      <text x={(rollFrom.x + rollTo.x) / 2 + 3} y={(rollFrom.y + rollTo.y) / 2 - 3} fill="var(--accent)" fontSize="5">{groundPath ? "Release" : "Roll"}</text>
    </> : null}
    {playbackStatus ? <text x={statusX} y={statusY} fill="var(--accent)" fontSize="6" fontWeight="700">{playbackStatus}</text> : null}
    {playback.finalVisible && !finalMatchesContact ? <>
      <circle data-shot-final="true" cx={finalPoint.x} cy={finalPoint.y} r="3" fill="#fff" stroke={ballStroke} strokeWidth=".6" />
      <text x={finalPoint.x + 5} y={finalPoint.y + 6} fill="#fff" fontSize="5">Ball</text>
    </> : null}
  </>;
}
