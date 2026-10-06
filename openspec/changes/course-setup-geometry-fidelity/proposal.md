## Why

The resolver uses setup-specific canonical geometry, but the playing-hole API currently projects the base `GeneratedHole` geometry. A non-neutral width setup can therefore show terrain that differs from the terrain that classifies the player's shot.

## What Changes

- Project the active event setup's effective canonical geometry, rather than base geometry, through `PlayingHole`.
- Keep the neutral/default setup byte-for-byte equivalent to the existing base geometry and avoid storing duplicate geometry.
- Add API, rendering, and simulation regression coverage for setup-specific surface agreement.
- Record generator-version/save compatibility as a deferred decision for `course-design-foundations`; do not implement it here.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `course-setup`: an event setup's effective geometry becomes the single geometry used for both resolution and projection.
- `graphql-api`: canonical `PlayingHole.geometry` represents the active effective hole geometry.
- `web-hole-visualization`: the rendered terrain must agree with the active setup as well as the base hole.

## Impact

- **Simulation/API:** `GeneratedHole`/`RoundHole`, playable-event presentation accessors, `WorldService`, and API mapping.
- **Frontend:** no new gameplay controls or terrain generation; consume the existing geometry field faithfully.
- **Tests:** deterministic default/non-default setup geometry, surface classification, GraphQL DTO, and renderer agreement.
- **Persistence:** no save format change in this correction; generator-version policy is deferred explicitly.
