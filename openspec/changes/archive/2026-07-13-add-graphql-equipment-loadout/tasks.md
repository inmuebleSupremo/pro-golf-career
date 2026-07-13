## 1. WorldService accessors

- [x] 1.1 Add `playerEquipment(sessionId)` → owned items of the player (`equipmentInventoryOf(playerId).all()`; requires a player).
- [x] 1.2 Add `playerLoadout(sessionId)` → the current loadout's selected items (`tournamentLoadoutOf(playerId).selection().values()`).
- [x] 1.3 Add `selectLoadoutItem(sessionId, EquipmentCategory category, String name)` overload: resolve the owned item by category+name and delegate to the engine's `selectLoadoutItem(item)`; throw `IllegalArgumentException` on no match. Keep the existing full-item overload.

## 2. Mapper & schema

- [x] 2.1 `ApiMapper`: add `equipmentCategory(String)` name parser.
- [x] 2.2 `schema.graphqls`: add queries `playerEquipment(id): [EquipmentItem!]!` and `playerLoadout(id): [EquipmentItem!]!`, and mutation `selectLoadoutItem(id, category, name): Boolean!` (document category/name addressing).

## 3. Resolvers

- [x] 3.1 `WorldQueryController`: `playerEquipment` and `playerLoadout` `@QueryMapping`s (guard on `hasPlayer` → empty list), projecting via `ApiMapper.equipment`.
- [x] 3.2 `PlayerMutationController`: `selectLoadoutItem` `@MutationMapping` delegating to the new `WorldService` overload (parse category name), returning `true`.

## 4. Tests

- [x] 4.1 `playerEquipment`/`playerLoadout` for a created player return the standard kit (all categories); empty for a world with no player.
- [x] 4.2 Loadout swap: arrange a second owned item in a category (add an upgrade to the player's inventory), `selectLoadoutItem(category, name)` → true, and `playerLoadout` reflects the new item in that category.
- [x] 4.3 Error: `selectLoadoutItem` with an unowned category+name → BAD_REQUEST.
- [x] 4.4 `ApiBoundaryTest` still green (no `sim.*` return types); full suite green.

## 5. Verify

- [x] 5.1 `mvn test` from `backend/` — full suite green.
- [x] 5.2 Manual smoke: create world + player over GraphQL, read `playerLoadout`, select an owned item, re-read to confirm.
