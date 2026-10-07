# Tasks

## 1. Intent, eligibility, and profile foundation

- [x] 1.1 Add immutable non-null `ShotFamily`, evolve engine `BallStrikeIntent`, and make the existing non-spatial putting route explicit as minimal `PuttIntent`, retaining API-boundary compatibility defaulting only where required.
- [x] 1.2 Add pure `ShotFamilyEligibility` for starting lie, individual club, and family; classify invalid technique separately from legal-but-poor choices.
- [x] 1.3 Encode the compact club-family matrix and authoritative green, bunker, trees, recovery, deep-rough, and ordinary-lie rules.
- [x] 1.4 Add a pure derived `ShotExecutionProfile` combining actual lie, `ClubSpec`, family, existing attributes, and `Environment` without exposing execution knobs.
- [x] 1.5 Make human and AI intent the sole input to the compatibility decision adapter; remove parallel AI legacy-decision construction where it can diverge.

## 2. Resolver and settlement semantics

- [x] 2.1 Preserve FULL baseline behavior except documented lie-aware eligibility/penalties; calibrate CONTROLLED as a reach-for-control trade-off.
- [x] 2.2 Implement bounded PITCH, CHIP, and BUNKER execution profiles using existing skill mappings and individual clubs.
- [x] 2.3 Add one bounded authoritative post-contact ground-response endpoint for eligible playable contact; never roll from water/OB and keep bunker response zero/negligible.
- [x] 2.4 Apply the conservative unsupported-boundary rule: no chain/traversal; candidate leaving its supported contact surface settles at contact.
- [x] 2.5 Clarify and migrate contact surface versus final settled surface throughout `ShotOutcome`, `ShotSettlement`, round progression, and downstream statistics.
- [x] 2.6 Preserve deterministic trace-on/trace-off equivalence, scoring behavior for equivalent execution, and summary-only bulk trace allocation.

## 3. Trace, putting, and API contract

- [x] 3.1 Add an explicit optional authoritative trace roll phase, separate from `ShotTraceTransition` recovery/replay.
- [x] 3.2 Define and test valid trace ordering for normal, pitch, chip, bunker, water, and out-of-bounds outcomes.
- [x] 3.3 Retire automatic fringe-within-ten-yards putting; preserve green `PuttIntent` / `playPutt` routing and enable deliberate fringe choices without spatial-putting changes.
- [x] 3.4 Add additive GraphQL `shotFamily`, dedicated `playPutt`, eligibility/guidance, roll facts, and final/contact surface semantics; stale or invalid submissions produce no resolved trace.
- [x] 3.5 Regenerate typed frontend operations and cover API compatibility for omitted family, current saves, and legacy/historical nullability where applicable.

## 4. Guidance, UI, and visible playback

- [x] 4.1 Extend server-derived `ShotGuidance` with per-club family availability and concise disabled reasons.
- [x] 4.2 Add the minimal Club → Technique → Landing target control flow without redesigning the play screen or exposing internal execution values.
- [x] 4.3 Label roll-capable AimPoint selection as a landing target; do not add final-position prediction.
- [x] 4.4 Render only backend-authored trace roll endpoints, visually distinct from recovery/replay and reduced-motion-safe.
- [x] 4.5 Add focused frontend tests for roll/recovery distinction and no client-generated rollout; UI behaviour remains covered by type/lint/build checks and manual E2E.

## 5. AI, statistics, calibration, and compatibility

- [x] 5.1 Add deterministic minimal AI family policy: FULL default, CONTROLLED lay-up, short-game pitch/chip, and bunker extraction.
- [x] 5.2 Verify human/visible-AI intent and result parity; retain background simulation practicality and summary-only trace materialization.
- [x] 5.3 Add tests proving family/club/lie eligibility, backend rejection, FULL-versus-CONTROLLED distinction, PITCH-versus-CHIP rollout distinction, and BUNKER behavior; assert no selectable family is a mechanical alias in its intended context.
- [x] 5.4 Run fixed existing scoring, strategy, terrain, penalty, persistence, and generator-version corpora; family policy and distinct mechanics are covered by focused deterministic tests. Product telemetry for runtime family-rate reporting remains out of scope.
- [x] 5.5 Run architecture, persistence/save compatibility, historical generator/version, scoring, API, frontend codegen/type/lint/test, and manual E2E verification.

## Completion evidence

- Manual E2E: **PASS** (user-provided, 2026-10-07).
- FULL / CONTROLLED felt broadly correct; PITCH / CHIP were mechanically distinct enough for this slice; and BUNKER behaviour was acceptable.
- Fringe putting versus short-game choice and dedicated green putting worked acceptably.
- Landing-target semantics and post-contact rollout appeared coherent.
- No blocking feature-specific defect was found. Existing UI/UX limitations remain deferred.

## Explicit exclusions

- [x] 6.1 Do not implement bump-and-run, flop, punch, stinger, shot shape, directional wind, apex, full trajectory, spin, bounce, slope, firmness, arbitrary ground traversal, forced carries, tree collision, spatial putting, green contours, equipment progression redesign, save-format redesign, or visual redesign.
