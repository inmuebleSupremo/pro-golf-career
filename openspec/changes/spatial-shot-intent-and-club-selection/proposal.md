# Spatial Shot Intent and Club Selection

## Why

Manual play currently sends a broad club, a target distance/lateral offset, and a conservative/balanced/aggressive strategy. The resolver then selects important spatial direction internally through `ShotAim`. This prevents the player from expressing where they intend to hit the ball and makes human and AI play use different decision contracts.

## What Changes

- Add immutable `ShotIntent`, initially `BallStrikeIntent(ClubId, AimPoint)`.
- Define absolute canonical `AimPoint(x, y)` for intended carry/first contact.
- Add a static individual-club catalogue mapped to existing equipment families.
- Replace manual strategy/distance/lateral controls with canonical SVG point selection, supplied defaults, reach guidance, and an anti-abuse aim envelope.
- Make AI strategy an internal policy which emits the same intent; narrow `ShotAim` to AI/guidance only.
- Replace public GraphQL shot decision input with intent plus stale-shot revision protection.
- Retire `ShotDecision`, `targetDistance`, `targetLateral`, and human strategy after gates.

## Non-goals

This does not add shot types, short-game overhaul, shot shape, trajectory control, directional wind, bounce/roll, flight-terrain intersection, forced carries, putting redesign, equipment-progression redesign, or a major animation rewrite.

## Future Sequence

1. `authoritative-shot-trace-foundation`
2. `surface-aware-shot-families-and-short-game`
3. `trajectory-shape-and-wind-control`
4. `flight-terrain-response`
5. `spatial-green-and-putting`

## Impact

Affected capabilities: `shot-resolution`, `playable-round`, `strategic-hole-routing`, `graphql-api`, `web-play`, `web-hole-visualization`, and `save-persistence`.

This is specification work only. The later implementation will affect pure simulation seams, strategy policy, GraphQL/controllers, manual-play UI, and calibration tests.
