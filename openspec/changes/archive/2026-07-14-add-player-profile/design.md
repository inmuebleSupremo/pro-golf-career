## Context

The play/career screens exist but nothing shows the player's own golfer. The engine has all the data via existing reads:
- `world.playerGolferId()`; `world.careerOf(id)` → `age()`, `player().identity()` (`firstName/lastName/nationality/archetype`), `player().attributes()`.
- `world.currentRanking().positionOf(id)` → `Optional<Integer>` (world ranking).
- `world.financialAccountOf(id).snapshot()` → `availableFunds`, `tournamentEarnings`.
- `world.careerStatisticsOf(id)` → `StatLine` (`events`, `wins`, `topTens`, …).
- `world.tourOf(id)` → `Optional<TourTier>`.

The GraphQL edge follows the DTO pattern: `WorldQueryController` `@QueryMapping` → `WorldService` → app-layer DTO (enum fields as `.name()`), guarded by `worldService.hasPlayer(owner, id)`; `ApiBoundaryTest` forbids sim types in resolver signatures.

## Goals / Non-Goals

**Goals:** one `playerProfile` query aggregating identity/attributes/ranking/earnings/tour/stats; a hub golfer header and a play-leaderboard highlight consuming it.

**Non-Goals:** editing attributes; a full stats/records screen; other golfers' profiles; any engine change (read-only aggregation).

## Decisions

### D1 — A single aggregation query returning a profile DTO

**Decision:** `playerProfile(id: ID!): PlayerProfile` (nullable). `WorldService.playerProfile(owner, session)` builds a `PlayerProfileDto` from the world reads above; the resolver returns null when `!hasPlayer`. `PlayerProfile` includes `attributes: [AttributeValue!]!` (`{ attribute, value }`) so the client can render the build without hardcoding attribute names. World ranking is nullable (unranked golfers).

**Why:** the hub and leaderboard need this data together; one query is one round trip and one coherent shape. Building the DTO in `WorldService` matches the existing `WorldStatusDto` precedent and keeps the resolver thin. Attributes as name/value pairs avoid coupling the schema to the attribute enum.

### D2 — Leaderboard highlight by golfer id

**Decision:** the play surface already reads `eventLeaderboard { golfer { id } }`; fetch `playerProfile.golferId` and highlight the row whose `golfer.id` matches. The hub header is a new section above the existing status.

**Why:** the profile's `golferId` is the missing key that lets the client identify "you" in the field — no new leaderboard field needed.

## Risks / Trade-offs

- **Profile query on a session mid-event vs between events** → all reads are valid whenever a player exists; ranking/earnings simply reflect current state. Guarded by `hasPlayer`.
- **Attribute list ordering** → return attributes in the engine's `Attribute` enum order for a stable, predictable display.
