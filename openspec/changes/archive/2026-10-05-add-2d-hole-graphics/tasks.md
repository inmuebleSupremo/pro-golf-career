## 1. Permanent marquee venues (sim.world)

- [x] 1.1 In `World`, add a deterministic marquee→course mapping keyed off the world seed: each marquee event class (each Major by its fixed ordinal, each tour's Tour Championship by tier) resolves to a stable `coursePool` index that does not depend on `nextTournamentId`. Reproducible from `masterSeed`, constant across seasons. — `marqueeCourseIndex(prestige, key)` via `Seeds.deriveSeed(masterSeed ^ MARQUEE_VENUE_SALT, …)`.
- [x] 1.2 In `generateStructuredSchedule` and `generateProportionalSchedule`, use the stable index for `MAJOR` and `TOUR_CHAMPIONSHIP` prestige; keep the `nextTournamentId % coursePool.size()` rotation for `REGULAR` and `SIGNATURE`. — structured switch keys majors on `p.week()` (fixed MAJOR_WEEKS) and championships on `tier.ordinal()`; proportional majors key on ordinal `m`. `nextTournamentId` still advances per event, so regular/signature indices are byte-identical.
- [x] 1.3 Confirm nothing else reads the marquee `courseIndex` assuming rotation (schedule archive, snapshot/restore paths); the mapping is derivable, not stored — no snapshot field change. — schedule is snapshotted/restored as generated values and archives keep their own; no code assumes rotation.

## 2. Playing-hole geometry query (app.api)

- [x] 2.1 Add `dto/PlayingHoleDto` (record): `holeNumber, par, length, fairwayHalfWidth, greenHalfWidth, greenDepth, elevationDelta, hasGreensideBunker, hasWater, hasTrees, pinLateral, pinDepth, courseType, layoutSeed` (String).
- [x] 2.2 In `ApiMapper`, map from the pending event's `GeneratedHole` + the active round's `PinPosition` (via `pinFor(round, setup)` using the **event's** `CourseSetup`, so the pin matches the played hole). `layoutSeed = Long.toString(hole.holeSeed())`; `courseType` from the host course classification (same token the scene uses). — `ApiMapper.playingHole(...)`; `PlayableEvent` exposes `currentHole()`/`currentPin()`/`holeGeometry(n)`/`pinAt(n)`/`classification()`, pin uses the live round (playoff round during a playoff).
- [x] 2.3 In `WorldQueryController`, add a `playingHole` query for the session's pending event (defaults to the current hole/round; accepts an explicit hole number for pre-fetch). Reject cleanly when no event is pending. — resolver + `WorldService.currentPlayingHole(owner, id, Integer hole)`; null when no pending event or event played to the end.
- [x] 2.4 In `schema.graphqls`, add the `PlayingHole` type and the `playingHole` query field; keep the return type a DTO (no engine record in the schema).

## 3. Reachable surfaces on the shot situation (app.api)

- [x] 3.1 Add `dto/SurfaceBandDto` (`startDistance, endDistance, regions`) + `dto/SurfaceRegionDto` (`surface` String, `halfWidth` cumulative). Add `reachable: [SurfaceBand!]!` to `dto/ShotSituationDto` and the `ShotSituation` type in `schema.graphqls` (+ `SurfaceBand`/`SurfaceRegion` types). — kept the faithful 2-level shape (distance bands × lateral regions) rather than flattening, so it mirrors the resolver's `ShotZoneProfile` exactly.
- [x] 3.2 In `ApiMapper`, project `ShotSituation.reachable()` (`ShotZoneProfile`) into the ordered band DTOs — the same profile the resolver uses: each `ZoneBand` → `SurfaceBandDto` with its `LateralRegion`s → `SurfaceRegionDto(surface.name(), outerHalfWidth)`. No engine type on the schema.

## 4. Backend tests

- [x] 4.1 Marquee-venue test: the same Major (and a Tour Championship) resolves to the same `courseIndex` across two seasons; two worlds with the same seed agree; a Regular event's index may differ across seasons. — `WorldMarqueeVenueTest` (majors by fixed week, championships by tier; across-season + same-seed reproducibility).
- [x] 4.2 `playingHole` mapping test: DTO carries the hole's geometry and hazard flags; pin lateral reflects the active round (differs across rounds while dimensions/flags stay constant); `layoutSeed` is stable for the same hole across two sessions of the same seed. — `ApiPlayingHoleMapperTest`.
- [x] 4.3 Reachable-surfaces test: the projected bands match the engine `ShotZoneProfile` for the shot (same surfaces, same order), and are non-empty within the shot's reach range. — `ApiPlayingHoleMapperTest.reachableSurfacesMirrorTheResolverProfile` (band-for-band, region-for-region, contiguity).
- [x] 4.4 Schema-isolation guard still passes: no engine record (`GeneratedHole`, `PinPosition`, `ShotZoneProfile`) appears as a GraphQL type. — existing `ApiBoundaryTest` (reflection guard) auto-covers the new resolver; green.

**Full backend suite: 606 tests, 0 failures** — regression harnesses (WorldScoringRealism, WorldPlayerSuccess, WorldSnapshotRoundTrip, WorldMajors, …) all green.

## 5. Frontend API binding (lib/api/play.ts)

- [x] 5.1 Add the `playingHole` query + TS types (`PlayingHole`, mirroring the DTO). Add a `usePlayingHole` hook alongside the existing play hooks. — `PlayingHoleDocument` in `operations.ts` (with optional `$hole`), `usePlayingHole(id, hole?)` in `lib/api/play.ts` keyed by hole + invalidated with play mutations; codegen regenerated, `tsc` clean.
- [x] 5.2 Extend the `ShotSituation` type + query with `reachable: SurfaceBand[]`. — added `reachable { startDistance endDistance regions { surface halfWidth } }` to `currentSituation` in `PlayStateDocument`.

## 6. Biome style kits (lib/play/biomes.ts)

- [x] 6.1 Define a `BiomeKit` (palette: out/rough/fairway/mow-stripes/green/fringe/sand; hazard + vegetation styling; `<defs>` symbol set) for the six themes. — `BIOME_KITS` in `lib/play/biomes.ts`; full palette + potBunkers/waste/rock/elevation flags + vegetation kind & density. Illustration palette centralized here (separate from UI tokens, per boundaries).
- [x] 6.2 Port the reusable `<defs>` from `docs/course-holes/*.svg` — deciduous canopy + `forest-shadow`, mountain `pine`, tropical `palm` + water drop-shadow + mower-stripes, links `fescue-texture` + pot-bunker style, desert waste + white bunkers — as inline SVG symbol/pattern/filter fragments. — foliage symbol vector data ported as structured `VEG_SYMBOLS` (+ `SYMBOL_SCALE`); mower-stripe pairs, fescue flag, pot-bunker/waste/rock flags live on the kit. Filters/patterns (`canopyShadow`, `waterShadow`, fescue, mow) are assembled from this data by the component in §8 (keeps `lib/` JSX-free like `scene.ts`).
- [x] 6.3 `resolveBiome(courseType)`: PARKLAND→parkland, LINKS→links, DESERT→desert, MOUNTAIN→alpine, WOODLAND→heathland, **COASTAL→tropical**; default parkland. (Supersedes the ad-hoc mapping in `scene.ts` for the schematic.) — done; `scene.ts` left untouched (it maps the backdrop photo; biomes maps the schematic — intentionally different for COASTAL).

## 7. Hole geometry + fidelity layer (lib/play/hole-geometry.ts)

- [x] 7.1 Seeded PRNG (mulberry32) keyed on `layoutSeed`; synthesize stable cosmetic layout — hazard flank(s), dogleg lean, hazard longitudinal position, tree scatter — from it (identical every render). — `mulberry32` + `seedInt` (FNV hash of the string seed) in `lib/play/hole-geometry.ts`; `projectHole` derives flank/dogleg/tree scatter deterministically.
- [x] 7.2 Yard→SVG projection (length/widths/green → path geometry, tee-bottom → green-top), pin placed on the **real** `pinLateral` side. — `projectHole` → `HoleLayout` (centerline bezier w/ dogleg, fairway/rough widths, green ellipse, tee, pin clamped to its own green on the real side, placed water blob + bunkers + trees, alpine elevation shading).
- [x] 7.3 Ball-placement resolver implementing the fidelity rule: place a resolved shot by its real `carry`/`distanceRemaining`; when `finalSurface` is a hazard, snap the ball into the rendered hazard of that kind (whichever flank) rather than the raw signed `lateral`; non-hazard lies use the real signed `lateral`. A resolved surface always wins over synthesized surroundings. — `ballPosition(layout, shot)`: WATER→rendered water, BUNKER→nearest rendered bunker, else centerline fraction from `distanceRemaining` + real signed `lateral`.

## 8. Parametric hole component (components/play/hole-2d.tsx)

- [x] 8.1 Render the hole SVG from `PlayingHole` + the biome kit: rough corridor, fairway (+ mower stripes where the kit stripes), green + fringe, tee, hazards (water/bunkers per flags & synth placement), vegetation, and the pin on its real side. Inline the kit's `<defs>`. — `components/play/hole-2d.tsx` (`Hole2d`); assembles filters/patterns/veg symbols from biome data, `useId`-namespaced ids; also renders rock outcrops (alpine), waste flash (desert), elevation shading.
- [x] 8.2 Overlay the reachable surface bands (from the shot situation) as the truthful shot-preview aid. — `reach` prop → `reachStretch` maps the carry window to a longitudinal landing-stretch overlay along the fairway (honest, claims no lateral sides; richer per-surface lateral tint deferred to avoid a sidedness lie).
- [x] 8.3 Responsive + legible: scales within the play panel; caption (hole/par/length) legible on any biome; respects `docs/frontend` boundaries and tokens. — SVG uses `viewBox`+`preserveAspectRatio` and a parent-supplied `className` for sizing; `aria-label` set; illustration colours all from the biome kit (no hardcoded colours beyond the ported template art), chrome/caption handled by the parent in §10.

## 9. Shot playback (in hole-2d.tsx or components/play/shot-playback.tsx)

- [x] 9.1 Animate the ball from origin to the ball-placement resolver's resting point when a `ShotOutcome` arrives: flight arc + trail, landing marker, and a surface reaction (splash/sand/roll). Height cue + bounce/roll are optional polish. — in `hole-2d.tsx`: on a new `ball`, animate from the previous rest (or tee) along a bowed arc (SMIL `animateMotion` + drawing trail + ball scale height cue), then a surface-coloured reaction ring; ball settles at rest. Hole change resets the origin; `usePrefersReducedMotion` skips animation and places the ball directly.

## 10. Flyover intro + Play Event integration (components/play/hole-flyover.tsx, play-event.tsx)

- [x] 10.1 `HoleFlyover`: a brief per-hole intro card (number · par · length + the event scene image) that recedes to reveal the schematic. — `components/play/hole-flyover.tsx`; scene photo + hole identity, auto-dismiss (1.6s) or click, reduced-motion instant, re-shows per hole (render-phase reset).
- [x] 10.2 Integrate into `play-event.tsx`: on reaching a new hole, show the flyover then the `Hole2d` schematic as the persistent play surface; keep the existing club/target/strategy controls (2D is output-only). Demote the scene banner to the flyover; leave the end-of-event scene screen unchanged. — new `HoleStage` (Hole2d + flyover) atop the left column; `usePlayingHole(id)` feeds geometry, situation feeds the reach hint; latest shot lifted to `PlayEvent` (`lastShot`) and fed as `ball` (Play shot + Sim shot both animate); cleared on hole change (render-phase). `EventHero` kept as the one-time identity/nav header (photo's identity role); end-of-event scene screen untouched. tsc + eslint clean.

## 11. Verify

- [x] 11.1 Full backend suite green (`mvn test`); marquee-venue, playing-hole, and reachable-surface tests pass; existing shot-engine/world regression harnesses unchanged. — **606 tests, 0 failures** (exit 0), re-run as the final gate.
- [x] 11.2 Frontend typecheck/build clean; verify the rendering across ≥2 biomes (incl. a water hole and a par 3): layout, pin on the correct side, hazard outcomes shown in a hazard, ball playback. — `tsc` + `pnpm build` clean. Verified live via an isolated render page (temp, removed; `PUBLIC_PATHS` revert restored) across all 5 biomes: SVG structure correct per biome, **COASTAL→tropical** (palms+water), links fescue / desert yucca, pins on the correct side, **ball-fidelity snap exact** (WATER→water anchor x=158.35 match; BUNKER→bunker (128.51,303) exact; FAIRWAY placed along centerline), playback animated, **no console errors**.
  - ⚠ NOT done: the authenticated in-app play-through (register/login/advance-to-event) — I do not create accounts or enter passwords. Backend requires auth (401 unauth), so the end-to-end `playingHole` query + flyover-in-context + in-page playback is left for the user to drive in the running app. A pixel screenshot also couldn't be captured (Browser pane not displayed); verification was structural (DOM/coordinate assertions).
