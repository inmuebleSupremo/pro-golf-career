## Why

You can't see your own golfer anywhere. No GraphQL query exposes the player's identity, attributes, ranking, or earnings, so the career hub just says "Season 3" and the play leaderboard can't highlight you. This makes the career feel anonymous. This slice exposes a **player profile** and wires it into the hub and the play leaderboard so the career is visibly *yours*.

## What Changes

- **Backend**: add a `playerProfile(id): PlayerProfile` GraphQL query returning the player's golfer id, name, nationality, age, archetype, world ranking, career earnings and bank balance, current tour, career stats (events / wins / top-10s), and per-attribute values. Owner-scoped; null when no player is assigned.
- **Frontend**: render a golfer **header** on the career hub (name, nationality, age, archetype, world ranking, career earnings, and the attribute spread), and **highlight the player's row** on the play leaderboard by matching the profile's golfer id.

## Capabilities

### New Capabilities
- `player-profile-api`: The GraphQL query exposing the player golfer's identity, attributes, world ranking, earnings, tour, and career stats — the read the client needs to show whose career this is.
- `web-player-profile`: The frontend golfer header on the career hub and the player-row highlight on the play leaderboard, driven by the profile query.

## Impact

- **Backend**: new `PlayerProfileDto` + `AttributeValueDto`, a `playerProfile` schema type/query, a `WorldService.playerProfile` aggregation over existing world reads (career/identity/attributes/ranking/finances/stats), and a guarded resolver. No engine change (read-only aggregation). A backend test.
- **Frontend**: a `PlayerProfile` typed operation, a hub header component, and a leaderboard highlight. No new dependencies.
- **Docs**: `docs/frontend/*` governs the UI; realised without modification.
