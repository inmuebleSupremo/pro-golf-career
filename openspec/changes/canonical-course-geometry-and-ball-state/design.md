## Context

The current pure engine is deterministic and shared by human and AI play. A `GeneratedHole` supplies dimensions and boolean hazards; `HoleZones` synthesizes a symmetric one-dimensional `ShotZoneProfile` for each remaining distance. `ShotResolver` samples carry and signed lateral error, then classifies the result through that profile. Round loops retain only remaining distance and lie.

The already-shipped 2D-hole layer uses the same coarse dimensions only as inputs to a separate seeded SVG generator. It invents doglegs, bunker locations, water placement, carry gaps, and fairway-width variation. Its animation is cosmetic and forced to finish at a screen position chosen from the outcome label. It cannot show the actual legal water drop or stroke-and-distance origin because neither is a spatial game-state value. Its completed OpenSpec change has been archived; its useful biome themes, flyover, viewBox transform, and decorative drawing utilities remain in scope, while its permission to invent gameplay landforms is superseded here.

This change replaces the architectural premise that 2D is presentation-only. It does not add flight physics: the current carry/lateral sampling, clubs, strategy enum, putting probability, and calibration targets remain the compatibility mechanics. The engine stays framework-free, deterministic from the seed hierarchy, and equally used by the human and AI.

## Goals / Non-Goals

**Goals:**

- Give every generated hole a canonical yard-space geometry that deterministically maps positions to surfaces.
- Retain an actual ball position for the duration of a hole, including the legal position after a penalty recovery.
- Represent contact, penalty, recovery, and playable settlement explicitly enough for all consumers to describe one event consistently.
- Let the API project domain geometry and spatial state without exposing engine records or SVG details.
- Migrate incrementally while preserving the resolver's current stochastic model and aggregate calibration as closely as practical.
- Add deterministic, geometry, recovery, API-projection, and regression tests before treating canonical geometry as production authority.

**Non-Goals:**

- Club-bag/loft/spin systems; shot types or intentional draw/fade mechanics.
- Aerodynamic trajectory, bounce/roll, lie-angle, elevation, tree-collision, or terrain-response physics.
- Green contours, spatial putting, advanced short game, or new weather effects.
- A generic GIS, arbitrary SVG-path parser, authoring product, or visual redesign.
- Persisting a paused in-progress playable event. That is outside the current save scope.

## Decisions

### 1. Canonical geometry is a pure, bounded yard-space domain model

Each hole SHALL use local Cartesian yards: the tee reference point is `(0, 0)`, the canonical hole direction is positive `y`, and positive `x` is golfer-right while looking toward the cup. `CourseGeometry` contains a tee point, a green/cup frame, an outer playable boundary, and immutable terrain regions. `Position2d` is a finite value object in that coordinate system.

V1 regions are deliberately modest: every persisted terrain and the playable boundary is a closed simple polygon. The generator MAY use a centreline and widths as transient construction helpers, but it SHALL resolve them to polygons before creating `CourseGeometry`; there is no persistent `Corridor` type and no path/corridor encoding in GraphQL. Consumers receive resolved canonical geometry rather than generator knobs. No SVG path syntax is part of `sim.*`.

Alternative considered: retain a centreline plus variable-width bands only. Rejected because it cannot represent one-sided water, discrete bunkers, or a stable recovery/drop point. Alternative considered: a general polygon/GIS dependency. Rejected as disproportionate; a small deterministic point-in-polygon implementation is sufficient.

### 2. `surfaceAt` has a deterministic precedence contract

`CourseGeometry.surfaceAt(Position2d)` is the only production source of a final terrain surface once a generated hole has migrated. Regions shall be validated at generation time and lookup shall use this fixed precedence: explicit `WATER` and `OUT_OF_BOUNDS`; `BUNKER` and `WASTE_AREA`; `GREEN` and `FRINGE`; `TREES` and `RECOVERY_AREA`; `FAIRWAY`, `FIRST_CUT`, `PRIMARY_ROUGH`, and `DEEP_ROUGH`; then `OUT_OF_BOUNDS` outside the playable boundary. The generator SHALL reject incompatible overlap at the same precedence instead of relying on insertion order.

The initial geometry need not model every visual object. Terrain that is not present in canonical geometry is cosmetic and cannot imply a playable hazard.

### 3. Separate contact from playable settlement

Introduce immutable spatial result values conceptually equivalent to `BallState` and `ShotSettlement`:

- `BallState`: current playable position and lie for the next shot.
- `ShotContact`: the sampled physical landing/contact position and its surface.
- `ShotSettlement`: contact, penalty result, optional recovery/drop position, and the resulting playable `BallState`.

A non-penalty shot settles at its contact point. Water preserves the contact for reporting/animation but sets a legal rough drop as the next playable state. Specifically, let `C` be contact and `P` the pre-shot playable position: starting at `C + normalize(P - C) * 15 yards`, inspect 1-yard points progressing toward `P`; select the first `PRIMARY_ROUGH` point whose cup distance is no greater than `P`'s. Generated water geometry must include a suitable tee-side rough relief corridor. If none exists, the deterministic, explicit fallback is the exact pre-shot state with the existing water penalty and recovery kind `STROKE_AND_DISTANCE_FALLBACK`. Out of bounds also preserves the contact but restores the pre-shot playable state for stroke-and-distance. The existing one-stroke penalty semantics remain unchanged. This is intentionally not a new Rules of Golf engine.

Alternative considered: overwrite the ball's position with a water contact or omit the contact after resolving a penalty. Rejected because it recreates the existing animation/state disagreement.

### 4. Preserve current shot sampling through an incremental spatial resolver seam

The existing resolver continues to sample the same carry and lateral distributions, attributes, conditions, and seeded randomness. For a canonical generated hole, it converts the sampled shot-space offset from the current `BallState` toward the cup into `Position2d`, then evaluates `surfaceAt`. `distanceRemaining` is measured from the playable settlement position to the cup, preserving the existing definition for normal play.

`HoleModel`/`ShotZoneProfile` remain as a bounded compatibility read for the existing carry/lateral sampler and reachable-preview data while `ShotContext` still requires a zone profile. The production course generator and playable/automatic round paths move to the canonical terrain path together: the canonical resolver replaces the sampled profile surface with `CourseGeometry.surfaceAt`, and public current-hole terrain rendering uses geometry rather than this read model. The adapter is explicitly transitional: no new gameplay feature may add terrain by extending `HoleZones` after canonical generation is authoritative. It is eligible for removal only after every generated hole, both human and AI round paths, and the current-hole API/frontend use canonical geometry; the old sampler/preview zone-profile read has no consumer; calibration gates pass; and an architecture check limits any remainder to test support.

Aggregate scoring is a regression constraint, not a promise of byte-identical individual historical outcomes: canonical geometry may move a particular boundary. The implementation must first preserve the sampled distribution and then calibrate generated region dimensions against the current fairway/GIR/scoring harnesses.

### 5. The frontend consumes domain geometry, not rendering instructions

The GraphQL MVP retains `PlayingHole` as the primary current-hole read model and adds `geometry`, `ball`, and `ShotOutcome.settlement`. `geometry` is exactly `{ tee, cup, playableBoundary, regions }`, where each point is local-yard `{ x, y }` and each region is exactly `{ surface, boundary }`; boundary vertex order is canonical and non-repeated. `ball` is exactly `{ position, lie }`. `settlement` is exactly `{ contact { position, surface }, recoveryPosition, recoveryKind, ball }`; `recoveryPosition` is absent for ordinary play and `recoveryKind` distinguishes normal, water-drop, and stroke-and-distance fallback/replay. Existing scalar `PlayingHole` fields can remain as deprecated overview fields during migration, but not as a renderer's terrain source. It does not expose Java records, `surfaceAt`, random seeds as a terrain-authoring API, SVG `d` attributes, colours, animation instructions, or a path/corridor shape.

The frontend owns the yard-to-viewBox transform, visual theme, organic edge treatment, vegetation, texture, shadow, camera, and animation pacing. It SHALL not add, move, or use a visual bunker/water/dogleg/fairway boundary as gameplay terrain. The shipped biome kits, flyover, and non-load-bearing ornaments survive; its local seed may continue to place purely cosmetic decoration but can never place or reshape a gameplay surface. The frontend is migrated in layers: canonical landforms first, then purely decorative dressing around them. Playback starts at the authoritative pre-shot ball, shows the contact, and then visibly moves to the settlement/recovery position when they differ.

### 6. Geometry is seed-derived; current saves need no geometry migration

`GeneratedHole` evolves to own or expose `CourseGeometry`, deterministically derived from the existing course/hole seed and generator version. Geometry is regenerated when a world is restored, like the course pool today; it is not serialised as duplicate mutable data.

The current `WorldSnapshot` does not persist a pending `PlayableEvent` or mid-hole `PlayableRound`; therefore existing saved games contain no in-progress ball state to migrate. This change SHALL not claim to add mid-event saves. If a later change snapshots a pending event, that snapshot must include `BallState` and any required settlement state so load resumes from the same legal point.

### 7. Testing establishes invariants before visual polish

Tests shall verify seed reproducibility, finite/valid counter-clockwise polygons, precedence, no invalid overlap, settlement-to-surface agreement, the exact 15-yard/1-yard water-relief sequence and fallback, automatic/interactive equivalence, DTO fidelity, adapter-boundary enforcement, and preservation of calibration tolerances. Frontend unit tests shall consume a representative geometry DTO and verify it does not create gameplay terrain outside its canonical regions. Manual E2E remains required for legibility and transition behaviour. The persistence test protects the present boundary by rejecting a pending event rather than silently discarding its ball state; a later feature that permits that snapshot must add an explicit round-trip spatial-state contract.

## Risks / Trade-offs

- **Canonical geometry alters individual legacy results** → retain the carry/lateral sampler, introduce geometry with calibration harnesses, and allow the legacy fixture adapter only while migration is incomplete.
- **Polygon handling becomes an accidental physics engine** → restrict V1 to point containment, boundary validation, and deterministic recovery-point selection; defer collision and rolling.
- **Frontend needs a broad rendering rewrite** → retain its biome/theme/drawing utilities and replace only gameplay-relevant synthesized landforms incrementally.
- **Region overlap creates ambiguous lies** → validate generation and define precedence centrally; reject same-priority conflicting regions.
- **API payload size grows** → expose compact vertices and only the current hole's geometry; cache by immutable course/hole identity on the client.
- **A later event-save feature omits ball state** → document the explicit persistence requirement now and add a contract test if event snapshots are introduced.

## Migration Plan

1. Add pure spatial types, geometry validation/lookup, and seeded generated geometry alongside `GeneratedHole` fields; do not remove `HoleZones`.
2. Add canonical `BallState`/settlement handling to the shared round loop and a production spatial resolver path, while keeping the legacy `HoleModel`/zone-profile adapter for fixtures and controlled comparison tests.
3. Calibrate generated region dimensions against the existing deterministic scoring, fairway, GIR, putting, and player/AI equivalence harnesses. Remove production dependence on `HoleZones` only after those gates pass.
4. Evolve `PlayingHole` and `ShotOutcome` with the defined additive geometry and spatial-state DTOs, then change the frontend to render canonical landforms while preserving its biome/flyover/decorative layers.
5. Remove or deprecate the obsolete zone-profile read model once it has no consumer; guard production against `HoleZones` dependencies and move any surviving fixture adapter to test support. A later change may delete that test-only adapter.

Rollback before step 5 is feature-flag/adapter based: production may continue resolving through the legacy path and rendering the existing presentation while canonical geometry remains unused. No persisted save conversion is required because geometry regenerates and paused events are not saved.

## Resolved Review Decisions

- The completed `add-2d-hole-graphics` change is archived before this change and its base visualization specification is modified below; there is one authoritative visual-terrain contract.
- The GraphQL MVP uses generic polygon `TerrainRegion { surface, boundary }` values, with no path/corridor DTO.
- Water relief is the tee-ward 15-yard start, 1-yard deterministic rough scan specified above, with an explicit stroke-and-distance fallback.
- The remaining production zone-profile compatibility read is documented rather than mislabelled as fixture-only; retirement has the objective gates listed in Decision 4 and the canonical-geometry specification.
