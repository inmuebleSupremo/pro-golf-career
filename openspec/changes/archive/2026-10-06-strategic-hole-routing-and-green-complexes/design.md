## Context

The course pipeline currently has a useful separation: deterministic V2 `CourseDesignProfile` and `CoursePlan`/`HoleBrief` express venue intent, while `CanonicalGeometryGenerator` turns scalar hole inputs into the canonical polygons used by `CourseGeometry`, rendering, and shot settlement. V2 fairways still follow one shallow fixed curve, greens are axis-aligned ellipses, and full-shot frames point directly at the green centre. This prevents a stronger dogleg from being generated safely: existing shot flows would attempt a direct inside-line cut.

V3 expands only the semantic-to-canonical compiler stage. `CourseGeometry` stays the authoritative terrain representation and `HoleBrief` stays the course-level planning contract. The new records are immutable generator input/output metadata associated with V3 generated holes; they are neither a competitor to `CourseGeometry` nor a player-facing schema.

| Layer | Responsibility | Does not own |
| --- | --- | --- |
| `HoleBrief` / design profile | Course composition and archetype intent | Coordinates or polygons |
| V3 spatial plan | Route, zones, green orientation, surround meanings, progression targets | Surface classification or mutable gameplay terrain |
| Canonical compiler | Valid terrain polygons from scalar + V3 semantic input | Independent strategic rules |
| `CourseGeometry` | Authoritative terrain lookup and SVG geometry | Design semantics |
| Existing shot flow | Current club/strategy decision and sampled settlement | A new player aiming system |

## Decisions

### 1. V3 is a retained generator, not a reinterpretation of V2

V3 is a distinct supported course-generator version. The registry selects a generator only from an explicit version; new-world creation may choose the current version, but restore always selects the stored pin. V1 and V2 retain their existing output and fixtures exactly. A V3 world regenerates all permanent courses from the established seed hierarchy and V3 inputs; unsupported pins fail before a usable world is returned.

The existing world-level pin remains sufficient while a world owns one fixed course pool. The `Course.generatorVersion` integrity stamp must agree with the world pin. V3 semantic records are deterministic generated state associated with V3 courses, not a JSON polygon snapshot; legacy versions have no fabricated V3 plan. A future mixed-version course-pool policy needs a separate approved change.

### 2. Use a simple route that can be measured and compiled deterministically

`HoleRoute` is a polyline in generated-hole coordinates: a tee origin, a green/approach anchor, and zero or one intermediate anchor in V3. Zero anchors represent a straight route; one represents a single dogleg. The intermediate list is structurally explicit so a later version can add another anchor, but V3 generation and validation reject two or more anchors. The route is generated from the semantic brief and deterministic seed—not reverse-engineered from `CourseGeometry`.

All route-aware data uses route distance (arc length from tee) plus signed perpendicular centre offset. This makes a landing zone meaningful around both straight and dogleg segments without encoding a club or a player coordinate. Segment joins have a deterministic tangent/bisector convention so there is no ambiguous local lateral frame.

Before compilation, V3 validates: tee-to-approach connectivity; strictly increasing progress; bounded segment displacement and turn angle; no self-intersection or non-adjacent overlap; valid approach direction; and route length consistent with the brief/par length band. Validation also proves that each required landing zone is reachable in route order and that its preferred approach corridor can lead into the final green entry. Invalid candidate plans are deterministically rejected/re-sampled from a bounded seed-derived attempt sequence; exhaustion is an explicit generator failure, not an ambient fallback.

### 3. Model landing zones as intent, not shot controls

`LandingZone` shall contain `role`, route distance, signed centre offset, depth, half width, preferred approach side, and an abstract `ReferenceCarryBand`. Roles are `PRIMARY`, `SAFE`, `AGGRESSIVE`, with `LAYUP` reserved as optional/future rather than required on every hole. Carry bands are abstract route-reach classes (for example short, standard, long/reachable) and have no club identity, player attributes, UI control, or ball-flight rule.

Zones are route-relative target areas used by planning, feasibility, the compiler, and defaults. They do not declare another fairway, a player-owned target, or a promise that every strategy is equally viable. At least one `PRIMARY` zone is required for a non-short hole; risk/reward holes may have a safe/aggressive pair. Zone reachability is assessed only against the route's own progression contract and abstract carry bands, never by pretending a particular golfer can execute a particular shot.

### 4. Give existing archetypes bounded spatial expressions

V3 preserves the V2 archetype names and maps them to constrained spatial intent:

| Archetype | V3 mapping |
| --- | --- |
| `POSITIONAL` | A modest single dogleg or controlled straight route where permitted; a narrower/controlled primary zone; a safer but spatially inferior zone or line; and a preferred approach side. |
| `BALANCED` | A neutral primary zone and broadly neutral approach relationship, without forced narrowness or manufactured alternate route. |
| `RISK_REWARD` | A safe and an aggressive spatial choice when eligible. The aggressive zone is shorter to the approach and/or creates a more favourable approach geometry; its distinction is not merely a higher hazard probability. |

Profile width/recovery settings and biome biases remain bounded composing inputs. A generous profile cannot become penal merely because a hole is positional; hazards do not become a new role taxonomy in this change.

### 5. Compile a single route-following fairway, not a network

The V3 fairway compiler sweeps a corridor along the route centreline, with deterministic local width changes derived from route distance, `LandingZone` extents, pinch/widen instructions, bailout allowance, and the final approach. It generates a simple, valid playable boundary plus fairway/rough/fringe regions through the existing canonical model. The compiler may widen around a safe zone or tighten a controlled primary zone, but creates no separately navigable fairways or branch network.

The green compiler consumes `GreenComplexPlan`: green-centre offset from the approach anchor, major/minor dimensions, rotation, approach direction, open/protected/bailout side declarations, and an optional run-up opening. It emits a rotated ellipse or other simple convex polygon and validates its containment, non-degeneracy, relationship to the final approach, and valid connection to fairway/fringe. The plan must carry an approach/orientation relation; an arbitrary rotated oval is not enough.

`GreenSurroundRole` is semantic only: `OPEN_ENTRY`, `PROTECTED_SIDE`, `BAILOUT_SIDE`, `SHORT_MISS`, `LONG_MISS`, and `RECOVERY_SIDE`. The initial compiler maps these meanings to existing surface placements/widths where possible; it adds no `Surface` enum values, contours, elevation, putting grid, or spatial putting model.

### 6. Adapt legacy hazards minimally; defer role-based hazards

Existing bunker, water, and tree decisions remain the V2-compatible environmental/recovery inputs. V3 placement may become aware of route position, zone footprint, final approach corridor, and green side/orientation so it does not contradict generated semantics. It must remain deterministic and preserve safe playable recovery where required.

No hazard receives a reusable tactical role, no hazard catalogue grows, and no new elaborate water/bunker construction is promised. That larger mapping belongs only to a later independently specified `role-based-hazard-generation` change.

### 7. Bridge progression targets without replacing control systems

Each V3 hole exposes internal deterministic progression targets derived from the primary/safe/aggressive zones and final approach anchor. Existing AI target selection uses those targets; existing strategy labels may prefer a safe or aggressive recommendation. If a target is absent or a caller is V1/V2, the deterministic current green-centre fallback remains.

Human play, which has no free spatial aiming UI, receives the sensible route-progress target as the default behind the current decision surface. It preserves the V3 semantic data for later Target/Aim work and does not remove existing club, target-distance/lateral compatibility, or strategy seams. The forward-compatible conceptual sequence remains **Club → Target/Aim → Shot Type → Shot Shape/Trajectory → Execution**; only the first/current compatibility portion exists now.

### 8. Treat quality evidence as version-specific

V1/V2 fixtures remain exact: no V3 work may update them to accommodate V3. V3 gets a committed deterministic corpus spanning fixed seed/classification coordinates and profile/archetype anchors. It verifies repeated regeneration, route feasibility, zone ordering/reachability, simple canonical geometry, green/approach relationships, target fallback, safe/aggressive recommendation selection, and existing playable settlement invariants.

V3 scoring uses a fixed representative round corpus and pre-committed aggregate envelopes. It is not required to equal V2 scores exactly, because routes/green orientation intentionally change terrain. Failures are classified as: historical regression (V1/V2 drift), V3 pathology (invalid/non-progressing geometry or unplayable resolution), or V3 calibration miss (outside approved V3 envelope). The calibration fixture/thresholds are locked before V3 tuning; later changes require an explicit calibration change.

## Alternatives Rejected

- **Bézier/fully freeform routes now:** harder to measure, project, validate, and compile deterministically without delivering more initial strategic value than a one-dogleg polyline.
- **Infer semantics from compiled polygons:** reverses ownership, makes versioned regeneration fragile, and cannot represent intended but visually subtle decisions.
- **Make `CourseGeometry` carry route semantics:** mixes gameplay terrain with planning metadata and invites a second authority.
- **Serialize generated polygons or add semantic JSON as a save workaround:** duplicates deterministic state and leaves generator compatibility undefined.
- **Provide player aiming controls now:** a materially different player experience, which requires shot-intent, UI, and physics work beyond this change.
- **Generate double/branching routes now:** adds combinatorial feasibility and AI complexity before basic progression is proven.

## Risks and Mitigations

- **Doglegs cause direct-cut shots.** Progression targets are produced with the route and used by AI/default human flow, with deterministic green-centre fallback for old holes.
- **Polygon sweeps self-intersect near turns.** Route bounds plus compiler geometry validation reject/re-sample before a `CourseGeometry` is returned.
- **Spatial terminology overpromises new mechanics.** Landing zones and surround roles are internal semantic metadata, explicitly separated from clubs, UI, surfaces, and shot physics.
- **V3 drifts scoring.** A locked V3 corpus/evidence contract measures its own intended envelope while historical fixtures stay immutable.

## Delivery Boundaries

Implementation is backend simulation/testing work only. It preserves `sim.*` purity, keeps persistence adapters in `app.*`, runs the full backend suite, and performs the existing focused/manual gameplay smoke test. There is no frontend implementation other than confirming canonical SVG output continues to render the geometry it is given.
