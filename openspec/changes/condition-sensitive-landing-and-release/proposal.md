## Why

The current authoritative resolver knows exactly where a non-putting shot first contacts canonical terrain, but its post-contact response is deliberately minimal: FULL and CONTROLLED shots do not release, while PITCH and CHIP use fixed surface multipliers. Weather already derives ground firmness, yet firmness does not affect that response. As a result, a firm fairway driver and an equivalent wet-fairway driver can stop identically after landing, making club, condition, and course decisions less recognisably golf-like than the newly authoritative flight and wind systems support.

This focused change makes first-contact release visibly and deterministically sensitive to firmness, club category, shot family, and canonical landing surface. It is not a physics-engine rewrite, obstacle/collision system, or spatial-putting project.

## What Changes

- Add a pure, deterministic post-contact ground-response calculation for non-putting FULL, CONTROLLED, PITCH, and CHIP strikes that already have canonical geometry.
- Carry existing weather-derived ground firmness into the resolver-facing condition contract; retain existing wind and lie-quality behaviour.
- Make club category and shot family produce materially different, calibrated release tendencies: low-lofted long shots release more than higher-lofted approaches under comparable conditions, and PITCH/CHIP retain distinct short-game identities.
- Keep `FlightSolution.positionAt(1)` as the sole first-contact authority, then resolve a backend-owned ground release and final `BallState`; materialized `ShotTrace` exposes that same roll for playback.
- Adopt for review a bounded first-milestone boundary policy: allow at most one deterministic transition between ordinary playable canonical surfaces; clamp before a second or non-playable boundary. This avoids the current all-or-nothing same-surface stop without introducing hazard traversal or general terrain physics.
- Preserve the existing dedicated putting route. Ground firmness SHALL not alter `PuttIntent` resolution in this milestone; green-speed/contour behaviour remains later green-and-putting work.

## Capabilities

### New Capabilities

- `ground-response`: deterministic, condition-sensitive post-contact release and bounded ordinary-surface boundary handling for non-putting shots.

### Modified Capabilities

- `playing-conditions`: weather-derived ground firmness becomes an explicit resolver input while green-speed remains outside non-putting release.
- `shot-resolution`: canonical non-putting settlement gains deterministic ground response after authoritative first contact.
- `playable-round`: visible and background human/AI execution preserve equivalent ground-response outcomes.
- `web-hole-visualization`: playback presents the resolver-authored contact, release, and final position, including a permitted ordinary-surface final lie.

## Impact

- **Simulation:** `PlayingConditions`, resolver-facing conditions, `ShotExecutionProfile`, `ShotResolver`, `ShotTrace`, settlement, and the shared round/play entry points gain a bounded ground-response seam. `sim.*` remains pure and deterministic.
- **Persistence/compatibility:** no in-progress event is currently snapshotted. Completed results remain historical; a loaded career uses its already persisted conditions for future unresolved strokes. Whether exact cross-build replay requires an explicit response-policy version is an approval question recorded in the design.
- **API/UI:** only additive trace/DTO presentation work should be needed if existing roll endpoints remain sufficient. The client must not calculate release or surface crossings.
- **Calibration:** driving distance, proximity, GIR, hazard-adjacent outcomes, and scoring distributions require explicit before/after evidence; the existing calibration harnesses remain gates.
