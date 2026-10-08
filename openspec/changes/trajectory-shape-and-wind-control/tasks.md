# Tasks

## 1. Identity, intent, and compatibility

- [x] 1.1 Add immutable `Handedness` to player identity and all required generator/creation paths; add nullable creation input/default-to-RIGHT and resolved profile projection, limited to shot-shape interpretation.
- [x] 1.2 Add backward-compatible snapshot serialization/rehydration defaults for missing handedness; verify existing saves load as `RIGHT` without unrelated player changes.
- [x] 1.3 Add non-null `ShotShape` to `BallStrikeIntent`, retaining API/fixture compatibility defaulting to `STRAIGHT` only at defined adapters.
- [x] 1.4 Add pure family/club/lie shape eligibility and guidance: FULL/CONTROLLED permit all initial shapes; PITCH/CHIP/BUNKER permit STRAIGHT only; putts remain outside ball-strike shape semantics.

## 2. Wind and pure flight foundation

- [x] 2.1 Evolve resolver-facing `Environment` to carry signed canonical local wind flow and preserve lie quality; retain a documented deterministic weather-to-local conversion compatible with existing weather generation.
- [x] 2.2 Separate and name ordinary execution dispersion, deterministic signed wind displacement, and wind-related uncertainty; retire duplicate use of the unsigned legacy crosswind path.
- [x] 2.3 Add pure parametric `FlightSolution`/profile types using resolver-derived height and the actual aim-relative frame; do not add player-controlled height, spin, or collision.
- [x] 2.4 Make the resolver derive one authoritative first-contact position exclusively from `FlightSolution.positionAt(1)`, then retain existing canonical surface lookup, roll, and settlement semantics.
- [x] 2.5 Calibrate the smallest shape-execution tradeoff using existing club control, attributes, dispersion, and workability. Record the selected behavior/constants and prove it is bounded, explainable, and not universally dominant.
- [x] 2.6 Correct resolver shape-side mapping for both handedness values using the observable bulge/return contract, not UI label swapping.

## 3. Trace and resolver integration

- [x] 3.1 Add an optional authoritative airborne trace path with ordered `(progress, canonical position, height)` samples; include exact origin/contact endpoints and no separate contact calculation.
- [x] 3.2 Keep trace materialization observational: visible human/AI/sim shots materialize samples; `simHole`, `simRound`, `simEvent`, tournament field resolution, and calibration corpora remain summary-only.
- [x] 3.3 Preserve trace ordering and semantics: airborne path → optional authoritative roll → optional recovery/replay transition → final settled point; water/OB gets no roll.
- [x] 3.4 Ensure PITCH, CHIP, and BUNKER return authoritative straight airborne paths when observable while remaining shape-ineligible; putts expose no airborne path.

## 4. Shared play, API, and frontend playback

- [x] 4.1 Extend human and AI adapters/policy so both submit the same handedness-aware shape intent and resolve through the shared flight seam.
- [x] 4.2 Add additive GraphQL input, trace DTO/schema fields, and server-derived shape availability; regenerate frontend GraphQL types.
- [x] 4.3 Add a minimal shape selector to the existing Club → Technique → Landing-target controls, with server-derived disabled states and no curve/height/wind execution control.
- [x] 4.4 Replace visible straight-chord flight interpolation with trace-sample playback when available; retain backend contact, roll, recovery, final-point, timing, and reduced-motion authority boundaries.
- [x] 4.5 Add API/frontend mapping and focused playback tests proving the browser does not derive trajectory, contact, curve, drift, or final placement.
- [x] 4.6 Restore canvas AimPoint interaction after non-holing playback completes while preserving result feedback and settled state.
- [x] 4.7 Sequence authoritative roll playback as airborne → contact → roll → final without altering rollout mechanics.
- [x] 4.8 Project and render read-only effective canonical wind flow as a small arrow/strength indicator without client flight prediction.
- [x] 4.9 Refine authoritative positive-roll presentation: expose visible `Rolling` status, perceptible short-release
  feedback, and distinct contact/final settlement without changing resolver rollout endpoints, distances, or recovery
  semantics.
- [x] 4.10 Correct the wind readout presentation with a symmetric projected arrowhead, visible `Wind toward` plus
  effective unit/strength, and a development-only fixed-fixture component preview that cannot affect live authority.

## 5. Determinism, calibration, and verification

- [x] 5.1 Add fixed-seed calm STRAIGHT golden tests for carry, lateral displacement, authoritative contact, settlement/final ball, score, and RNG consumption; add corpus guards for calm carry, distance dispersion, lateral dispersion, and scoring distributions.
- [x] 5.2 Add unit tests over arbitrary non-degenerate aim axes and both handedness values for FADE/DRAW bulge-and-return direction, plus family eligibility, finite/monotonic samples, zero endpoint heights, `path.last.position == contact.position`, and ideal zero-error/zero-wind in-effective-reach STRAIGHT/FADE/DRAW AimPoint return semantics while permitting execution/wind/reach-limit misses.
- [x] 5.3 Add directional-wind tests for head/tail carry direction, signed crosswind drift, aim-axis re-decomposition, wind uncertainty independence, workability resistance, and putt immunity.
- [x] 5.4 Add parity tests for equivalent human/AI intents and summary-visible equivalence; add allocation/performance coverage appropriate to the existing test harness.
- [x] 5.5 Run architecture-purity, save/load, API/GraphQL, deterministic tournament, scoring/strategy/attribute/hazard/weather calibration, frontend codegen/typecheck/lint/build, and manual E2E.
- [x] 5.6 Add frontend interaction/phase tests for retargeting after playback, roll/no-roll/recovery sequencing, and canonical wind-vector direction/magnitude projection; add trace tests confirming no configured FULL/CONTROLLED/BUNKER roll and bounded PITCH/CHIP roll.
- [x] 5.7 Add deterministic frontend presentation tests using authoritative `0.75`-yard PITCH and `2.55`-yard CHIP
  roll fixtures for phase timing, visible `Rolling`, endpoint/final feedback, no premature final ball, no-roll, and
  recovery behavior; test symmetric wind-arrow tips for canonical `+X`, `-X`, `+Y`, and `-Y`, visible label/unit,
  and the isolated development preview.

## External integration blocker (out of scope for this change)

- [ ] B.1 Before integrating this feature, create and complete a separate effective-cup-placement OpenSpec change in
  an isolated worktree. It must require setup-specific GREEN cup classification, minimum playable green-edge
  clearance, geometry-aware pin placement, preserved front/back and tucked-pin difficulty, deterministic retained
  generator/save compatibility, actual-tournament-setup corpus regressions, practical hole completion, and scoring /
  tournament calibration. It SHALL NOT be implemented as a trajectory/wind workaround or by changing the established
  putting architecture in this change.

## Explicit exclusions

- [x] 6.1 Do not implement flight-terrain intersection, tree/water interception, forced carries, bounce, spin, slope, aerodynamic spin response, multi-surface ground traversal, richer rollout, trajectory-height controls, cut/hook variants, spatial putting, green contours, or visual redesign.
- [x] 6.2 Do not use sampled trace points as collision authority. A future `flight-terrain-response` change must consume the parametric `FlightSolution` evaluator.
