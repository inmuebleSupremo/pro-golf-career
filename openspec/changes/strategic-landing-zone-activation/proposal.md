## Why

V4 course generation already creates route-aware SAFE, PRIMARY, and, on risk/reward holes, AGGRESSIVE landing zones. They are useful to generation diagnostics, but they are not yet a reliable golfing decision surface. Straight routes deliberately return no progression target, the current AI mainly selects a club from distance and pin lateral, and the human UI always renders three bare aim buttons even when their coordinates describe no real alternative.

This change activates that existing V4 spatial investment without changing how a ball flies or settles. It supplies truthful, deterministic landing-zone choices only where course geometry, the current ball, playable club/family choices, and the supported ground-response model establish a meaningful trade-off. It makes positional par 4s and strategic par 5s play as positions to choose, rather than merely as coordinates to render.

## What Changes

- Add a pure, deterministic V4 strategic-target planner that produces evaluated SAFE, PRIMARY, and AGGRESSIVE *options*, rather than treating their coordinates as unconditional defaults.
- Evaluate each candidate from the current ball using existing route/landing-zone metadata, canonical terrain/hazards, legal club/family choices, reachable landing distance, and the existing bounded ground-response semantics where they are authoritative.
- Offer an option only when it is reachable, legal, materially distinct, and truthfully represents a trade-off. The planner must fall back to the established ordinary target rather than invent a risk/reward choice.
- Make the existing human target buttons concise, non-binding guidance: show only server-authored distinct options with a short server-authored explanation and suggested club/family. The player may still aim anywhere legal and freely choose club, family, and shape.
- Have AI use the same candidate planner and choose deterministically by its existing strategy/disposition. It may evaluate deterministic terrain exposure but must not inspect, sample, or consume the future random shot result.
- Preserve V1/V2 and holes without an eligible V4 spatial plan through the current strategy/aim path. Completed historical results remain untouched; future unresolved V4 strokes may use the new policy without a strategy migration.

## Capabilities

### New Capabilities

- `strategic-landing-zone-activation`: truthful, deterministic V4 landing-zone options shared by human guidance and AI planning.

### Modified Capabilities

- `strategic-hole-routing`: V4 route and landing-zone semantics become eligible shot-planning inputs instead of diagnostic-only progression metadata.
- `graphql-api`: current-shot guidance projects structured, backend-authored strategic options rather than only three unconditional points.
- `web-play`: the existing guidance controls distinguish genuine alternatives from a single ordinary target while retaining free manual play.

## Impact

- **Simulation:** a small pure planning seam sits before the existing `BallStrikeIntent` / `ShotResolver` boundary. It reuses `HoleSpatialPlan`, canonical `CourseGeometry`, club/family availability, `ShotIntent`, and current ground response; it does not duplicate flight or settlement.
- **API/UI:** `ShotGuidance` becomes an additive structured option projection. Generated frontend GraphQL types will need regeneration during implementation. The browser remains advisory and has no terrain, reach, risk, or final-ball authority.
- **Compatibility:** no generator version, save migration, course replacement, or strategy migration is required. Legacy and non-eligible V4 play retains the established default path.
- **Calibration:** this feature may change target selection and therefore scoring/hazard exposure. It requires focused fixed-fixture and existing calibration evidence, but it does not reopen rollout-distance tuning.
