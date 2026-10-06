## Why

V3 made route, landing-zone, and green-complex intent explicit, but its bunker, water, and tree features are still seeded boolean outputs. They are plausible terrain, not deliberate golf-course design: a hazard cannot currently identify the strategic decision it is meant to influence.

V4 should translate existing spatial intent into a small semantic hazard plan before compiling canonical terrain. This makes hazards intentional without claiming mechanics the simulation does not yet have, such as flight-path collision, forced carries, tree obstruction, or player free aiming.

## What Changes

- Add V4 as a retained, explicit course-generator version. V1, V2, and V3 remain unchanged; new worlds select V4 only after implementation, while restored worlds regenerate through their stored version pin.
- Add a compact internal `HazardFeature` plan for existing surfaces (`BUNKER`, `WATER`, `TREES`, `RECOVERY_AREA`). Every V4 gameplay hazard polygon has semantic provenance: a bounded role, meaningful anchor, side, and severity.
- Plan features from `HoleRoute`, landing zones, dogleg turns, `GreenComplexPlan`, `HoleBrief`, profile, biome, and deterministic seed; then compile them into the existing authoritative `CourseGeometry`.
- Define V4 feasibility, overlap, water-relief, route-target, AI/default-human compatibility, calibration, and quality contracts.
- Retain the current canonical renderer with no visual redesign. V4 avoids overlapping strategic hazards, so a renderer-order change is not required unless implementation proves an existing canonical overlap cannot be avoided.

## Capabilities

### New Capabilities

- `role-based-hazard-generation`: V4 semantic hazard features, strategic placement roles, feasibility, canonical compilation, and calibration.

### Modified Capabilities

- `course-generation`: V4 is a supported version and compiles semantic hazard plans into canonical hazard regions.
- `hole-spatial-model`: hazard semantics remain generated metadata while canonical geometry remains the sole terrain authority.
- `canonical-course-geometry`: V4 water and hazard geometry preserve deterministic precedence, relief, and settlement safety.
- `strategic-hole-routing`: V4 route/default targets remain clear of designed hazards.
- `course-persistence` and `save-persistence`: V4 uses existing pinned deterministic regeneration without polygon snapshots.

## Impact

- **Simulation/course domain:** pure immutable planning records, deterministic V4 hazard planning/compiler validation, generator registry support, and calibration fixtures.
- **Shot flow:** no new control system or flight model. Existing route targets remain a compatibility bridge and must not be generated inside hazards.
- **Persistence:** the world-level generator pin remains the provenance contract; semantic features and terrain regenerate from seed plus V4 inputs.
- **Frontend:** canonical polygons continue to render directly. No strategy overlay, new hazard UI, or SVG redesign is introduced.

## Non-Goals

- Free aiming, club-bag redesign, shot types, shaping, trajectories, flight-path/terrain intersection, or forced-carry mechanics.
- Tree collision, line-of-sight blocking, punch-out requirements, elevation/contours, or putting redesign.
- A new terrain engine, new surface catalogue, polygon snapshots, splines, terrain meshes, or complex boolean geometry.
- Cosmetic-only hazard roles, island-green novelty, or a large frontend redesign.
