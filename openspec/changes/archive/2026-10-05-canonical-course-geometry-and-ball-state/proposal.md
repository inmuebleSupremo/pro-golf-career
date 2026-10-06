## Why

Career Mode currently resolves golf in a deterministic 1D corridor while the client independently invents a 2.5D SVG hole. A landing label can be made to look plausible, but the simulation's terrain, the displayed terrain, the recovery point, and the next-shot origin are not one shared event. This foundational change establishes the smallest shared spatial model needed to make those facts coherent before adding richer shot or physics mechanics.

## What Changes

- Add a pure, seed-derived canonical 2D course-space model in yards, including tee, cup, playable terrain, hazards, and out-of-bounds.
- Add deterministic surface lookup and a persistent per-hole ball position so the engine can identify the actual origin and settlement of each shot.
- Define a settlement record that distinguishes physical contact, penalty result, legal recovery/drop, and the playable next-shot position.
- Transition generated production holes from presentation-only `HoleZones` to canonical geometry while retaining a constrained legacy adapter for existing fixtures and staged migration.
- Expose application DTOs for canonical geometry and spatial shot state; make the frontend render those DTOs rather than generate gameplay-relevant terrain locally.
- Preserve seeded determinism, shared human/AI resolution, and current club/strategy outcome distributions as far as practical; no new ball-flight, club-bag, putting, or terrain-response mechanics are introduced.

## Capabilities

### New Capabilities

- `canonical-course-geometry`: a pure canonical coordinate, terrain, lookup, ball-state, and settlement foundation shared by gameplay consumers and presentation.

### Modified Capabilities

- `course-generation`: generated holes produce stable authoritative geometry in addition to the existing strategic data during migration.
- `hole-spatial-model`: canonical 2D terrain becomes the authoritative surface source; zone bands become a compatibility seam rather than the permanent spatial authority.
- `shot-resolution`: a complete outcome distinguishes contact and playable settlement coordinates without changing the present shot-mechanics vocabulary.
- `playable-round`: interactive and automatic hole loops retain the playable ball position and apply identical recovery transitions.
- `graphql-api`: authenticated read models expose canonical geometry and spatial shot/ball state through DTOs only.
- `save-persistence`: regenerated geometry and any future persisted in-progress ball state retain deterministic continuity without introducing framework concerns into `sim.*`.
- `web-hole-visualization`: the implemented 2D hole layer switches from client-authored gameplay landforms to canonical terrain while retaining its biome, flyover, and decorative presentation work.

## Impact

- **Simulation:** new pure spatial value types and lookup service under `sim.course`/`sim.spatial`; generated-course and round-loop integration; compatibility support for current `HoleModel` fixtures.
- **Application/API:** new geometry and position DTOs/mappers/schema fields at the `WorldService` boundary; no engine types cross GraphQL.
- **Frontend:** the existing SVG remains a renderer, but consumes canonical terrain polygons/boundary and authoritative shot origins/settlements. Biome styling, flyover, cosmetic vegetation, texture, and lighting remain client-owned.
- **Persistence:** generated geometry remains derivable from seed/version. The current save format does not preserve an in-progress `PlayableEvent`; this change does not broaden that scope, but specifies how a later event snapshot must carry `BallState`.
