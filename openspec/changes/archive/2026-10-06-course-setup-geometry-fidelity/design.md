## Context

`RoundHole` resolves canonical contact through `GeneratedHole.geometryForWidth(setup.widthScale())`. In contrast, `PlayableEvent.currentHole()` exposes a `GeneratedHole`, and `WorldService` passes its base geometry to `ApiMapper.playingHole`. Thus an event with a non-neutral width setup can classify a contact against one polygon set while the client renders another. Pin coordinates are setup-aware, but terrain is not.

`CourseGeometry` is immutable and setup variants are deterministically regenerated and cached from the hole seed, dimensions, hazard flags, and width scale. This change corrects projection fidelity only; it does not change setup values, calibration, generation design, or the shot model.

### Deferred generator-version/save compatibility recommendation

Courses are regenerated on restore from the master seed/configuration. `Course.generatorVersion` is currently descriptive: restoration does not select a historical generator implementation. A material generator revision could therefore change venues in existing saves.

The smallest robust policy for `course-design-foundations` is **version-pinned regeneration**: persist the generator version used by a world/course pool in the save/snapshot, route generation through a versioned generator registry, and keep the prior implementation or a deterministic compatibility adapter while saves using it remain supported. New saves use the latest version. This preserves venue identity and deterministic restoration without serialising every polygon.

Persisting complete generated geometry is a larger storage/schema commitment and does not by itself preserve old generator semantics for future new venues. Deterministic migration is only appropriate when an equivalence proof exists; it should not be assumed for richer course design. The later change must define retention/deprecation policy, save migration behavior, and regression coverage before changing `GENERATOR_VERSION`.

## Goals / Non-Goals

**Goals:**

- Make the setup-specific geometry used by shot settlement the exact geometry returned to the API and rendered by the client.
- Preserve deterministic variant generation, caching, neutral setup behavior, and current setup-calibration semantics.
- Demonstrate agreement through direct engine, GraphQL, and frontend rendering tests.

**Non-Goals:**

- Redesign course difficulty, hole generation, hazards, or course identity.
- Change `CourseSetup` factors, scoring calibration, GraphQL shape, or visual styling.
- Implement generator-version persistence or migration in this change.

## Decisions

### Project the active `HoleModel` geometry

Expose a presentation accessor from `PlayableEvent` for the exact `RoundHole`/playoff `HoleModel.geometry()` active for the requested hole and round. `WorldService` and `ApiMapper` shall receive that effective `CourseGeometry`, while overview metadata continues to originate from the stable `GeneratedHole`.

This avoids recomputing setup geometry in the application layer and makes the simulation's geometry the sole authority. Passing only a width scale to the mapper was rejected because it duplicates generation knowledge outside the engine.

### Preserve one deterministic cached source

`GeneratedHole.geometryForWidth` remains the variant factory/cache. The correction shall reference its immutable result; it shall not persist or copy polygons per event, player, request, or DTO. Neutral setup returns the existing base geometry instance/value.

### Test geometry, not only fields

Tests shall choose a non-neutral setup and a coordinate whose surface changes between base and effective geometry, then prove that resolver settlement, API geometry, and the rendered polygon agree. Default setup tests shall prove no behavior or payload drift. Existing GraphQL types remain additive-compatible because the shape does not change.

## Risks / Trade-offs

- **A presentation accessor accidentally uses a different round/setup** → derive it from the active playable event/hole model and cover normal rounds plus playoff/pre-fetch behavior.
- **Variant generation is duplicated or unbounded** → retain the existing immutable cache and pass its result by reference/value only.
- **Calibration changes while fixing projection** → prohibit changes to setup values or resolver logic and run existing calibration/setup tests.
- **Legacy reachable preview remains a separate transitional read** → this change guarantees canonical terrain projection only; a later course-design change must retire or align preview bands before they describe richer terrain.

## Migration Plan

1. Add the effective-geometry presentation seam and route `PlayingHole.geometry` through it.
2. Add regression coverage for neutral and non-neutral setups, including surface classification and GraphQL projection.
3. Verify the frontend renders returned regions unchanged.
4. Rollback is safe: restore the prior base-geometry projection; no saved state, schema field, or generated terrain is migrated.
