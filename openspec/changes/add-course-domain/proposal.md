## Why

The shot-resolution core already consumes a `ShotZoneProfile` and depends on a `HoleModel` interface, but nothing produces them yet — every test hand-builds a profile. Before tournaments can be played on anything real, the simulation needs Courses: persistent, procedurally-generated 18-hole environments that emit exactly the zone-band data the engine consumes. The Course domain (REQ-068–085) is the next foundational layer: it turns a seed into a playable, reproducible course while owning nothing about competition. Building it now unblocks the tournament engine and gives the shot engine real inputs.

## What Changes

- Define the **Course** and **Hole** domain entities (REQ-068–070): a Course is exactly eighteen Holes plus the environmental information required for play; par is derived from holes; a Course is immutable once generated for a tournament.
- Add **procedural course generation** driven by the existing `deterministic-rng` seed hierarchy: from a course seed, generate pars, lengths, tee/green positions, elevation, and — crucially — each hole's surface and hazard layout expressed as the existing `ShotZoneProfile` / `ZoneBand` types. Generation implements/emits the `HoleModel` interface the shot engine already uses.
- Support **per-round pin positions** (REQ-076): a hole exposes one active pin per round; pins may vary between rounds while the rest of the hole stays fixed within a completed round.
- Add **course identity**: name, region, environment classification (Links, Parkland, Desert, Mountain, Coastal, Woodland), and style (REQ-079/080). Classification is data other domains read to influence weather generation and strategic character — the Course itself does not generate weather.
- Add an **analytical difficulty profile** (REQ-078) derived from length, surface layout, hazard placement, green complexity, and exposure. Difficulty is descriptive only and never modifies player attributes.
- Add the **player↔course mastery** relationship (REQ-081): familiarity tracked per player, owned by the relationship (not the Course), never transferable between courses, persisting across seasons.
- Add **course persistence as historical assets** (REQ-082): courses generated during a career are permanent; historical tournament results reference the exact course used and remain reproducible from seed + version.
- Reuse the existing 13-surface catalogue (REQ-071/072); every generated playable location belongs to exactly one Surface. No new surfaces are introduced.

Explicitly out of scope (enforced by requirements, not just omission): no tournament-specific data in the Course domain — no leaderboards, prize money, rankings, competitors, or round scores (REQ-083); no dependence on any visual representation (REQ-084); no shot resolution, tournament management, AI, weather generation, or progression logic (REQ-085). Implementation is a later change — this proposal produces artifacts only.

## Capabilities

### New Capabilities
- `course-generation`: Deterministic procedural generation of a complete 18-hole course as `HoleModel` / `ShotZoneProfile` data — pars, lengths, tee/green/pin positions, elevation, and surface/hazard zone bands from a seed (REQ-068/069/070/071/072/073/074/075/076/077).
- `course-identity`: A Course's stable identity — name, region, environment classification, and style — that other domains read for weather and strategic character (REQ-079/080).
- `course-difficulty`: An analytical, read-only difficulty profile emerging from a Course's physical characteristics (REQ-078).
- `course-mastery`: The per-player familiarity relationship with a Course, owned separately from the Course and persistent across seasons (REQ-081).
- `course-persistence`: Courses as permanent historical assets whose exact form is reproducible for historical tournament results (REQ-082).

### Modified Capabilities
<!-- None. This change DEPENDS ON the existing hole-spatial-model (ZoneBand/ShotZoneProfile/Surface,
     HoleModel) and deterministic-rng (seed hierarchy) capabilities but changes none of their
     requirements — it produces the data those contracts describe. -->

## Impact

- **Codebase**: New framework-free `com.progolf.sim.course` package(s) in the existing `backend/` module, consistent with `sim.core` / `sim.spatial` / `sim.shot`. Produces `spatial.ShotZoneProfile` and implements `shot.HoleModel`; consumes `core.SeedCoordinate` / `RngFactory`. No persistence framework yet — the domain exposes serialisable state; the Persistence change wires storage later.
- **Downstream consumers (future changes)**: The Tournament engine will select/assign generated Courses and read pin positions per round; Weather generation will read environment classification; Statistics/records will reference course identity. The `HoleModel` emitted here is the same interface `RoundResolver` already depends on, so the shot engine needs no change.
- **Dependencies**: No new third-party dependencies. Generation must route all randomness through the existing seed hierarchy — no ad-hoc RNG — preserving reproducibility (REQ-082, REQ-265/299).
- **Boundary risk**: Difficulty and identity are analytical/data only; requirements forbid them from affecting attributes or embedding tournament state, keeping domain ownership clean (REQ-083/085).
