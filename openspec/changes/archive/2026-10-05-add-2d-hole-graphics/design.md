## Context

A played event flows `PlayableEvent` → `ShotSituation` (hole, par, shot number, distance-to-pin, lie, `pinLateral`, reachable `ShotZoneProfile`) → the player's club/target/strategy → `ShotOutcome` (`finalSurface`, `carry`, signed `lateral`, `distanceRemaining`, `hazardEntered`, `penaltyStrokes`, `strokes`). The frontend renders this as stats in `play-event.tsx`; only the per-shot situation reaches the client (via the `shotSituation` query), never the hole itself.

Hole geometry is immutable and abstract. `GeneratedHole` carries `par`, `length`, `fairwayHalfWidth`, `greenHalfWidth`, `greenDepth`, `hasGreensideBunker`, `hasWater`, `hasTrees`, `elevationDelta`, and a `holeSeed`; `pinFor(round)` derives the per-round pin. Crucially the hazard model in `HoleZones` is **left/right symmetric** — water/tree/bunker bands flank both sides equally — so a −30y and a +30y miss resolve identically. The sim has no concept of "bunker left, water right", dogleg, or discrete hazard position.

Course venues are already stable per world seed (`generateCoursePool()`), but the schedule binds a course to an event by `courseIndex = nextTournamentId % coursePool.size()` (`World.generateStructuredSchedule` / `generateProportionalSchedule`), and the id counter climbs every season, so a recurring named event does not keep its course.

The six `EnvironmentClassification` values already flow to the client as `courseType` and drive the scene photos; `docs/course-holes/*.svg` are six hand-authored, richly illustrated single holes whose `<defs>` (lobed deciduous canopies, jagged pines, palms, fescue-texture rough, pot bunkers, mower stripes, drop-shadows) are the art language to inherit.

## Goals / Non-Goals

**Goals:**
- Render a faithful, biome-styled 2D hole in Play Event, parametrically from the hole's own numbers, scaling to all 18 holes of every pool course.
- Animate each resolved shot as ball-flight playback; introduce each hole with a broadcast-style flyover card that recedes to the schematic.
- Expose hole geometry to the client additively, with no engine type crossing the API edge and no mutation.
- Give marquee named events a permanent venue across seasons.
- Keep the simulation byte-identical: no shot-engine, snapshot, or persistence change.

**Non-Goals:**
- No new shot input model — the player still chooses club/target/strategy; the 2D view is an **output** surface (locked: draw-2D / resolve-1D hybrid).
- No mechanically-real hazard sidedness in V1 — hazard flank stays cosmetic (the "real later" phase would graduate it into sim data).
- No regeneration of course geometry, and no change to `HoleZones` symmetry or `ShotZoneProfile` invariants.
- No new course art commissioned — the templates become a reusable style kit, not fixed backdrops.

## Decisions

**Decision: the fidelity contract — load-bearing vs. flavor.** Load-bearing facts the UI SHALL render literally and never contradict: `finalSurface`, `carry`/`distanceRemaining`, `penaltyStrokes`/`strokes`/`hazardEntered`, `pinLateral` (the pin's side is real), and the hole dimensions (par, length, widths, green). Flavor the client MAY synthesize: which flank a hazard sits on, dogleg lean, hazard longitudinal position, tree scatter, and which template symbols dress the biome. The reconciling rule: because hazard bands are symmetric, when `finalSurface` is a hazard the ball is drawn **in the hazard the schematic actually rendered** (whichever flank), rather than by the raw signed `lateral`; every mechanical fact stays true while the side stays cosmetic. Non-hazard lies place by the real signed `lateral`.

**Decision: parametric SVG generator, not fixed-backdrop overlay.** The render is generated from geometry — a fairway corridor of the hole's length/width, a green ellipse of its size/depth, tee, and the active pin on its real lateral — then dressed with template `<defs>`. A fixed backdrop cannot match a hole that is 340y with a narrow green; a generator can, and it scales across the whole pool. This lives entirely in the frontend.

**Decision: cosmetic placement is derived on the client from a stable layout seed.** The query returns the hole's `holeSeed` (as a string, to survive JS 64-bit limits) as a `layoutSeed`. A small seeded PRNG turns it into stable hazard flanks, dogleg lean, and tree positions — identical every render and every session, so a hole looks "designed" and permanent without any sim change. This is the seam where "real later" can replace client synthesis with server data behind the same render.

**Decision: an additive `playingHole(...)` read query returning a `PlayingHole` DTO.** It carries the load-bearing geometry (`par`, `length`, `fairwayHalfWidth`, `greenHalfWidth`, `greenDepth`, `elevationDelta`, hazard booleans), the active round's `pinLateral` (and pin depth), `courseType` for the biome kit, and `layoutSeed`. It maps from engine records at the app edge (no engine type in the schema, per `graphql-api`), is read-only, and adds no mutation. Optionally it also projects the reachable `ShotZoneProfile` so the shot-preview overlay can show *truthful* reachable bands rather than invented ones.

**Decision: biome kit mapping, `EnvironmentClassification` as authority.** PARKLAND→Parkland, LINKS→Links, DESERT→Desert, MOUNTAIN→Alpine, WOODLAND→Heathland, and **COASTAL→Tropical/wetland** (water-heavy; LINKS already covers the seaside look). This resolves the vocabulary collision (the enum has no TROPICAL) deterministically, using the Game-Style↔Environment↔SVG-theme table in `docs/courses.txt`, and keeps all six templates in use.

**Decision: broadcast flyover → schematic for photo/graphic coexistence.** The scene photo becomes a brief per-hole intro card ("Hole 4 · Par 4 · 410y") that recedes; the parametric schematic is the persistent play surface (the "match-engine" model). The photo keeps its identity role without competing during play; the end-of-event scene screen is unchanged.

**Decision: marquee events keep a permanent venue.** In schedule generation, majors and tour championships select their course by a **stable key** (e.g. a fixed per-marquee index into the pool) rather than the running `nextTournamentId`, so the same named event returns to the same course every season. Regular and signature events keep the rotating assignment. Deterministic from the world seed; the fix is confined to the allocation step and changes no course data.

## Risks / Trade-offs

- **A cosmetic hazard could contradict a real outcome** (ball "lands in water" where grass was drawn) → resolved by the fidelity rule above: hazard outcomes are drawn into the rendered hazard, so the picture can never disagree with the surface, distance, or penalty.
- **Marquee venue change shifts which course a named event plays** → this is the intended fix; it is deterministic and reproducible, touches only allocation, and leaves the pool and all course data unchanged. Any test asserting a specific course index for a marquee event is updated; scoring is venue-agnostic so results direction is unaffected.
- **Parametric art may fall short of the hand-authored templates** → the generator inherits the templates' `<defs>` and palettes, and green-contour / angled-stripe polish is deferred, not blocking; the schematic is legible and on-brand from day one.
- **Geometry query drift from the resolver's reality** → the DTO reads the same `GeneratedHole`/`pinFor` the resolver uses; projecting the reachable `ShotZoneProfile` (rather than re-deriving widths on the client) keeps the preview honest.
- **New frontend surface area in a non-stock Next.js** → follow `docs/frontend` boundaries and the existing play components; the render is self-contained SVG with no new runtime deps.
