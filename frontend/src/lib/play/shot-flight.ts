/*
 * Shot playback physics — the prototype's three-phase loop (air flight, ground check, roll), ported to drive the
 * ball across the top-down hole map. The loop's structure is unchanged: gravity integrates a virtual height `z`,
 * the mid-air curve is clamped off the instant `z` hits 0, the first bounce sheds horizontal speed by `bite`, and
 * the roll decays by `fric`. Ground travel collapses to a single longitudinal axis `s` (from → to); the virtual
 * height only scales the ball radius and offsets its shadow, exactly as in the prototype. The shot is cosmetic:
 * it always settles at the sim's resolved landing (`to`), so the carry/roll split is derived from the profile to
 * make the natural bounce-and-roll come to rest there.
 *
 * Kept framework-free (no React, no DOM) so it can be unit-stepped in isolation; the component owns only the
 * fixed-timestep requestAnimationFrame driver and the SVG writes.
 */

import type { ShotPhysicsProfile } from "@/lib/play/shot-router";
import type { Point } from "@/lib/play/hole-geometry";

export type LandEffect = "splash" | "sand" | "roll";

const GRAVITY = 0.2;
const VZ_K = 0.6; // profile.vz → initial virtual vertical velocity (sets hang time; tuned to the sequence's shot budget)
const HEIGHT_K = 0.009; // virtual height → added ball-radius scale (reads as loft)
const CURVE_K = 0.32; // profile.curve (prototype px) → map-space lateral bow
const SHADOW_K = 0.035; // virtual height → shadow offset (down-right), reads as ball rising off the turf
const ROLL_STOP = 0.05; // roll settles once longitudinal speed drops below this
const FRAME_CAP = 100; // hard ceiling (~1.7s) so a long roll can't outlast the sequence's shot lock (clears the highest hang time)

/** The fixed physics timestep (ms), so flight speed is independent of the display's refresh rate. */
export const STEP_MS = 1000 / 60;
/** Base radius (px) of the travelling ball; the loop's `scale` multiplies it to read loft. */
export const TRAVEL_R = 2.6;

/** One advanced frame of a flight: where to draw the ball, how big, its shadow offset, and whether it has settled. */
export interface FlightFrame {
  readonly x: number;
  readonly y: number;
  readonly scale: number;
  readonly shadowDx: number;
  readonly shadowDy: number;
  readonly airborne: boolean;
  readonly settled: boolean;
}

/**
 * A per-step simulator for one shot's flight, from `from` to the resolved landing `to`, shaped by `profile`.
 * `step()` advances one fixed timestep and returns the ball's on-screen position, radius scale, and shadow offset;
 * it reports `settled` once the ball has come to rest at `to`. Kept as a closure so the driver stays a thin loop.
 */
export function planFlight(from: Point, to: Point, profile: ShotPhysicsProfile): { step: () => FlightFrame } {
  const dxTot = to.x - from.x;
  const dyTot = to.y - from.y;
  const dist = Math.hypot(dxTot, dyTot) || 0.0001;
  const ux = dxTot / dist;
  const uy = dyTot / dist; // unit vector along the line of play
  const nx = -uy;
  const ny = ux; // perpendicular, for the mid-air curve bow

  let z = 0;
  let velZ = 0;
  let s = 0;
  let velS: number;
  let frame = 0;
  let airFrames = 0;
  let rolling: boolean;
  let bounced = false;

  if (profile.vz > 0) {
    velZ = profile.vz * VZ_K;
    airFrames = (2 * velZ) / GRAVITY;
    // Split the shot so air-carry + the natural roll (≈ velS·(1−bite)/fric) lands on `to`: a biting wedge carries
    // almost the whole way, a low runner lands short and chases the rest.
    const rollPerCarry = (1 - profile.bite) / (airFrames * profile.fric);
    const carryDist = dist / (1 + rollPerCarry);
    velS = carryDist / airFrames; // rule: longitudinal speed = carry distance / air frames
    rolling = false;
  } else {
    // Pure roll (putt): the prototype's roll-launch speed, which overshoots and is clamped to stop at the cup.
    velS = Math.sqrt(2 * profile.fric * dist) * 1.5;
    rolling = true;
  }

  function step(): FlightFrame {
    frame += 1;
    let curveOffset = 0;

    if (!rolling) {
      velZ -= GRAVITY;
      z += velZ;
      s += velS;
      // Mid-air curve — clamped off the exact step the ball grounds (z ≤ 0), so it never bends along the roll.
      if (profile.curve !== 0 && z > 0) {
        const progress = Math.min(frame / airFrames, 1);
        curveOffset = Math.sin(progress * Math.PI) * profile.curve * CURVE_K;
      }
      if (z <= 0) {
        z = 0;
        if (!bounced) {
          bounced = true;
          velS *= 1 - profile.bite; // first-bounce spin bite
        }
        velZ = Math.abs(velZ) * profile.rest;
        if (velZ < 0.5) {
          velZ = 0;
          rolling = true;
        }
      }
    } else {
      s += velS;
      velS *= 1 - profile.fric;
    }

    const settled = s >= dist || frame >= FRAME_CAP || (rolling && velS < ROLL_STOP);
    if (settled) s = dist; // rest exactly on the sim's resolved landing

    const x = from.x + ux * s + nx * curveOffset;
    const y = from.y + uy * s + ny * curveOffset;
    const scale = 1 + z * HEIGHT_K;
    return { x, y, scale, shadowDx: z * SHADOW_K, shadowDy: z * SHADOW_K * 0.8, airborne: z > 0, settled };
  }

  return { step };
}
