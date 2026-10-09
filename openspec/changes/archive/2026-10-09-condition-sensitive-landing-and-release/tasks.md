## 1. Decisions and response contract

- [x] 1.1 Approve the one-ordinary-surface-transition/clamp policy and the legacy-career future-stroke compatibility policy; use existing representative course/driver fixtures.
- [x] 1.2 Trace the current weather-to-hole-to-shot construction paths and add a pure resolver-facing firmness input with documented neutral compatibility defaults.
- [x] 1.3 Define an immutable internal ground-response profile from club/category, shot family, playable contact surface, lie, and firmness; document chosen constants only after calibration evidence.
- [x] 1.4 Preserve the dedicated putt and BUNKER branches; verify that green speed is not introduced as a non-putting release modifier.

## 2. Authoritative settlement

- [x] 2.1 Derive first contact only from `FlightSolution.positionAt(1)` and derive a finite terminal ground direction from the same continuous evaluator.
- [x] 2.2 Resolve deterministic desired release for FULL, CONTROLLED, PITCH, and CHIP on eligible canonical surfaces, retaining family/club ordering and existing short-game distinction.
- [x] 2.3 Implement the approved bounded boundary policy against `CourseGeometry`, including one ordinary playable transition and deterministic clamping before a second/non-playable boundary.
- [x] 2.4 Keep hazard/recovery settlement unchanged for airborne contact and prohibit roll-created water/OB penalties in this milestone.
- [x] 2.5 Produce the final `BallState`, settlement, and optional trace roll from the single resolver result; summary-only execution must not allocate observable trace data.

## 3. Shared play and presentation

- [x] 3.1 Route interactive human, visible simulated/AI, round, event, and background paths through the same response calculation without changing AI policy privileges.
- [x] 3.2 Confirm the existing trace roll/final-point contract expresses the approved response; no API/DTO/schema or generated-client change is needed.
- [x] 3.3 Confirm canonical playback consumes backend-authored contact, rolling, and final state without client physics; no client implementation change is needed.
- [x] 3.4 Confirm existing reproducible resolver fixtures make firm-versus-soft acceptance legible; no development fixture is needed.

## 4. Determinism, compatibility, and calibration

- [x] 4.1 Add paired fixed-seed driver firm-versus-soft fairway scenarios that prove equal airborne contact inputs and materially ordered final release.
- [x] 4.2 Add paired long/mid-iron and higher-lofted green-approach scenarios that prove family/club ordering and firmness sensitivity without asserting uncalibrated physical constants.
- [x] 4.3 Add PITCH and CHIP surface/firmness scenarios that preserve their distinct bounded short-game releases.
- [x] 4.4 Add boundary fixtures for same-surface release, one ordinary playable transition, second-boundary clamp, and non-playable-boundary clamp; prove no roll-created penalty/recovery.
- [x] 4.5 Prove repeatability, trace-versus-summary equality, human/AI equivalence, no extra RNG consumption, and historical save loading under the approved compatibility policy.
- [x] 4.6 Run and record the existing driving-distance, proximity/GIR, scoring, strategy, weather, tournament, and world calibration corpus; review changed distributions before accepting constants.

## 5. Acceptance

- [ ] 5.1 Run architecture-purity, focused resolver/geometry/save/API tests, frontend codegen/typecheck/lint/build if changed, and the established full regression suite while reporting the known WaterDrop baseline separately.
- [ ] 5.2 Perform a short manual E2E using reproducible firm and soft scenarios: driver release, a higher-lofted approach, PITCH/CHIP, and a visible ordinary-surface boundary result; verify shown contact/final markers and next-shot origin equal backend state.

### Manual acceptance record — 9 October 2026

Manual gameplay feedback is positive overall: the condition-sensitive ground-response mechanism appears to work
well and observed gameplay is acceptable for this milestone. This records overall gameplay acceptance only; it
does not attest that each of task 5.2's enumerated firm/soft, club, boundary, marker, or next-shot scenarios was
manually verified. Task 5.2 remains open for any later scenario-specific E2E evidence.

## 6. Deferred post-milestone calibration (non-blocking)

- [ ] 6.1 Recalibrate absolute rollout distance after broader play data, with particular attention to DRIVER and
  other long-club release, landing firmness/surface conditions, and club-dependent response. Recheck scoring
  distributions and hazard-adjacent outcomes before changing constants. This is a future tuning task, not an
  acceptance blocker for the current implementation.
