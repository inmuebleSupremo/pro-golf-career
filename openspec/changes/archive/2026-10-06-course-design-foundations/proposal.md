## Why

The current generator is deterministic and spatially authoritative, but it composes a course by shuffling a fixed par mix and independently sampling each hole. As a result, biome affects a small set of biases and style is cosmetic; neither gives an 18-hole venue a deliberately composed, strategically recognisable identity.

This change establishes the smallest useful design layer before the canonical geometry compiler: a seeded course profile produces a coherent 18-hole plan of lightweight briefs, and that intent measurably steers the existing generator without replacing the canonical spatial/play architecture. It also closes the current generator-version persistence gap before a material generator revision can rewrite saved venues on restore.

## What Changes

- Add a deterministic, course-level design model: a compact `CourseDesignProfile`, an 18-hole `CoursePlan`, and parameterised `HoleBrief`s.
- Make the first design slice functional: profile and briefs shall steer par ordering, length bands, fairway-width tendency, and existing hazard/recovery inputs before the current canonical geometry compiler runs. The three deliberately small archetypes (positional, balanced, and risk/reward) each have a bounded current-generator expression rather than serving as labels.
- Preserve the current canonical `CourseGeometry`, gameplay resolver, setup variants, human/AI interfaces, and seeded determinism; the new model is design intent, not another spatial representation.
- Introduce version-selected course generation and version-pinned world restoration. The current generator becomes a retained historical version; the first design-aware generator is a new version.
- Persist the world/course generator version in snapshots and support existing saves that lack it by restoring them through the historical version rather than silently regenerating them with the latest algorithm.
- Add a locked-before-tuning V1 baseline/calibration contract plus composition, profile-adherence, deterministic/version, save-compatibility, geometry-validity, human/AI, and calibration guardrail coverage.

## Capabilities

### New Capabilities

- `course-design`: deterministic course-design profiles, deliberate 18-hole plans, and lightweight hole briefs that can guide generation while remaining separate from biome, event setup, and simulation settlement.

### Modified Capabilities

- `course-generation`: select a retained generator implementation by explicit version and compile design intent through the existing canonical geometry pipeline.
- `course-persistence`: preserve historical course identity by pinning every restored world/course pool to its recorded generator version.
- `save-persistence`: record generator-version provenance and load legacy saves without that provenance through the known historical generator version.

## Impact

- **Simulation/course domain:** new pure immutable planning records and a versioned generator registry; `Course` exposes the generated profile/plan as design data while `GeneratedHole` and `CourseGeometry` remain the playable terrain authority.
- **World/snapshot/persistence:** `WorldSnapshot` carries a course-generator version; application persistence supports the prior save format as a V1-default migration path and writes the new provenance for future saves.
- **Gameplay/calibration:** human and AI continue through existing `HoleModel`/geometry seams. The first slice changes only bounded generation inputs and adds profile-aware diagnostics before later routing, hazard-role, and shot-intent work.
- **API/frontend:** no player-facing GraphQL, route, SVG, or visual redesign is required in this change. Existing rendering consumes the resulting authoritative geometry unchanged.
