## 1. V4 versioning and semantic planning model

- [x] 1.1 Add retained V4 generator selection/current-version policy while preserving exact V1/V2/V3 fixtures, pinned restore behavior, and explicit rejection of unknown versions.
- [x] 1.2 Add pure immutable V4 hazard-planning records/enums for existing hazard surfaces, roles, meaningful anchors, relative sides, bounded severity, and compiler envelope data; retain them beside V4 generated holes rather than in `CourseGeometry`.
- [x] 1.3 Plan deterministic V4 hazard features from route, landing zones, dogleg corner, green plan/surround roles, brief, profile, biome, and seed; separate role selection from frequency and severity.
- [x] 1.4 Validate semantic feature provenance, compatible surface/role combinations, anchor/side interpretation, bounded budgets, and deterministic repeatability before terrain compilation.

## 2. Canonical V4 hazard compilation

- [x] 2.1 Compile route/landing-zone bunker features that challenge bounded landing-envelope edges, turns, or bailouts without occupying primary/safe cores.
- [x] 2.2 Compile green-guard bunker and permitted protected-side water features from `GreenComplexPlan`, preserving open entry, run-up, bailout, and recovery sectors.
- [x] 2.3 Compile only endpoint-truthful lateral/route-side/risk-boundary water; explicitly exclude forced-carry and crossing-water claims until flight-path mechanics exist.
- [x] 2.4 Compile tree and optional recovery-area features solely as recovery-surface consequences, without obstruction mechanics.
- [x] 2.5 Keep polygons simple, bounded, canonical, and non-overlapping among strategic hazards; use existing rotated ellipses plus at most one narrow simple route-aligned polygon helper where justified.

## 3. Feasibility, settlement, and compatibility

- [x] 3.1 Enforce primary/safe usable cores, bounded aggressive exposure, clear route/default-target points, and no complete route blockage.
- [x] 3.2 Enforce green-complex feasibility: protected-side adherence, open-entry/run-up and bailout preservation, at least one recovery sector, and no green-core sealing.
- [x] 3.3 Prove deterministic water relief for representative intended contacts, preserve ball-state/settlement truthfulness, and reject pathological water/OB combinations.
- [x] 3.4 Keep current AI/default-human route targeting compatible without building hazard-management AI, free aim, or new player controls.
- [x] 3.5 Confirm the canonical renderer remains truthful for V4 polygons; add a narrowly scoped precedence-aware draw-order adjustment only if V4-valid geometry requires it.

## 4. Tests, corpus, and calibration

- [x] 4.1 Lock a deterministic V4 corpus covering profile, biome, archetype, route, zone, green, and hazard-role anchors before tuning.
- [x] 4.2 Add model/compiler tests for feature provenance, role/anchor geometry, simple polygons, precedence, zone and green feasibility, water relief, target safety, and V1/V2/V3 isolation.
- [x] 4.3 Add fixed V4 diagnostics for bunker/water/tree/recovery contacts, penalties, relief fallbacks, role distribution, safe/aggressive outcome differential, route completion, and scoring envelopes.
- [x] 4.4 Add normalized within-course repeated-pattern checks and classify failures as historical regression, V4 pathology, or V4 calibration miss.
- [x] 4.5 Run backend and applicable frontend verification; record environment-blocked checks separately from product failures.

## 5. Integration review

- [x] 5.1 Confirm no GraphQL hazard-role API, strategy overlay, free-aim UI, or SVG redesign was introduced; canonical geometry remains the renderer contract.
- [x] 5.2 Perform the established V4 manual gameplay smoke test: positional landing guard, risk/reward safe/aggressive distinction, greenside protection/bailout, water recovery, and V1/V2/V3 save restoration.
- [x] 5.3 Run strict OpenSpec validation and resolve every finding before implementation review.

Manual E2E: PASS. All V4-specific requirements were checked successfully with no blocking role-based-hazard-generation regression. Minor issues observed during broader play are unrelated and explicitly deferred.
