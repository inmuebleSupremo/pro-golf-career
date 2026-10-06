## 1. Versioning and semantic model

- [x] 1.1 Add retained V3 generator selection/current-version policy while proving V1 and V2 fixtures remain byte-for-byte unchanged and unknown versions fail explicitly.
- [x] 1.2 Define pure immutable V3 records/enums for `HoleRoute`, route coordinates/projection, `LandingZone`, `ReferenceCarryBand`, preferred approach side, `GreenComplexPlan`, green-surround roles, and progression targets; keep them associated with V3 generated holes rather than `CourseGeometry`.
- [x] 1.3 Extend deterministic V3 hole planning to map each existing `HoleBrief`/profile/biome combination to constrained route, zone, green, and surround semantics.
- [x] 1.4 Implement semantic validation and bounded deterministic candidate selection for route connectivity/progression/turns/intersections, zone ordering/reachability, and approach-corridor feasibility.

## 2. Canonical V3 compilation

- [x] 2.1 Implement the route-relative fairway/playable-boundary compiler for straight and one-dogleg routes, including bounded width, pinch, widen, bailout, and final-approach behavior without branch networks.
- [x] 2.2 Compile green-complex plans into valid rotated convex green/fringe geometry with orientation, approach, open/protected/bailout side, and optional run-up semantics.
- [x] 2.3 Adapt existing bunker/water/tree placement only enough to respect route zones, final approach, and green sides; do not introduce role-based hazards or new surfaces.
- [x] 2.4 Preserve canonical geometry validation, surface precedence, effective event setup behavior, renderer inputs, and resolver settlement invariants for V3 output.

## 3. Compatibility and persistence

- [x] 3.1 Provide V3 route-progression targets to existing AI/default target selection; strategy chooses only a safe/aggressive recommendation where supplied, with deterministic legacy fallback.
- [x] 3.2 Route human/default play through the same sensible progress target without adding free aiming controls or removing retained target-distance/lateral seams.
- [x] 3.3 Confirm world snapshot/save restore regenerates V3 course semantics and canonical geometry from the existing seed/version pin, writes no polygon copy, restores legacy V1/V2 behavior unchanged, and rejects unknown pins.

## 4. Tests and calibration

- [x] 4.1 Lock the V3 fixed seed/classification/profile/archetype corpus and its calibration contract before V3 parameter tuning; preserve the existing exact V1/V2 corpus separately.
- [x] 4.2 Add deterministic/model tests for straight and one-dogleg routes, validation failures, archetype mappings, landing-zone relationships, green orientation/surround semantics, and version isolation.
- [x] 4.3 Add compiler/property-style corpus tests for simple non-self-intersecting geometry, zone reachability, approach corridors, canonical surfaces, and deterministic regeneration.
- [x] 4.4 Add progression-target tests covering AI safe/aggressive selection, human/default routing, and V1/V2 fallback behavior.
- [x] 4.5 Run fixed V3 scoring/playable-round diagnostics against the committed envelope and classify any issue as historical regression, V3 pathology, or calibration miss.
- [x] 4.6 Run `mvn test` (including architecture purity) and record any environment-blocked checks separately from product failures.

## 5. Integration review

- [x] 5.1 Confirm no GraphQL schema/API/UI redesign is required and the existing canonical SVG presentation renders V3 geometry without authoring terrain.
- [x] 5.2 Perform the established manual gameplay smoke test for a V3 world, including a dogleg/route-progress case and a legacy saved-world restoration.
- [x] 5.3 Run `openspec validate strategic-hole-routing-and-green-complexes --type change --strict` and resolve every finding before implementation review.
