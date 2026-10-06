## 1. Establish the V1 compatibility baseline

- [x] 1.1 Extract the current algorithm as retained V1 behind a pure explicit-version registry/API; do not change its generated output.
- [x] 1.2 Lock the 48-coordinate historical V1 seed/classification corpus (eight per environment classification), complete-course/canonical-geometry golden fixtures, and representative on-disk current-v2-envelope save fixtures.
- [x] 1.3 Capture the V1 composition, terrain-exposure, and existing scoring diagnostics for that corpus; check in immutable baseline data and retain existing V1 strategy/scoring tests unchanged.
- [x] 1.4 Before V2 tuning, commit the reviewed calibration contract: fixed 9-profile scoring-anchor set, 12-coordinate/12-round deterministic scoring sample, profile/biome envelopes, minimum directional separations, and maximum repeated-plan-signature allowance.

## 2. Implement version-pinned regeneration and save compatibility

- [x] 2.1 Add a world-level course-generator version pin; create new worlds through the current version and restore worlds only through their pin.
- [x] 2.2 Extend application save compatibility to write explicit provenance, load valid current v2 saves with missing provenance as V1, and reject unsupported versions before partial restore.
- [x] 2.3 Add disk JSON save/load coverage for explicit V1/V2 pins, missing-version legacy fixtures, unknown pins, and exact historical V1 venue/geometry continuity.

## 3. Introduce compact profiles and hole briefs

- [x] 3.1 Add immutable, framework-free `CourseDesignProfile`, `HoleBrief`, length-band, and three-archetype models with validation and no biome/setup conflation.
- [x] 3.2 Implement bounded, current-generator expressions for positional, balanced, and risk/reward briefs; keep routes, target zones, hazard roles, and green-complex detail explicitly out of scope.
- [x] 3.3 Add deterministic profile/brief tests proving a profile is not cosmetic and is internal-only (no GraphQL/UI addition).

## 4. Introduce deterministic eighteen-hole planning

- [x] 4.1 Add immutable `CoursePlan` generation from the established course-seed hierarchy before hole generation.
- [x] 4.2 Enforce par-72/front-back composition, par-relative length variety, positional/risk-reward opportunities, archetype diversity, and no more than three adjacent identical archetypes.
- [x] 4.3 Test all 27 profile-axis combinations across the fixed 48-coordinate corpus for deterministic plan validity and profile adherence.

## 5. Compile V2 intent through canonical geometry

- [x] 5.1 Implement the V2 brief-to-existing-parameter bridge for bounded length, fairway width, current hazard eligibility, and recovery inputs while retaining biome environmental bias.
- [x] 5.2 Compile V2 courses exclusively through the existing canonical geometry generator and preserve `GeneratedHole`/`HoleModel`, setup-geometry, resolver, and frontend contracts.
- [x] 5.3 Add V2 geometry-validity, surface-classification, settlement/recovery, and setup-specific effective-geometry coverage.

## 6. Measure profile differentiation and generation quality

- [x] 6.1 Run the locked 1,296-course geometry/plan corpus and report ordered width, recovery, length-band, archetype, exposure, and repetition metrics against the calibration contract.
- [x] 6.2 Run the locked V2 scoring anchor corpus with the existing representative human/AI configurations; distinguish intended profile deltas from scoring regressions and pathologies using the contract.
- [x] 6.3 Investigate any failed locked envelope or invariant; do not relax a fixture or existing V1 regression merely to admit V2 results.

## 7. Verify scoring, determinism, and compatibility

- [x] 7.1 Run the full backend suite including architecture purity, V1/V2 deterministic generation, save/load, course setup, geometry, and calibration coverage.
- [x] 7.2 Prove exact V1 regeneration and legacy-save restoration, explicit V2 regeneration, and clear unknown-version rejection.
- [x] 7.3 Update course-generation/persistence documentation and operator notes with registry retention, rollback, and future mixed-version admission rules.

## 8. Prepare manual gameplay E2E

- [x] 8.1 Manually play/simulate a representative V1-restored career and a new V2-profiled world; confirm human/AI round resolution, canonical frontend rendering, and setup-specific terrain behavior.
- [x] 8.2 Record E2E findings separately from automated calibration evidence; do not add profile-selection controls or public API fields in this slice.

Manual E2E completed by the user: **PASS**. Generated V2 courses showed acceptable variation and general gameplay behaviour. No blocking terrain, ball-state, rendering, sequencing, or event-setup regressions were identified. Approved for integration into `develop`.
