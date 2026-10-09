## Context

V6 is a retained successor, not a rewrite of V5. It keeps V5 local candidate architecture and canonical compiler,
but adds the missing upstream course-scale decision: where every hole belongs and what shared landscape it responds
to. The engine remains pure and deterministic.

## Design

### D1 — Shared course coordinates and bounded placements

`CourseLandscapePlan` owns a course-yard frame, environmental profile, continuous features and eighteen ordered
`HolePlacement`s. A placement contains a tee origin, heading, rigid invertible local-to-course transform, conservative
design envelope and predecessor/successor transition facts. It maps local canonical points and polygons to course
space, and maps landscape samples back into a local candidate frame.

V6 derives a fixed, seed-indexed landscape and candidate placement budget. It uses deterministic scored selection
with explicit rejection reasons: envelope overlap, unsafe spacing, failed inverse transform, unsuitable landscape
relation and excessive green-to-next-tee transition. If a search exhausts, a documented deterministic fallback
selects an open-ground routing ring with valid spacing/transitions; it still creates one course plan and never
silently reverts to eighteen unrelated diagrams.

### D2 — Continuous environmental systems

The first feature vocabulary is deliberately bounded: open ground; parkland fields/copses; woodland masses and
clearings; links heath/dune bands; lakes; and coast/bay shore curves with a defined water side. Parameters are
continuous seeded control points/extents/density fields, never named maps or showcase templates. A coast/bay is a
single shared feature observed by several selected holes, not repeated water ellipses. Woodland is course-scale
massing with selected fairway corridors/clearings. Parkland combines broad fields, copses and varying enclosure.
Links remains exposed/open and uses dune/heath context distinct from coastal parkland.

### D3 — Landscape is an upstream candidate constraint

Each placement receives local samples for open-corridor availability, feature-edge distance/direction, clearing or
woodland density, shore proximity and permitted recovery sides. V5 route knots, width stations, landing targets,
approach sectors, green placement and bunker/recovery intent respond to those facts before selection. The existing
V5 target, pin, topology and human/AI validation still reject unsupported or unplayable output.

### D4 — Three layers, one physical truth

| Layer | Content and authority |
| --- | --- |
| Gameplay | Local effective `CourseGeometry`; the sole authority for surfaces, targeting, settlement, recovery and penalties. |
| Context | Immutable `CourseLandscapePlan` features and transforms; establishes geography but cannot change mechanics. |
| Presentation | Deterministic renderer detail constrained by supplied context and masks; cannot create a gameplay surface. |

When a shared feature crosses a playable boundary, V6 either compiles its in-bound expression from the same source
into canonical geometry or masks context outside. The milestone uses bounded primitives and sampled tests rather
than generic boolean geometry. No duplicate random shore/tree wall can disagree between context and gameplay.

### D5 — Projection and evidence

`PlayingHole` receives a read-only local context projection with stable course/feature identities. The gallery also
uses a read-only course-map projection containing shared context plus transformed canonical-hole geometry. Normal
play, padded local gallery and complete course map use those same generated facts at different scales.

The gallery covers every hole of complete known and locked-unseen V5/V6 corpus courses. Quality reports include
placement validity, transitions, shared coastline/clearing/field relationships, context/canonical agreement and
existing routing/structural diversity measures. Metrics gate regressions but do not replace manual visual review.

### D6 — Versioning and persistence

V6 is explicit and retained. V1--V5 output remains exact. A V6 course regenerates landscape, placements and local
canonical terrain from existing seed hierarchy, version and inputs; saves carry provenance, not duplicated polygons,
context or candidate pools. V6 becomes current only after later explicit acceptance.

## Risks and mitigations

- **Placement failure:** fixed budgets, rejection diagnostics and the valid course-scale fallback prevent ambient
  retries and independent-hole degradation.
- **Decoration becoming false physics:** context is below canonical geometry and cross-boundary features compile or
  clip.
- **Visual sameness:** complete known/unseen galleries inspect shared relationships and structural diversity.
- **Scope creep:** elevation, collision, carries, bridges and generic booleans remain excluded.
