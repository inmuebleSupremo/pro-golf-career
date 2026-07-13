## Why

`add-graphql-mutations` deferred `selectLoadoutItem` (switch the tournament loadout to an already-owned item): the engine identifies an item by record equality and there was no read surface exposing the player's owned equipment, so wrapping the mutation alone would have been unusable — a client had no way to name the item to equip. This change closes that gap, completing the equipment loop and the gameplay API surface before the Auth step introduces a cross-cutting concern.

## What Changes

- **Owned-equipment read queries** (keyed by session id, player-scoped): `playerEquipment` (every item the player owns, across categories) and `playerLoadout` (the item currently selected in each of the seven categories). Both return the existing `EquipmentItem` DTO; empty when no player is assigned.
- **`selectLoadoutItem` mutation**: identifies the owned item by `category` + `name` (the fields the read DTO already carries), resolves it to the player's owned `EquipmentItem` in the app layer, and equips it via the engine's existing `selectLoadoutItem`. Returns success; an unknown category+name yields a `BAD_REQUEST` error.
- **`WorldService` accessors**: `playerEquipment(sessionId)`, `playerLoadout(sessionId)`, and a `selectLoadoutItem(sessionId, category, name)` overload that does the owned-item lookup. No engine change — these delegate to the existing `World.equipmentInventoryOf` / `tournamentLoadoutOf` / `selectLoadoutItem`.
- **`ApiMapper`**: an `EquipmentCategory` name parser (reusing the enum-by-name convention).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `graphql-api`: extend with the owned-equipment read queries and the usable `selectLoadoutItem` mutation. The rest of the API surface is unchanged.

## Impact

- **Modified app-layer code**: `WorldService` (three accessors), `WorldQueryController` (two queries), `PlayerMutationController` (the `selectLoadoutItem` mutation), `ApiMapper` (category parser), `schema.graphqls` (two query fields, one mutation field). The `EquipmentItem` DTO from the read slice is reused.
- **Unchanged**: the simulation core (`sim.*`), the engine's `selectLoadoutItem` semantics, and the `ArchitecturePurityTest` boundary.
- **Identity note**: an owned item is addressed by category + name; upgrade names carry a random suffix (`<CATEGORY>-Pro-<n>`) and the free item is `Standard <CATEGORY>`, so category+name is a stable, practically-unique handle within a category.
- **Tests**: `@SpringBootTest` + `GraphQlTester` covering `playerEquipment`/`playerLoadout` for a created player, a real loadout swap to a second owned item in a category, and the `BAD_REQUEST` path for an unknown item.
