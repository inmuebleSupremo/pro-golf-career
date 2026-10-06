## Why

The Play Event screen is text- and stat-driven: the player reads a distance and a lie, picks a club, and sees a number back. There is no picture of the hole, so shot-by-shot decisions have no spatial meaning. We want a 2D graphical hole layer so the player *sees* the hole they are playing and each shot lands on it.

Exploration corrected two premises. Courses are **already** permanent and deterministic — `World.generateCoursePool()` builds a fixed seed-derived pool, regenerated identically on restore, and `GeneratedHole` geometry is immutable. The pin is **already** dynamic per round via `GeneratedHole.pinFor(round)`. The real gaps are narrower: (1) marquee **named** events do not keep a venue — `courseIndex = nextTournamentId % coursePool.size()` rotates every season, so "The Masters" plays a different pool course each year; (2) hole geometry is not exposed to the frontend at all (only the per-shot situation is); (3) there is no visual layer.

V1 keeps the simulation **untouched**: fidelity is "visual now, real later", so the 476-test regression surface, snapshots, and shot engine are not at risk.

## What Changes

- Add a **parametric 2D hole render** to Play Event, drawn purely from each hole's existing sim numbers (par, length, fairway/green dimensions, elevation, hazard flags, active-round pin side) and styled by per-biome kits lifted from the `docs/course-holes` template art. Change a number, the picture follows — no hand-authored layouts.
- **Ball-flight playback** of each resolved shot, and a **broadcast flyover-intro** per hole: the existing scene photo becomes a brief intro card that recedes to the persistent schematic.
- An **additive, read-only GraphQL query** exposing per-hole geometry as an application DTO: dimensions, hazard flags, elevation, the active round's pin lateral, and a stable layout seed for deterministic cosmetic placement. No engine type leaks; no mutation.
- **Pin marquee named events to permanent venues**: the majors and tour championships each keep a fixed pool course across seasons; regular and signature events keep rotating. Deterministic and reproducible from the world seed.
- A **fidelity contract**: load-bearing facts (final surface, distances, penalties, pin side, hole dimensions) render literally and are never contradicted; hazard flank, dogleg lean, and tree scatter are seed-derived flavor.

## Capabilities

### New Capabilities
- `web-hole-visualization`: the Play Event screen renders a faithful, biome-styled 2D representation of the hole being played, with per-round pin placement and animated shot playback, never contradicting a resolved shot's mechanical facts.

### Modified Capabilities
- `graphql-api`: a new read query exposes a playing hole's geometry (dimensions, hazards, elevation, active-round pin, layout seed) as a DTO, so the client can render it without any engine type crossing the API edge.
- `world-schedule`: marquee named events (majors and tour championships) SHALL keep a permanent venue across seasons, rather than rotating through the course pool by tournament id.

## Impact

- **frontend**: a new parametric hole SVG component + biome style kits + shot playback + flyover-intro, integrated into `components/play/play-event.tsx`; the scene photo (`lib/play/scene.ts`) is demoted from banner to intro. New API binding in `lib/api/play.ts`.
- **backend app layer** (`com.progolf.app.api`): a new `PlayingHole` DTO, a schema query, and a resolver reading existing engine geometry. `schema.graphqls` gains the query + type.
- **backend sim** (`com.progolf.sim.world`): `world-schedule` course allocation gives marquee events a stable course index; isolated and deterministic. No change to `sim.course`, `sim.shot`, snapshots, or the shot-engine regression harness.
