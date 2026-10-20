## Context

Existing retained course versions are compatibility commitments. The V5/V7 architectural planner contains named routing-form selection and the V3 strategic route is not always the exact route used to compile V5 geometry. The accepted Generator 8 architecture closes that geometry-to-decision seam without reinterpreting those retained versions.

## Decisions

### Retain Generator 8 beside historical versions

The principles-driven implementation is registered as Generator 8. Version 6 remains unassigned to the rejected landscape prototype. Generator 8 is selected only for newly created worlds through an explicit development opt-in; it does not replace the default. Snapshot provenance remains authoritative on restore, so existing V1–V7 careers cannot be silently upgraded.

### Derive structure before terrain, then re-measure the resulting terrain

Generator 8 derives a connected V5-family route, corridor widths, and landing-zone intent from hole length, par, width and deterministic continuous draws. It uses the existing canonical terrain compiler and accepted hazard constraints. Its route-derived strategic intent is persisted with the generated hole. The shared `StrategicTargetPlanner` still re-evaluates only ordinary, reachable targets in emitted geometry; a named landing zone never makes an unavailable target actionable.

### Review the whole corpus through the real renderer

The fixture exporter serializes all 18 holes for every fixed course, including canonical polygons, setup-aware cups, and shared-planner options. The development route offers every course and a decision-only view, so it cannot silently substitute curated examples for the full batch. The last half of each biome corpus is marked unseen. Manual visual review and real-round gameplay E2E were completed and accepted as satisfactory architectural foundation evidence.

### Compare retained and preview output honestly

The fixture exports retained Generator 7 and Generator 8 output from the exact same coordinate side by side. Course-level measures distinguish straightforward par 4/5 holes from material turns using route distance, heading change, and chord offset; they also report fairway widths, bunker grouping, water, trees, and a coarse repeated-structure diagnostic. These are review evidence, not new gameplay scoring rules or a fixed template quota.

### This iteration does not claim shoreline geography

Generator 8 improves local routing, bunker complexes and Parkland tree fields only. Existing local side-water shapes are not presented as coherent coastline geography. Coastlines, coves, bays, broader course-scale landscapes, environmental interaction, putting, and other simulation features remain separately outstanding and are not addressed by this change.

## Risks / Trade-offs

- This is an accepted architectural foundation, not a final replacement course planner; output may still expose weak or repetitive cases.
- Fixture data is intentionally large because it is canonical geometry from every hole, not frontend-created terrain.
- The preview uses a fixed review golfer and calm weather to make target evidence comparable. It does not assert player-facing strategy quality across all conditions.

## Deferred

- Default adoption, broad calibration, course-realism claims, V6-like landscape placement, and the remaining simulation work all require separate approved changes.
