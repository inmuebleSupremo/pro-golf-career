## Context

The GraphQL layer wraps `WorldService`; equipment purchases already defer to the player (`buyEquipment`, which auto-equips), but switching to a previously-owned item (`selectLoadoutItem`) was left unwrapped because it needs an owned-item handle and a read surface. The engine already exposes `equipmentInventoryOf(golferId)` (with `all()`), `tournamentLoadoutOf(golferId)` (a category→item selection), and `selectLoadoutItem(EquipmentItem)` (validates `owns` and swaps the category's slot). The only missing pieces are app-layer read accessors and an item-addressing scheme.

## Goals / Non-Goals

**Goals:**
- Expose the player's owned equipment and current loadout as queries.
- Make `selectLoadoutItem` usable over GraphQL with a natural, stable item handle.
- No engine change; reuse the existing DTO and error classification.

**Non-Goals:**
- Adding an id to `EquipmentItem` or otherwise changing the engine.
- A per-category "available items" grouping or an `equipped` flag on owned items (the client derives equipped state by diffing `playerLoadout` against `playerEquipment`).
- Auth / ownership scoping (the next step).

## Decisions

**D1 — Address an owned item by `category` + `name`.** The read DTO (`EquipmentItem`) already carries both; upgrade names carry a random suffix and the free item is `Standard <CATEGORY>`, so category+name is practically unique within a category. The `selectLoadoutItem` mutation takes `(category, name)`, and the app layer resolves it to the owned `EquipmentItem`. *Alternative:* introduce a synthetic item id — rejected: it would require an engine change (items are value records with no identity) or an app-side id registry to keep stable across requests; category+name is stable and already surfaced. *Alternative:* accept the full `EquipmentItem` as an input object — rejected: brittle (the client must echo exact quality/characteristics/cost for the `owns` equality check to pass) and leaks the item's full shape into an input.

**D2 — Resolution lives in `WorldService`, not the resolver.** A `selectLoadoutItem(sessionId, EquipmentCategory, String name)` overload looks the item up in the player's inventory (`equipmentInventoryOf(playerId).itemsIn(category)`, match by name) and delegates to the engine's `selectLoadoutItem(item)`; a miss throws `IllegalArgumentException` (→ `BAD_REQUEST`). This keeps the resolver a thin delegator and the lookup testable without the web layer. The existing full-item `selectLoadoutItem(sessionId, EquipmentItem)` primitive is retained.

**D3 — Two flat queries mirroring the read model.** `playerEquipment(id)` returns all owned items; `playerLoadout(id)` returns the seven currently-selected items (the loadout's `selection().values()`). Both project to the existing `EquipmentItem` DTO and return empty when no player is assigned (guarded by `hasPlayer`, like the other player-scoped reads). *Alternative:* one query returning owned items each tagged `equipped` — rejected for this slice as a richer DTO; two symmetric lists are simpler and the client diffs them.

## Risks / Trade-offs

- **Category+name collision within a category** (two owned items, same name) → astronomically unlikely (random suffix; at most one purchase per category per season) and, if it ever happened, both would be equally valid selections of the same category; the app resolves to the first match. Documented in the proposal.
- **`playerLoadout` returns items in `EnumMap` (category-declaration) order** → deterministic and fine; the DTO carries the category, so order is not load-bearing for the client.
- **No `equipped` flag** → the client computes it by diffing the two lists; acceptable and keeps the DTO unchanged. Can be added later without breaking the schema.
