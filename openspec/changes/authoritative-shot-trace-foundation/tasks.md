# Tasks

## 1. Pure trace contract and resolution seam

- [x] 1.1 Add immutable `ShotTrace` and `ShotTraceTransition` using existing spatial, club, contact, and recovery types; document all point semantics.
- [x] 1.2 Assemble traces only from existing pre-shot context, canonical contact, and `ShotSettlement`; do not add or duplicate settlement rules.
- [x] 1.3 Add an explicit observational materialization mode that preserves the single resolver/settlement path and defaults bulk callers to summary-only.
- [x] 1.4 Attach an optional trace to `ShotOutcome` without adding speculative trajectory fields or changing outcome scoring semantics.
- [x] 1.5 Add deterministic trace-on/trace-off equivalence tests covering carry, lateral, contact, settlement, penalties, putting, and score.

## 2. Interactive and simulation integration

- [x] 2.1 Materialize traces for human `playShot`, visible `simShot`, and future observable AI seams.
- [x] 2.2 Keep `simHole`, `simRound`, `simEvent`, background tournaments, and calibration corpora summary-only by default.
- [x] 2.3 Add normal, bunker/rough/fairway, water drop, water fallback, OB replay, and hole-out trace tests.
- [x] 2.4 Add deterministic human/visible-AI parity tests and allocation-policy tests proving background results do not materialize traces.

## 3. GraphQL contract

- [x] 3.1 Add nullable `ShotOutcome.trace` schema, DTO, mapper, and typed operations using existing position/contact projections where possible.
- [x] 3.2 Return trace data from observable `playShot` and `simShot`; retain settlement and existing outcome fields unchanged.
- [x] 3.3 Add API tests for normal/penalty/hole-out projection, authenticated ownership, null trace where legitimate, and stale rejection with no outcome or trace.

## 4. Canonical playback and feedback

- [x] 4.1 Replace live playback dependence on current guidance/client-derived placement with trace-origin, aim, contact, transition, and final-point data.
- [x] 4.2 Implement display-only origin-to-contact interpolation, trace target/contact/final markers, optional miss vector, and distinct labelled recovery/replay treatment.
- [x] 4.3 Keep timing/easing frontend-owned; implement reduced-motion feedback that exposes every semantic point and transition without animation.
- [x] 4.4 Add focused transform/component tests for trace-driven marker lifecycle, transition rendering, reduced motion, and no client-side contact/aim authority.

## 5. Retire synthetic flight and verify

- [x] 5.1 Verify every active `PlayingHole` uses canonical geometry and trace playback covers normal, water, OB, recovery, hole-out, and reduced-motion states.
- [x] 5.2 Remove or isolate production synthetic flight profiles, strategy/club flight mappings, carry-based club inference, synthetic bounce/spin/roll, and client-generated outcome placement after retirement gates pass.
- [x] 5.3 Add production-import/regression checks preventing synthetic-flight authority from returning to canonical play.
- [x] 5.4 Run backend architecture/persistence and calibration suites plus frontend codegen, typecheck, lint, and relevant automated tests.
- [x] 5.5 Perform the user-owned manual E2E for trace feedback.
  - Manual E2E: PASS.
  - Normal-shot trace behaviour appeared coherent.
  - Intended target, contact, settlement/final position, and recovery semantics behaved acceptably.
  - No blocking ShotTrace-specific regression was found.
  - Fine visual assessment is somewhat difficult in the current UI/UX; those explicitly deferred issues are not part of this feature.

## Explicit exclusions

- [x] 6.1 Do not implement curved flight, directional wind, apex, spin, bounce, rollout, terrain-flight intersection, forced carries, tree collision, shot shape, trajectory controls, short-game overhaul, spatial putting, UI redesign, persistence, or historical replay in this change.
