## Context

The current spatial resolver already has the correct authority chain:

```text
BallStrikeIntent → ShotResolver → FlightSolution.positionAt(1) → canonical contact
  → ShotSettlement / BallState → optional ShotTrace → frontend playback
```

`FlightSolution` is a continuous pure evaluator. Its nine samples are an observable projection and cannot become collision or rollout authority. `CourseGeometry.surfaceAt` classifies canonical 2D polygons, and `ShotSettlement` owns the next legal ball. `ShotExecutionProfile` currently applies a fixed family/surface response: FULL, CONTROLLED, and BUNKER have zero configured release; PITCH and CHIP have bounded values. The resolver accepts a release only when its endpoint remains on the same contact surface, otherwise silently settles at contact.

`PlayingConditions` already derives `groundFirmness` from rain and humidity, but `environmentForHole` currently exposes only signed wind and lie quality to the shot engine. `greenSpeed` is also derived but unused by shot resolution. Putting is a separate probability/proximity route and intentionally does not use the non-putting ground-response path.

The spatial foundation, authoritative trace, shape/wind, and valid effective-pin changes are complete. This change is the smallest next step: meaningful post-contact release without airborne collision, general terrain traversal, or putting physics.

## Goals / Non-Goals

**Goals:**

- Make equivalent non-putting landings release materially farther on firm than soft ground, with deterministic outcomes.
- Give low-lofted long shots more typical release potential than higher-lofted approaches under comparable inputs, while keeping PITCH and CHIP distinct.
- Use existing canonical first contact, surface classification, conditions, settlement, and trace contracts rather than creating a parallel physics path.
- Avoid an artificial all-or-nothing stop at ordinary fairway/rough or fringe/green boundaries without accepting a general surface-traversal engine.
- Preserve human/AI and visible/background parity, save loading, deterministic seeded execution, and backend-owned presentation.

**Non-Goals:**

- Airborne terrain, water, tree, or obstacle collision; forced-carry logic; or sampled-path collision checks.
- Multi-bounce, spin-state, aerodynamics, elevation/slope, green contours, spatial putting, cup/lip behaviour, or player-controlled launch/spin inputs.
- Hazard traversal/settlement caused by roll, arbitrary multi-surface travel, a course-generator rewrite, or unrelated career-system work.
- Changing the separate putting model or applying `greenSpeed` to non-putting release in this milestone.

## Decisions

### D1 — Keep first contact and flight semantics unchanged

`FlightSolution.positionAt(1)` remains the only source of first contact. Ground response begins only after that point is classified by the effective `CourseGeometry`; it neither retests flight samples nor changes carry, apex, curve, wind, or airborne collision semantics. The response direction should be derived from the continuous terminal flight direction (or an equivalent aim-frame direction derived from the same evaluator), never from UI samples.

This directly reuses the future seam established by trajectory/wind and keeps presentation observational.

### D2 — Introduce a small pure ground-response profile, not a physics state machine

The resolver should derive an immutable internal response profile from existing information: club/category, `ShotFamily`, contact surface, and normalized ground firmness. It shall produce a finite desired release distance only; it shall not create velocity, spin, bounce count, or mutable ground state.

The profile must preserve clear ordering rather than prematurely choose physical constants:

- firm ground increases eligible release relative to equivalent soft ground;
- low-lofted long FULL/CONTROLLED shots can release more than higher-lofted approaches;
- PITCH and CHIP retain independently calibrated release identities;
- difficult playable lies/surfaces reduce or suppress release according to the existing surface vocabulary;
- BUNKER and putts remain outside this change.

Existing response constants are calibration starting points, not evidence for final values. Constants and acceptance ranges must be selected from deterministic corpus measurements during implementation.

Implementation calibration (9 October 2026) uses the existing `ClubSpec.baseCarry` category proxy: eligible
FULL release is `max(0, (baseCarry - 100) * 0.06)` yards before surface and firmness adjustment, and CONTROLLED
receives 55% of that entitlement. The existing distinct PITCH (0.75 yd) and CHIP (`2 + baseCarry * 0.005` yd)
profiles remain separate. Firmness multiplies eligible release by `0.15 + 0.85 * firmness`; the existing surface
multipliers retain fairway/tee as 1.0, fringe as 0.9, green as 0.7, first cut as 0.7, primary rough as 0.45,
and suppress release on the remaining difficult/non-playable surfaces. These intentionally small constants were
accepted after the fixed-seed corpus remained within its existing broad scoring, strategy, course, tournament,
weather, and world-realism envelopes; they are calibrated release tendencies, not a physical spin model.

Alternative: add explicit launch, spin, and rebound state. Rejected: neither player choice nor current data needs that complexity to create an understandable first release improvement.

### D3 — Extend the resolver-facing condition contract with firmness only

`PlayingConditions.environmentForHole` should preserve its existing wind/lie mapping and additionally supply the already derived, normalized `groundFirmness` to the pure resolver input. Compatibility constructors/fixtures must retain a documented neutral firmness that reproduces their intended baseline unless a fixture explicitly exercises the new response.

`greenSpeed` is intentionally not a non-putting release multiplier. It describes greens and is a natural input to later spatial-putting/green work; using it now would couple an endpoint release slice to a putting model that has no slope, line, or speed simulation.

### D4 — Approved boundary policy: one ordinary playable transition, then clamp

The proposed first-milestone policy is deliberately bounded:

1. Resolve canonical first contact and desired release along the authoritative terminal ground direction.
2. If the segment stays on its starting playable surface, settle at the desired endpoint.
3. If it crosses one boundary into another ordinary playable surface, permit that one deterministic transition and settle at the desired endpoint only while no second/non-playable boundary is crossed.
4. If the path would cross a second boundary, WATER, or OUT_OF_BOUNDS, clamp to the last deterministically valid point on the current ordinary playable path. Do not create a penalty, drop, or replay from roll in this change.

Boundary location must be calculated against canonical geometry (for example, deterministic segment subdivision plus bounded refinement), not inferred from SVG pixels or a zone profile. The result is one straight, bounded ground segment with at most one ordinary transition—not a general traversal architecture.

This is the smallest policy that lets a ball visibly release from fairway into first cut/rough or from fringe onto green when the geometry permits, while preventing hidden hazard semantics. It is approved for this implementation.

Alternatives considered:

- **Same-surface-only endpoint:** simplest but preserves invisible, artificial stopping at every ordinary boundary.
- **Clamp at every boundary:** makes the stop visible but still produces implausible fairway/rough and fringe/green behaviour.
- **Unlimited traversal with hazard settlement:** more realistic in some cases, but becomes a terrain/hazard-response system with new penalty and recovery contracts; defer it.

### D5 — Reuse settlement and trace, keep the browser passive

The resolver shall produce contact, permitted roll, and final `BallState` once. `ShotSettlement` continues to own the final legal ball; `ShotTrace.roll` remains the presentation projection of the same backend-computed segment. The frontend may sequence `airborne → contact → rolling → final`, but it must not calculate firmness, release distance, boundary crossing, final lie, or a substitute endpoint.

If a one-transition release ends on a different ordinary playable surface, the final lie comes from canonical geometry and the trace's final point, not from the contact surface or client classification.

### D6 — Compatibility is load-safe; future strokes adopt the new semantics

No new randomness or persistent ball state is needed. Current saves cannot contain a pending playable event, so no in-progress ball requires migration. Completed scores/results are never re-resolved. Existing saves already persist weather-derived conditions; future unresolved strokes can deterministically use those stored conditions after loading.

Approved: apply the new deterministic response to all future strokes in all loaded careers, without a new world policy/version or migration interface. This gives existing careers the improvement and keeps the state model small. It does mean a future event re-simulated under a newer game build can differ from its historical pre-feature outcome; exact cross-build replay of unplayed shots is not a requirement for this development stage.

## Risks / Trade-offs

- **Scoring/driving-distance drift** → Measure paired fixtures and existing scoring, proximity, GIR, strategy, weather, and world corpora; choose constants only after reviewing output.
- **One-transition logic accidentally becomes traversal** → Enforce a one-transition/non-hazard cap, explicit tests, and no generic path/state API.
- **A clamp looks surprising near a hazard** → Keep contact/final markers distinct, document that hazard roll is deferred, and use a deterministic final point visibly before the invalid boundary.
- **Fixture breakage from a new environment field** → Provide documented compatibility constructors/defaults and migrate fixtures intentionally.
- **Human/AI divergence or trace allocation changes results** → Route both through the shared resolver and compare trace/materialized versus summary results under identical seeds.
- **Green-speed scope creep** → Keep it out of non-putting response; revisit only with spatial greens/putting design.

## Migration Plan

1. Add the pure resolver input/default and response profile without changing persisted snapshots.
2. Use persisted `PlayingConditions.groundFirmness` for future strokes in loaded careers; leave completed records untouched.
3. Add deterministic calibration and save/load tests before changing UI playback.
4. Expose only backend-authored roll/final facts through existing/additive trace mapping, then validate visible playback.
5. If the approved replay policy requires historical-version pinning, add that policy and explicit migration coverage before enabling the new resolver behaviour.

Rollback removes use of the response profile for future strokes; no historical result, course geometry, player state, account, or save needs destructive conversion.

## Confirmed implementation inputs

- D4's one-ordinary-playable-surface-transition policy with a second/non-playable-boundary clamp is approved.
- D6's future-stroke compatibility policy is approved: existing careers adopt the new physics without a persisted response version; completed results remain untouched.
- Calibration SHALL use representative existing courses and driver fixtures. This change SHALL NOT expand or replace course generation.

## Manual acceptance record — 9 October 2026

Manual gameplay feedback is positive overall. The condition-sensitive landing and release mechanism appears to
work well, and observed gameplay is acceptable for this milestone. This is deliberately an overall acceptance
record: it does not claim manual verification of individual firmness, club-family, boundary, marker, or next-shot
origin scenarios that were not specifically attested.

## Deferred numerical calibration (non-blocking)

The current model is accepted as a first implementation, while absolute rollout distances remain deferred for
later calibration. In particular, future work should assess DRIVER and other long-club release, differing landing
firmness and surface conditions, and club-dependent response. Any revised constants must recheck scoring
distributions and hazard-adjacent outcomes: longer release could lower scoring or carry balls closer to clamps,
while shorter release could reduce the intended strategic distinction. This is a future calibration task, not a
new requirement or acceptance blocker for this feature.
