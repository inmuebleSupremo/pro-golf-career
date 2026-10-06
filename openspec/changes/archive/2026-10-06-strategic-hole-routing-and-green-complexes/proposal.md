## Why

`course-design-foundations` makes every V2 venue deliberate at the brief level, but its one-corridor geometry still cannot express where the player should progress, which side of a fairway offers a better approach, or how a green's orientation changes the final shot. Its positional and risk/reward archetypes are therefore deliberately bounded scalar biases, not yet spatial golf strategy.

This change introduces the smallest spatial design layer that makes those briefs honest: a piecewise-linear route, explicit landing zones, and a green-complex plan, compiled into the existing authoritative polygons. It is a new generator version so V1/V2 course identity and saved careers stay exact.

## What Changes

- Add V3 as a retained, version-selected course generator. V1 and V2 stay byte-for-byte historical implementations; new V3 worlds use the current version and restored worlds use their stored pin. Unknown versions fail explicitly.
- Map each V2 `HoleBrief` to internal V3 semantics before terrain compilation: a constrained `HoleRoute`, route-relative `LandingZone`s, a `GreenComplexPlan`, green-surround meanings, and deterministic progression targets.
- Compile those semantics into the current canonical `CourseGeometry`: a route-following fairway corridor, route-relative width/pinch/widen/bailout variation, a rotated green, and compatible existing hazards. Canonical polygons remain the only gameplay and SVG terrain authority.
- Add a minimal compatibility bridge so AI and human/default shot flows use generator-provided route progress targets rather than trying to cut directly through a dogleg. Existing strategy labels may choose safe/aggressive recommendations; no free spatial aiming or shot-control UI is introduced.
- Define deterministic V3 calibration and migration evidence while retaining exact V1/V2 fixtures and using version-appropriate V3 envelopes rather than V2 byte equality.

## Capabilities

### New Capabilities

- `strategic-hole-routing`: version-three semantic routes, landing zones, green complexes, route feasibility, canonical compilation, and progression-target compatibility behavior.

### Modified Capabilities

- `course-generation`: V3 is a supported selected generator and compiles spatial intent before canonical terrain.
- `course-design`: V3 gives existing hole briefs bounded spatial expressions without changing their 18-hole planning role.
- `hole-spatial-model`: generated semantic intent remains associated metadata; canonical geometry remains authoritative.
- `course-persistence`: historical courses retain V3 semantics through deterministic regeneration under their generator pin.
- `save-persistence`: snapshots persist provenance, not mutable polygons or a duplicate spatial representation.

## Impact

- **Simulation/course domain:** pure immutable semantic records, a V3 planner/compiler, generator registry entry, validation, and regression/calibration fixtures.
- **Shot flow:** a narrow target-selection bridge for existing AI/human defaults; the resolver, ball physics, current strategy options, and canonical surface settlement remain intact.
- **Persistence:** no new whole-polygon serialization; V3 semantic records are regenerated from the pinned seed/version and retained source inputs as part of course generation.
- **API/frontend:** no GraphQL field, interactive aiming control, or SVG redesign. Current canonical SVG rendering continues to consume `CourseGeometry`.

## Non-Goals

- Role-based hazard generation, fancy water/bunker layouts, generated double doglegs, or branching/multi-route fairways. A later `role-based-hazard-generation` change may own that work; it is not created here.
- Player-selectable free aiming; club-to-target UI; shot type, shape, trajectory, physics, elevation, contours, or spatial putting.
- A second terrain authority, serialised polygon saves, a course-profile API/UI, or major visual redesign.
