## Why

Under hard course setups, the current pin generator scales independent rectangular offsets while canonical terrain is separately narrowed and, for V3/V4, rotated. A generated cup can consequently classify as fringe, rough, fairway, or bunker instead of GREEN. That breaks the authoritative surface contract and can strand a ball near the cup on a lie from which the dedicated putting route is unavailable.

## What Changes

- Introduce a versioned pin-placement policy, independent of `courseGeneratorVersion`: `LEGACY_V1` preserves historical behaviour and `V5_EFFECTIVE_GREEN` produces valid effective-green cups.
- Require every V5 active cup to be GREEN in the setup-specific canonical geometry and at least 2.0 yards from the GREEN polygon boundary, measured by true Euclidean segment distance.
- Preserve the existing two deterministic pin-intent draws, but interpret their depth/lateral intent in a green-local frame and use bounded deterministic projection into the eligible inset region.
- Pin the selected policy to each scheduled/tournament event. New careers default to V5; existing saves deserialize to legacy policy until an explicit clean-boundary migration adopts V5 for future unstarted events.
- Expose one narrow authenticated migration action and a small existing-career management affordance; do not redesign save management or the career UI.

## Capabilities

### Modified Capabilities

- `course-generation`: round pins become policy-versioned, effective-green-valid placements while retaining exact legacy pin reproduction.
- `course-setup`: every setup-specific cup must use the same effective geometry as surface settlement.
- `tournament-play`: a tournament pins its selected policy for its entire lifecycle.
- `course-persistence`, `world-snapshot`, and `save-persistence`: preserve policy provenance and support an explicit future-event-only migration.
- `graphql-api`: adds a narrow authenticated policy-adoption mutation and status projection.

## Impact

- **Simulation:** pure deterministic geometry-aware pin placement plus policy selection at the course/tournament seam; no changes to putting, short game, physics, green shape, or setup constants.
- **Persistence:** a small policy/default/schedule provenance addition; existing saves receive legacy defaults.
- **Application/UI:** an owner-scoped migration command and minimal discoverable control on an existing career or save-management surface.
- **Calibration:** V5 requires new validity and scoring baselines; V1–V4 fixtures remain exact regressions.

## Non-Goals

- Spatial putting, contours, slopes, spin, bounce, rollout, or green redesign.
- Changing tournament setup factors, width scaling, shot caps, hole-out semantics, or WaterDrop behaviour.
- Silent save migration, changing completed results, or mutating an active event.
