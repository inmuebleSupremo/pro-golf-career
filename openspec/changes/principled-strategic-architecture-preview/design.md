## Context

Existing retained course versions are compatibility commitments. The V5/V7 architectural planner contains named routing-form selection and the V3 strategic route is not always the exact route used to compile V5 geometry. The experiment must not change either behavior while it investigates a shared geometry-to-decision seam.

## Decisions

### Keep the experiment outside the version registry

The preview has a package-private generator entry point that no `World` or persistence path invokes. It uses the same course-coordinate seed hierarchy and fixed test corpus, but does not add a generator version, alter current-version selection, or enter a snapshot. Existing V1–V7 generation remains on its retained paths.

### Derive structure before terrain, then re-measure the resulting terrain

The preview derives a connected V5 route, corridor widths, and landing-zone intent from hole length, par, width and deterministic continuous draws. It uses the existing V5 canonical terrain compiler and accepted hazard constraints. The shared `StrategicTargetPlanner` re-evaluates only ordinary, reachable targets in the emitted geometry; it does not promote a zone merely because preview metadata named it.

### Review the whole corpus through the real renderer

The fixture exporter serializes all 18 holes for every fixed course, including canonical polygons, setup-aware cups, and shared-planner options. The development route offers every course and a decision-only view, so it cannot silently substitute curated examples for the full batch. The last half of each biome corpus is marked unseen.

### Compare retained and preview output honestly

The fixture now exports retained Generator 7 and experiment output from the exact same coordinate side by side. It labels the experiment as a non-persistent V7/V5 compiler path rather than a generator version. Course-level measures distinguish straightforward par 4/5 holes from material turns using route distance, heading change, and chord offset; they also report fairway widths, bunker grouping, water, trees, and a coarse repeated-structure diagnostic. These are review evidence, not new gameplay scoring rules.

### This iteration does not claim shoreline geography

The Parkland/Links preview improves local routing, bunker complexes and Parkland tree fields only. Existing local side-water shapes are not presented as coherent coastline geography. Coastlines, coves, bays, and broader course-scale landscape remain separately outstanding and are not addressed by this change.

## Risks / Trade-offs

- This is a small architecture experiment, not a replacement course planner; output may expose weak or repetitive cases.
- Fixture data is intentionally large because it is canonical geometry from every hole, not frontend-created terrain.
- The preview uses a fixed review golfer and calm weather to make target evidence comparable. It does not assert player-facing strategy quality across all conditions.

## Deferred

- A generator-version proposal, persistence design, career exposure, broad calibration, course realism claims, and V6-like landscape placement all require a separate approved change.
