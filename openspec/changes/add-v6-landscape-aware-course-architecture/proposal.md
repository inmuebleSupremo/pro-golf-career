## Why

V5 creates richer individual golf holes, but its holes still occupy independent local coordinate systems. Local tree
regions and shore shapes cannot make an ordinary eighteen-hole course read as one believable coastal, woodland,
parkland or links landscape. Players need a course routed through shared geography while gameplay continues to use
the existing authoritative canonical terrain.

## What Changes

- Add retained generator version V6. V5 output is frozen; V1--V5 pins continue to regenerate exactly.
- Generate a deterministic shared `CourseLandscapePlan` and all eighteen oriented, non-overlapping `HolePlacement`s
  before V5-derived local architecture is selected and compiled.
- Use continuous shared coast/bay, woodland/clearing, parkland field/copse, links heath/dune and lake context to
  constrain routing, clearings, landing areas, green approaches and truthful canonical recovery/hazard geometry.
- Project the same immutable landscape context for normal play, padded local gallery views and complete course maps.
- Add course-scale quality evidence and a complete V5-versus-V6 gallery across known and locked-unseen seeds.

## Capabilities

### New Capabilities

- `course-landscape`: deterministic course-scale geography, bounded placement selection, transforms and landscape
  relationships that precede local hole architecture.

### Modified Capabilities

- `course-generation`: retained V6 selection composes shared geography before local canonical terrain.
- `course-design`: course identity includes placement, transitions and shared environmental massing.
- `hole-spatial-model`: context and transforms remain distinct from authoritative local gameplay geometry.
- `course-persistence` and `save-persistence`: V6 regenerates from explicit version/seed provenance without
  polygon snapshots or migration of V1--V5 saves.
- `graphql-api` and `web-hole-visualization`: read-only context projections support truthful normal and gallery
  rendering without client gameplay authority.

## Impact

- **Simulation:** pure deterministic landscape/placement records and bounded candidate selection in `sim.course`.
- **Gameplay:** V5 routing, fairway/green geometry, target selection and human/AI parity remain the local engine
  foundation; `CourseGeometry` remains the sole surface/settlement authority.
- **API/rendering:** additive immutable context/map DTOs; the renderer layers context behind literal effective
  canonical geometry.
- **Persistence:** V6 provenance only. No existing save, course pool or historical result is rewritten.

## Non-Goals

- Elevation/terrain meshes, generic polygon booleans, islands, bridges, tree-flight collision, forced carries,
  ground-hazard traversal, new shot physics, or client-side terrain mutation.
- A finite catalogue of course maps or hole templates.
