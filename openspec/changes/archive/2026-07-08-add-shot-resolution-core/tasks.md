## 1. Backend module scaffold

- [x] 1.1 Create the Maven backend module (Java 21, Spring Boot 3 parent) under `/backend` with a `com.progolf.sim` base package; no web/JPA starters required by the core.
- [x] 1.2 Create framework-free domain packages: `sim.core` (numerical model + RNG), `sim.spatial` (zone bands + surfaces), `sim.shot` (resolution). Add JUnit 5 test scaffolding.
- [x] 1.3 Add an `architecture` test (or package rule) asserting the core packages import no Spring/JPA/I/O types, to protect purity (design D1).

## 2. Numerical model

- [x] 2.1 Define the `ValueCategory` enum (Attribute, Modifier, Rating, State, Outcome) and the nine `Attribute` identities on the 0–100 scale.
- [x] 2.2 Implement attribute bounds validation (reject/clamp outside 0–100; permit transient internal overflow) per the Attribute Scale and Bounds requirement.
- [x] 2.3 Implement the fixed 7-step `CalculationPipeline` (Base → Career Effects → Temporary Modifiers → Environmental Effects → Controlled Randomness → Safety-Net → Outcome) as composable, ordered stages operating on an immutable working context.
- [x] 2.4 Enforce mathematical conventions (higher attribute ⇒ ≥ expected outcome; positive modifier ⇒ ≥ expected outcome) as invariants exercised by tests.
- [x] 2.5 Enforce "development targets only Attributes" and "Outcomes not reused as raw inputs" as guarded operations.

## 3. Deterministic RNG

- [x] 3.1 Implement a documented, version-pinned splittable generator (JDK `SplittableGenerator` or an explicit SplitMix64) behind an `Rng` abstraction.
- [x] 3.2 Implement coordinate-addressed seed derivation `deriveSeed(parentSeed, scopeId)` as a pure hash/mix (not sequential splitting) and the `SeedCoordinate` type (world, season, tournament, round, golfer, hole, shot).
- [x] 3.3 Provide `Rng` factory that yields a generator for any coordinate directly from the world master seed (no traversal state).
- [x] 3.4 Tests: same coordinate ⇒ same seed; order independence; sibling isolation; cross-process golden-value stability.

## 4. Zone-band spatial model

- [x] 4.1 Define the `Surface` catalogue enum (13 V1 surfaces) with per-surface playability/penalty/recovery metadata.
- [x] 4.2 Define `ZoneBand` (distance interval + ordered lateral sub-regions of surface+weight) and `ShotZoneProfile` (the ordered band set for a shot context).
- [x] 4.3 Implement partition validation: bands cover the reachable range with no gaps/unresolved overlaps; reject unknown surfaces.
- [x] 4.4 Implement `surfaceAt(carryDistance, lateralOffset)` resolving distance band → lateral sub-region → surface, and nothing else.
- [x] 4.5 Tests: full-partition coverage, surface-solely-from-bands, across-line lateral surface selection.

## 5. Shot resolution core

- [x] 5.1 Define immutable value types: `ShotDecision` (club, target, strategy), `ShotContext` (golfer attributes/state, environment, zone profile, seed coordinate), and `ShotOutcome` (final surface, distance remaining, lateral result, hazard, penalties, shot count, factor breakdown).
- [x] 5.2 Implement the per-shot core: shape mean via pipeline steps 1–4, sample carry + lateral from Normal distributions whose σ derives from accuracy/control attributes, modifiers, and strategy (design D4).
- [x] 5.3 Implement the rare-extreme tail/mixture for mishits and hero recoveries, keeping extremes rare (Continuous Controlled Randomness requirement).
- [x] 5.4 Implement safety-net bounding (dampen unrealistic/catastrophic samples post-sampling without flooring poor outcomes).
- [x] 5.5 Implement strategy shaping (Conservative/Balanced/Aggressive adjusts reward + dispersion without altering attribute contribution).
- [x] 5.6 Populate the explainability factor breakdown (attribute, environment, strategy, luck delta) on every outcome.
- [x] 5.7 Reject incomplete decisions (missing club/target/strategy) before producing any outcome.

## 6. Entry points

- [x] 6.1 Implement `resolveShot(ShotContext) -> ShotOutcome` using the per-shot core and the coordinate-seeded `Rng`.
- [x] 6.2 Implement `resolveRound(golfer, hole/round context) -> RoundOutcome` that loops the identical per-shot core, deriving decisions from a simple strategy policy and seeding each shot at the same coordinate `resolveShot` would use (design D6).
- [x] 6.3 Confirm no control-type input exists in the model path (shared-model requirement) via a code/test check.

## 7. Verification

- [x] 7.1 Statistical tests over large N: central tendency dominates; higher attribute ⇒ better mean; lower attribute still produces occasional exceptional results.
- [x] 7.2 Monotonicity tests: single-attribute increase never worsens expected outcome; negative modifier never improves it; Aggressive widens dispersion.
- [x] 7.3 Distribution-equivalence property test: same seeded hole via `resolveShot` sequence vs `resolveRound` yields identical outcomes.
- [x] 7.4 Reproducibility tests: re-resolving a shot is stable; save/load-style re-run of pending events yields identical outcomes.
- [x] 7.5 Centralize all realism constants in one configuration surface and document them for the later calibration pass.
- [x] 7.6 Run `openspec validate --change add-shot-resolution-core --strict` and resolve any findings.
