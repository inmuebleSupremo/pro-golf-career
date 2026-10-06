## Context

V3 established the deterministic planning pipeline:

`CourseDesignProfile -> CoursePlan -> HoleBrief -> HoleRoute / LandingZones / GreenComplexPlan -> CourseGeometry`

The last compiler step still receives boolean bunker/water/tree decisions and emits mostly seeded shapes. V4 inserts a bounded semantic stage:

`V3 spatial plan -> V4 HazardPlan / HazardFeatures -> canonical hazard polygons -> settlement and rendering`

`CourseGeometry` remains the sole authority for surface lookup, settlement, and SVG rendering. A hazard plan explains intent; it never resolves a shot or replaces a terrain polygon.

## Decisions

### 1. V4 is retained, version-pinned generation

V4 is a new supported generator version, not a reinterpretation of V3. New world creation selects V4 after implementation. V1, V2, and V3 retain their generators and exact historical fixtures. Snapshot restore uses the persisted world pin, missing legacy provenance remains V1, and unknown versions fail explicitly before a usable world is returned.

V4 regenerates permanent courses, plans, hazard features, and canonical geometry from the established seed hierarchy. It does not serialize generated polygons or duplicate hazard-plan JSON in saves.

### 2. Keep the internal hazard model compact and purposeful

The implementation should use pure immutable records/enums equivalent to:

| Concept | V4 responsibility |
| --- | --- |
| `HazardFeature` | One intentional existing-surface feature and its strategic provenance. |
| `surface` | Exactly one of `BUNKER`, `WATER`, `TREES`, or `RECOVERY_AREA`; no new surface values. |
| `role` | `LANDING_GUARD`, `TURN_GUARD`, `GREEN_GUARD`, `BAILOUT_BOUNDARY`, or `RECOVERY_BOUNDARY`. |
| `anchor` | A landing-zone role, route distance, dogleg corner, or approach-relative green side. |
| `side` | Route-relative left/right or approach-relative front/back/left/right when applicable. |
| `severity` | `LIGHT`, `STANDARD`, or `STRONG`, governing bounded intrusion and recovery consequence rather than count alone. |
| envelope | A small compiler-facing offset/span/clearance description, not a polygon or a club/player control. |

Every V4 bunker, water, tree, or recovery-area gameplay polygon must derive from exactly one feature. Terrain may still overlap its underlying rough/fairway layer by documented precedence; strategic hazard features must not overlap one another or cover green/tee/cup cores.

No cosmetic role belongs in V4. A compiler may omit a candidate feature deterministically when feasibility would be violated; it must not replace it with an unexplained random hazard.

### 3. Separate role, frequency, and severity

These inputs remain independent:

- **Role** answers why the feature exists and chooses its anchor/allowed side.
- **Frequency** is a small per-hole/course budget shaped by profile, archetype, and biome plausibility.
- **Severity** changes placement intrusion, envelope clearance, and recovery cost within fixed bounds.

`RecoverySeverity.PENAL` may select stronger or slightly closer features, but does not mean filling each hole with hazards. Biome adjusts the permitted surface mix; it does not replace strategic-role selection.

### 4. Role and surface mappings are deliberately bounded

| Role | Primary V4 surfaces | Intent |
| --- | --- | --- |
| `LANDING_GUARD` | Bunker; lateral water where feasible | Challenge an edge of a primary or aggressive landing envelope. |
| `TURN_GUARD` | Bunker, trees, recovery area | Make a dogleg-side miss meaningfully worse without blocking the route. |
| `GREEN_GUARD` | Bunker; protected-side water where feasible | Reinforce a declared protected green side. |
| `BAILOUT_BOUNDARY` | Bunker, trees, recovery area | Discourage an over-generous miss while preserving the declared bailout core. |
| `RECOVERY_BOUNDARY` | Trees, recovery area | Define a strategically worse miss/recovery side using existing lie mechanics. |

Water is never tagged or presented as a forced carry in V4. A cross-route creek or a feature whose consequence depends on ball-flight intersection is deferred until the shot engine traces authoritative flight against terrain.

### 5. Fairway features protect decisions, not random locations

`LANDING_GUARD` anchors to a primary or aggressive zone and is placed at a bounded edge, never in its usable core. `TURN_GUARD` is legal only when the route has a real dogleg and references its local turn frame. `BAILOUT_BOUNDARY` may discourage an excessively wide route-side miss but must leave its bailout area usable.

For risk/reward holes, the aggressive zone may receive additional bounded exposure while the safe zone remains a viable route. For positional holes, a guard may favor the desired approach side while preserving a spatially inferior alternative. Balanced holes use modest, conventional challenge and are not required to create a forced asymmetric choice.

### 6. Green features must honor GreenComplexPlan

`GREEN_GUARD` uses the final approach vector and the plan's approach-relative sides. Protected-side hazards may occupy the intended protective sector. `OPEN_ENTRY` and a declared run-up opening remain hazard-free approach corridors. `BAILOUT_SIDE` remains free of serious bunker/water obstruction in V4. At least one fringe/rough recovery sector must remain, and bunker/water may not cover the green core or fully seal normal access.

This is the first V4 use of V3 surround semantics as enforceable design input rather than retained labels.

### 7. Water is endpoint-truthful and relief-safe

V4 water may be lateral to a landing envelope, form a risk/reward route-side boundary, or protect a green side. It remains an endpoint/contact penalty: landing in water incurs the established penalty/drop behavior; a ball whose landing point clears a water polygon is not considered to have crossed it.

The generator must preserve deterministic water relief. It validates expected tee-ward probes from representative intended water contacts to a qualifying `PRIMARY_ROUGH` point that remains nearer the cup than the pre-shot point. Features that would normally require stroke-and-distance fallback, combine with OB to block progression, or leave no legal recovery are rejected rather than accepted as dramatic hazards.

### 8. Trees and recovery terrain use only current mechanics

Trees and `RECOVERY_AREA` are difficult playable lies, not obstruction volumes. They can frame an outside dogleg miss, bound a recovery side, or make a bailout edge less attractive. V4 neither models nor claims canopy contact, blocked lines, punch-outs, trunk collision, or mandatory shot shapes.

`RECOVERY_AREA` may become emitted V4 terrain only if its existing recovery/precedence behavior is validated alongside trees. It is not a new surface type.

### 9. Canonical geometry stays modest

Existing rotated ellipses remain the default primitive for bunkers, lateral water, trees, and recovery terrain. V4 may add one deterministic narrow, simple route-aligned polygon helper for elongated lateral water when an ellipse cannot express the intended bounded feature. It shall not introduce splines, terrain meshes, polygon boolean operations, or a generic authoring framework.

The compiler validates every resulting `TerrainRegion` and `CourseGeometry`; same-precedence overlap remains invalid. Strategic hazard polygons are intentionally non-overlapping, which avoids ambiguous visual stacking. The existing canonical renderer can therefore remain unchanged: it already renders arbitrary polygon vertices supplied by the backend. If implementation demonstrates a valid unavoidable overlap, a narrowly scoped precedence-sorted draw order is in scope; otherwise it is explicitly not work for V4.

### 10. Feasibility safeguards precede tuning

Before a V4 hole is emitted, validation must prove:

- all hazard features have valid semantic provenance and allowed anchor/surface/role combinations;
- a usable hazard-free core remains in PRIMARY and SAFE zones;
- AGGRESSIVE exposure is bounded rather than automatically fatal;
- route/default progression target points are not inside bunker, water, trees, or recovery terrain;
- the route and its normal approach corridor remain connected and unblocked;
- green open-entry, run-up, bailout, and at least one recovery sector are retained;
- canonical polygons are simple and terrain precedence remains deterministic;
- representative water contacts have legal relief and settlement stays truthful.

Geometric tests are necessary but insufficient. The fixed shot-resolution corpus must also measure safe/aggressive outcomes, hazard contact rates, recovery behavior, and route completion.

### 11. Current controls receive safety, not a new AI

The existing route-target bridge remains. V4 plans must leave selected safe/primary target cores clear, and may put bounded additional risk around aggressive targets. This prevents existing AI/default-human flow from knowingly aiming at a hazard without building course-management AI or requiring free aim.

Hazard semantics remain available for future Target/Aim reasoning: feature role, associated zone/green side, relative danger direction, surface, severity, and clearance envelope can later inform carry, lay-up, avoidance, corner challenge, bailout, and protected-side decisions.

### 12. Quality evidence is version-specific

V4 receives a fixed deterministic corpus spanning course profile, biome, archetype, route, zone, green, and feature roles. It checks repeatability; feature provenance and distribution; anchor proximity; simple/valid terrain; zone/green/relief feasibility; target safety; repeated normalized patterns within an 18-hole course; playable route completion; and surface contacts.

Calibration records bunker lies, water contacts/penalties, tree/recovery lies, relief fallbacks, penalty rate, recovery frequency, safe/aggressive expected outcome difference, and scoring envelope. It rejects both irrelevant hazards and pathological penalty/recovery/route failures. V1/V2/V3 fixtures remain exact historical assertions; V4 is assessed only against its approved V4 envelope.

## Alternatives Rejected

- **Modify V3 in place:** breaks existing V3 careers and versioned deterministic identity.
- **Place more random hazards:** changes density without making golf decisions intentional.
- **Treat water geometry as a forced carry:** misrepresents an endpoint-only resolver.
- **Build flight, obstruction, or player aiming now:** materially expands shot mechanics and player experience beyond hazard design.
- **Serialize polygons or hazard plans in saves:** duplicates deterministic state and weakens version provenance.
- **Require frontend role overlays:** turns internal design metadata into UI before players can act on it.

## Risks and Mitigations

- **Hazards make target paths unusable.** Validate target-core and route clearance before accepting a plan.
- **Water causes invalid recovery.** Probe relief and reject pathological layouts rather than relying on fallback.
- **Profile severity becomes hazard saturation.** Use budgets and severity/intrusion separately from role frequency.
- **Renderer disagrees with gameplay precedence.** Avoid strategic-hazard overlaps; add only a proven-needed, precedence-aware render ordering adjustment.
- **V4 scoring drifts excessively.** Lock corpus and envelope before parameter tuning; classify failures rather than altering legacy fixtures.
