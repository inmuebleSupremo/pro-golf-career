# Design: Trajectory Shape and Wind Control

## Context and invariants

`AimPoint` remains the absolute canonical intended first-contact point. It is neither a launch direction, a final-rest request, nor a player-selected curvature or height. `ShotSettlement` remains the sole authority for legal final ball state, recovery, replay, and post-contact roll. A trace remains observational: materializing it must not alter random draws, contact, score, or settlement.

The current spatial resolver samples scalar carry/lateral, projects one contact toward `aimTarget`, then settles it. This change keeps the existing execution distribution as its base but replaces direct contact projection with one pure flight result. The flight result—not a client animation and not a sample list—owns the contact position.

## Player identity and shape semantics

Add immutable `Handedness { RIGHT, LEFT }` to player identity. It is used only by the shape-direction mapping and is not a skill, strategy, equipment property, or UI decision. Existing persisted players and generator paths default to `RIGHT`; golfer creation accepts nullable handedness and defaults omission to `RIGHT`, while player-profile reads return the resolved identity.

Add:

```text
ShotShape = STRAIGHT | FADE | DRAW
BallStrikeIntent { club, aimPoint, shotFamily, shotShape }
```

The mapping uses the aim-relative `ShotFrame`, where positive lateral is golfer-right:

| Handedness | FADE mid-flight bulge / return | DRAW mid-flight bulge / return |
| --- | --- | --- |
| RIGHT | golfer-left, then right toward `AimPoint` | golfer-right, then left toward `AimPoint` |
| LEFT | golfer-right, then left toward `AimPoint` | golfer-left, then right toward `AimPoint` |

“Begins/bulges” describes the signed intermediate position relative to the direct origin-to-`AimPoint` line, not a
frontend label or a geographic screen direction. The submitted landing target remains the intended endpoint in calm
mean conditions. In an ideal zero-error, zero-wind fixture with a target within resolver-derived effective reach,
each shape returns exactly to the submitted AimPoint at normalized progress `1`; ordinary execution error, wind, or
reach limits may move actual contact away from it. Shape produces a signed in-flight lateral bulge rather than
silently redefining `AimPoint` as an initial launch line. Thus it is a real authoritative path now while
terrain-avoidance payoff waits for the next feature.

## Wind representation and calibration channels

Weather generation retains its speed/bearing/exposure behavior. Its resolver-facing translation emits a signed local `WindVector` describing air flow in canonical hole coordinates plus `lieQuality`. The existing synthetic hole bearing remains a deterministic mapping limitation until canonical course geography/orientation exists; it must be documented as such rather than presented as real geography.

For each shot, `FlightSolution` decomposes the vector against the actual origin-to-aim frame into:

1. **Ordinary execution dispersion** — existing club/attribute/fatigue/pressure/lie/equipment sampling. It remains zero-mean and independent of directional wind displacement.
2. **Deterministic signed wind displacement** — an along-axis carry adjustment and a signed cross-axis endpoint drift. It is derived from vector components and resolver-derived airborne exposure.
3. **Wind-related uncertainty** — calibrated widening of execution uncertainty from wind magnitude, separate from deterministic drift. It must not be applied twice through a legacy unsigned-crosswind path.

`equipmentWorkability` continues to resist wind channels only; existing club lateral-control attributes govern ordinary and shaped execution consistency. No new progression attribute is introduced. A calm vector makes the wind channels exact neutral transformations.

## Flight solution

Introduce a pure, compact internal representation conceptually equivalent to:

```text
FlightSolution {
  origin
  intendedAimPoint
  contactPosition
  frame
  horizontalCurveAt(t)
  heightAt(t)
}
```

It may store derived parameters instead of callable functions, but it must evaluate a single deterministic curve for all consumers. For `t` in `[0,1]`, horizontal motion combines resolved forward progress, resolved endpoint lateral displacement, shape bulge, and the chosen directional-wind progression. Height is a bounded derived arch with zero height at `t=0` and `t=1`; family/club/carry determine its profile, never the player.

The resolver obtains `contactPosition = flight.positionAt(1)`, classifies that exact point with canonical `surfaceAt`, then applies existing roll and settlement. It must not project a second independently calculated contact.

Visible traces materialize a fixed small ordered set of `AirbornePoint { progress, position, height }` values including origin and exact contact. Background paths may use the compact solution to resolve contact but do not allocate trace/sample collections. The values at `t=0` and `t=1` are exact origin/contact, height zero; progress is finite and strictly increasing.

## Eligibility and shape-execution tradeoff

Introduce pure shape eligibility alongside family eligibility:

| Family | Allowed shapes |
| --- | --- |
| FULL | STRAIGHT, FADE, DRAW |
| CONTROLLED | STRAIGHT, FADE, DRAW |
| PITCH, CHIP, BUNKER | STRAIGHT only |
| Putt | none; dedicated path |

Shape must not be a cosmetic alias or an unconditional penalty. Before choosing numbers, calibrate the smallest observable tradeoff using current club lateral/distance dispersion, relevant control attribute, and workability—not a new skill or fixed global carry penalty. The intended contract is: a shaped shot has a distinct deterministic curve and a bounded, explainable execution-control cost relative to equivalent straight execution; the cost is only large enough to prevent a universally dominant shape. Shape must not alter calm mean intended contact merely because it is shaped. Candidate tuning can be a modest shape-control uncertainty multiplier and/or a workability/club-control-scaled curve-delivery error, selected only after corpus evidence.

## Trace, GraphQL, and frontend

Extend `ShotTrace` additively with `airbornePath`. Its endpoint is the existing contact. Roll remains contact-to-ground endpoint; transition remains a rules recovery/replay operation. A flight may terminate in water or out of bounds, then show recovery separately.

GraphQL projects application DTOs only. `BallStrikeIntentInput.shotShape` may default to `STRAIGHT` at the API edge for legacy callers, while engine intent is non-null. Shot guidance exposes per-club/family shape availability. The frontend’s only new player control is shape; it continues to select the literal landing target. Playback consumes returned samples with frontend-owned timing/reduced-motion treatment and never interpolates or reconstructs physical flight when a path exists. It locks canvas targeting while a result is playing, then clears only the visual result state after a non-holing playback so the next target is selected from the updated ball position; textual feedback and resolver state remain intact.

When present, trace roll is a separate visual phase: airborne samples end at contact, then the browser may
interpolate only between authoritative `roll.from` and `roll.to`, and only then reveal final settlement. A trace with
no roll finishes at contact; recovery/replay follows its authoritative transition rather than invented ground travel.
The presentation must make a positive authoritative roll comprehensible at normal whole-hole scale even when the
endpoint separation is below a moving-ball's apparent diameter. It therefore uses an explicit visible `Rolling` phase
and sufficient timing and/or non-geometric emphasis for the existing endpoint facts, while keeping physical endpoints,
their scale, and resolver outcomes unchanged. Contact and final settlement remain separate labelled facts whenever
they differ; final ball rendering remains withheld until the roll ends. A missing `ShotTrace.roll` never receives a
synthetic rolling phase, and a recovery/replay transition remains visibly and semantically distinct from roll.

The play read model additionally exposes the current effective wind flow in canonical hole coordinates. Its direction
means the direction air moves **toward**, not a geographic bearing; magnitude uses the resolver's effective internal
wind units after existing weather/exposure conversion. The renderer projects that server-authored vector as a small
arrow/strength readout on the canonical hole, performs no drift/carry/contact calculation, and does not imply north.
The arrowhead must be symmetric and visibly point at the projected flow endpoint; its nearby label explicitly says
`Wind toward` and names the returned effective-wind unit. A development-only component preview may render fixed,
server-shaped effective-wind fixtures for headwind, tailwind, and either crosswind. It is a presentation test seam
only: it cannot mutate conditions, player state, intent, resolver inputs, or live gameplay authority.

## AI, materialization, and future terrain response

AI chooses the same `BallStrikeIntent`. Its initial policy defaults to STRAIGHT except where a deterministic, calibrated rule selects a legal shape; no AI-only shape mechanics exist. Human shots, visible simulated shots, and AI shots resolve through the same `FlightSolution` and may materialize the same trace. Bulk simulation remains summary-only.

`flight-terrain-response` is expressly responsible for asking `FlightSolution` for continuous positions/segments and determining earliest intersections. It must not treat the display sample list as collision truth. Until then, terrain along the airborne curve has no effect; only the exact first-contact endpoint is classified.

## Risks and decisions to validate

- The synthetic per-hole weather orientation is not geographic; retain it as a documented compatibility mapping until course orientation is modeled.
- Shape will be mechanically authoritative before it becomes tactically useful for obstacle avoidance. Its small execution tradeoff must not distort scoring merely to manufacture immediate payoff.
- New wind drift can materially change scoring. Fixed-seed calm `STRAIGHT` carry/lateral/contact/settlement/score/RNG equality and calm carry/distance-dispersion/lateral-dispersion/scoring corpus bounds are release gates.
- Handedness snapshot defaults must avoid breaking historical saves.
- Effective-cup placement is a verified pre-existing integration blocker: hard setup pin offsets can locate the cup
  outside the effective GREEN. Its remedy must be a separate, geometry-aware course-generation change that protects
  active-cup GREEN classification, edge clearance, retained-generator/save compatibility, hole completion, and
  scoring calibration. This feature neither changes nor works around that defect.
