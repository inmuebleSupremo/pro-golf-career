## 1. Spatial domain and generated geometry

- [x] 1.1 Add pure finite-yard `Position2d`, canonical hole frame, closed-polygon terrain regions, and `CourseGeometry` value types under `sim.*`, without framework or SVG dependencies. Generator helpers MAY use centrelines transiently, but no path/corridor encoding is persisted or exposed by the MVP.
- [x] 1.2 Implement deterministic region validation, fixed surface precedence, `surfaceAt(position)`, and boundary/overlap diagnostics.
- [x] 1.3 Evolve `GeneratedHole`/`CourseGenerator` to derive immutable canonical geometry from the existing seed hierarchy and generator version.
- [x] 1.4 Add deterministic geometry-generation tests for all course classifications, valid regions, surface precedence, and seed/version reproducibility.

## 2. Ball state and compatibility resolution

- [x] 2.1 Add immutable `BallState`, `ShotContact`, and `ShotSettlement` values that distinguish contact, penalty, recovery, and next-shot position.
- [x] 2.2 Extend the shared shot context/outcome and round-loop seams with a canonical spatial path while retaining a bounded legacy `HoleModel`/`ShotZoneProfile` adapter for existing fixtures.
- [x] 2.3 Convert generated production course play to resolve sampled carry/lateral offsets from the stored ball position against `CourseGeometry.surfaceAt`.
- [x] 2.4 Implement deterministic water-drop and out-of-bounds settlement from the new values while preserving present penalty counts and shared human/AI behaviour: search tee-ward from the water contact after the existing 15-yard setback for the first legal `PRIMARY_ROUGH` point that does not increase cup distance; if no such point exists, restore the pre-shot state as stroke-and-distance and expose that fallback in the settlement.
- [x] 2.5 Add unit and equivalence tests for normal settlement, water contact/drop, out-of-bounds replay, next-shot origin, and automatic versus interactive resolution.

## 3. Calibration and migration safety

- [x] 3.1 Compare canonical-path aggregate fairway, GIR, scoring, putts, and strategy distributions against the existing regression harnesses; tune generated geometry only as needed to remain within agreed tolerances.
- [x] 3.2 Audit `HoleZones` and zone-profile callers. Canonical geometry is the production terrain authority; retain the bounded sampler/reachable-preview compatibility read until its remaining callers can be removed under the documented retirement gates. Do not label it fixture-only or remove it prematurely.
- [x] 3.3 Confirm seed/version-derived geometry is reproducible across restore inputs and document that current saves reject a pending playable event, so they contain no ball state to migrate.
- [x] 3.4 Record and test the current persistence boundary: snapshots reject pending playable events. A future change that permits such a snapshot must include complete `BallState` and unresolved settlement state; this change does not add mid-event saves.

## 4. Application and GraphQL contract

- [x] 4.1 Define the MVP application DTOs and schema fields: `PlayingHole.geometry { tee, cup, playableBoundary, regions { surface, boundary } }`, `PlayingHole.ball`, and `ShotOutcome.settlement { contact, recoveryPosition, recoveryKind, ball }`, with no engine-type leakage. Preserve existing coarse `PlayingHole` fields only as deprecated/overview data during migration; they SHALL not drive terrain rendering.
- [x] 4.2 Map the pending playable event's canonical geometry and spatial state through `WorldService`/`ApiMapper` and add ownership/off-event/error coverage.
- [x] 4.3 Regenerate frontend GraphQL types and add API contract tests proving DTO regions and positions faithfully represent engine geometry.

## 5. Canonical rendering adoption

- [x] 5.1 Refactor the play-layer geometry projection to transform canonical DTO coordinates into SVG space rather than generate gameplay landforms from a local seed.
- [x] 5.2 Preserve biome styling, foliage, texture, lighting, and motion as non-gameplay decoration; remove or quarantine any decorative terrain that could be mistaken for a gameplay region.
- [x] 5.3 Animate contact and recovery/next-shot state distinctly, including water drops and out-of-bounds replay origins.
- [x] 5.4 Add frontend unit/integration coverage for canonical terrain rendering and record the supplied manual E2E evidence for terrain/ball/recovery coherence.

## 6. Verification and handoff

- [x] 6.1 Run the full backend test suite, frontend typecheck/lint/build, GraphQL code generation, and strict OpenSpec validation.
- [x] 6.2 Record calibration results, legacy-adapter status, manual E2E evidence, and deferred Open Questions before proposing the next shot-model change.
