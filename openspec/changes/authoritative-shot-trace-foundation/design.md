# Design: Authoritative Shot Trace Foundation

## Context

The current resolver already knows the pre-shot `BallState`, the literal human or AI aim target, sampled carry/lateral, projected canonical contact, resolved contact surface, and settlement ball state. `ShotSettlement` already distinguishes physical contact from the next legal playable ball. The current canonical renderer displays contact/recovery/resting markers but has no authoritative motion contract. Its retained fallback renderer synthesizes visual physics from client-side club/strategy/carry profiles and must not remain a production authority.

## Core Contract

Add a pure immutable, semantic trace equivalent to:

```text
ShotTrace {
  clubId: ClubId
  origin: Position2d
  intendedAimPoint: AimPoint
  contact: ShotContact
  transition: ShotTraceTransition? 
  finalPoint: Position2d
}

ShotTraceTransition {
  kind: RecoveryKind
  from: Position2d
  to: Position2d
}
```

`ClubId`, `AimPoint`, `Position2d`, `ShotContact`, and `RecoveryKind` remain the native vocabulary. No trajectory sample, velocity, arc, time, curve, spin, bounce, or rollout field belongs in V1.

### Point and transition semantics

| Value | Authority | Meaning |
| --- | --- | --- |
| `origin` | pre-shot `BallState.position` | Exact canonical point from which this stroke was played. |
| `intendedAimPoint` | resolved `BallStrikeIntent` / execution context | Exact canonical intended first-contact point; never current guidance or a post-shot selected target. |
| `contact` | resolver's canonical projection plus `CourseGeometry.surfaceAt` | Actual sampled first contact and its surface, including WATER and OUT_OF_BOUNDS. |
| `transition` | `ShotSettlement` | Optional non-flight rules transition from contact to recovery/replay position. |
| `finalPoint` | `ShotSettlement.ball.position` | Final legal playable point for the next stroke; it SHALL NOT be inferred from `ShotOutcome.finalSurface`. |

Normal playable contacts have no transition and `finalPoint == contact.position`. Water creates `WATER_DROP` to the legal drop, or `STROKE_AND_DISTANCE_FALLBACK` to the replay point when relief fails. Out of bounds creates `OUT_OF_BOUNDS_REPLAY` to the legal replay point. A transition describes a rules operation, not physical ball movement.

Hole-outs retain the same shape: origin, submitted/policy aim, resolved contact, no invented special path, and the settlement's final legal cup/ball position.

## Boundaries

`ShotSettlement` remains the only authority that converts contact into the next legal `BallState`; `ShotTrace` derives its transition and final point from that result and must not repeat penalty/drop decisions. `ShotOutcome` remains the scoring/gameplay summary (`carry`, `lateral`, surface, penalties, strokes, factors, settlement). It gains one optional nested trace rather than future individual physics fields. A trace is absent where canonical spatial facts or an observable consumer are legitimately absent.

## Materialization and parity

Trace materialization is an observational mode, not a separate simulator. The same resolver, seed, contact calculation, and settlement execute in every mode.

| Consumer | Trace policy |
| --- | --- |
| Human `playShot` | Materialize |
| Visible `simShot` / visible AI | Materialize |
| Interactive observable extension | Materialize |
| `simHole`, `simRound`, `simEvent` | Summary-only by default |
| Background tournaments, calibration corpora, bulk world simulation | Summary-only by default |

The trace factory shall reuse existing origin, aim, `ShotContact`, settlement, and final-ball references where possible. It shall not re-run resolution, geometry lookup, or settlement. Trace mode cannot affect random draws, scoring, dispersion, hazard incidence, putting, or final ball state.

## GraphQL and save behavior

Expose a nullable `ShotOutcome.trace` read model. It contains club, origin, aim point, contact, optional transition, and final point; reuse existing `Position`, `AimPoint`, and `ShotContact` projections where practical. A stale `playShot` result has neither an outcome nor a trace. Existing settlement remains available and is not replaced.

This is immediate playback data only. The world refuses snapshots while a playable event is pending, completed event shot detail has no public replay/history API, and existing historical outcomes may have no trace. Therefore no save format migration, trace backfill, or trace-history persistence is introduced. Future replay work may explicitly opt into persistence.

## Canonical playback and accessibility

Canonical SVG playback receives the result trace, not current situation guidance or a client-derived landing. It may interpolate from `origin` to `contact` as a display-only endpoint interpolation; it SHALL NOT call that path ballistic flight or introduce visual curvature, bounce, roll, or terrain crossings. It displays target, contact, final resting point, optional aim-to-contact miss vector, and an explicitly labelled/styled relief or replay transition. The target shown during feedback is `trace.intendedAimPoint`; planning guidance resumes only after feedback ends.

Timing, easing, marker treatment, and reduced-motion preference remain frontend-owned. Reduced motion still presents all semantic markers and recovery/replay distinction without requiring animation to understand the result.

## Synthetic-flight retirement

The current client profiles, strategy/club mappings, carry-based club inference, synthetic physics loop, and client-generated outcome placement are obsolete authority. Remove or isolate them only after all of these gates pass:

1. every active `PlayingHole` uses canonical geometry;
2. visible canonical shots return traces;
3. canonical playback covers normal, water, OB, recovery, hole-out, and reduced-motion states;
4. no live production play component depends on synthetic physics profiles; and
5. focused renderer/API tests prove trace-driven playback.

No fallback may silently substitute client-generated geography or flight for an active canonical event.

## Risks and mitigations

- **A straight interpolation appears physical.** Label/document it as endpoint interpolation and retain no flight-path domain field.
- **Penalty state is rendered from `finalSurface`.** Require final marker/lie to use settlement ball state and test water/OB transitions.
- **Refetch swaps the target marker.** Freeze the trace aim through result feedback.
- **Trace allocation changes calibration.** Test trace-on vs trace-off equality under identical seed/context and keep background summary-only.
- **Legacy code remains reachable.** Use the objective removal gates and production-import checks.

## Deferred evolution

Only when the backend computes them may future changes add launch direction, authoritative samples, apex, terrain intersections, bounce events, roll samples, spin response, or directional-wind effects. Such fields evolve `ShotTrace` additively and do not change V1 semantics.
