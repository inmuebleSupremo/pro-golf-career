## 1. Equipment model

- [x] 1.1 Create framework-free package `com.progolf.sim.equipment` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Add `EquipmentConstants` (baseline characteristic level, upgrade quality distribution, item costs by category, forgiveness/power shot scaling, affordability buffer, equipment seed salt) as the single tunables surface.
- [x] 1.3 Define `EquipmentCategory` (DRIVER, FAIRWAY_WOODS, HYBRIDS, IRONS, WEDGES, PUTTER, GOLF_BALL), `EquipmentCharacteristics` (forgiveness, power, workability, feel; a `standard()` baseline), and an immutable `EquipmentItem` (name, category, quality, characteristics, cost).

## 2. Inventory, loadout, bag

- [x] 2.1 Define `EquipmentAcquisition` (season, category, item name, method: INITIAL/PURCHASE/SPONSORSHIP) and implement `EquipmentInventory` (owned items, `owns`, `itemsIn`, `bestIn`, `add(item, season, method)`, append-only `history()`).
- [x] 2.2 Implement `TournamentLoadout` (one item per category) with `isValid(inventory)` (all selected items owned, all categories present) and helpers to build a standard/best loadout from an inventory.
- [x] 2.3 Implement `GolfBag` derived from a loadout: active items, `isValid()` (all categories), and shot-facing aggregates `forgivenessBonus()` / `powerBonus()` (0 at baseline, positive above).

## 3. Catalogue & acquisition policy

- [x] 3.1 Implement `EquipmentCatalogue.standardItem(category)` (baseline characteristics, zero cost) and `generateUpgrade(category, rng)` (quality-scaled characteristics and cost); deterministic.
- [x] 3.2 Implement `AcquisitionPolicy.chooseUpgrade(inventory, rng)` — target the weakest category, generate a candidate, return it only if it improves on what is owned; and `canAfford(funds, item)`. Deterministic, one upgrade per season.

## 4. Shot & player wiring (modified: shot-resolution, player-entity)

- [x] 4.1 Add `equipmentForgiveness`/`equipmentPower` to `GolferState` (≥0, neutral 0) with a back-compatible two-arg constructor and neutral `fresh()`.
- [x] 4.2 In `ShotResolver`, multiply dispersion by `(1 - forgiveness)` and reach by `(1 + power)` — exactly neutral at 0.
- [x] 4.3 Add a transient equipment profile (forgiveness/power) to `PlayerState` with `setEquipment(...)`; surface it via `Player.toGolferState`.
- [x] 4.4 Add `TransactionType.EQUIPMENT_PURCHASE` (Category.EXPENSE) for the ledger.

## 5. World wiring (modified: world-progression)

- [x] 5.1 In `admit`, create a standard `EquipmentInventory` (INITIAL acquisitions per category) and a standard `TournamentLoadout`; add read-only `equipmentInventoryOf(id)` / `tournamentLoadoutOf(id)` accessors.
- [x] 5.2 In `resolveEvent`, before play, derive each competitor's `GolfBag` from their loadout, validate it, and sync its forgiveness/power bonus into `player.state().setEquipment(...)`.
- [x] 5.3 In `seasonalTransition` (per surviving golfer), run acquisition: `AcquisitionPolicy.chooseUpgrade`; if affordable, `account.spend(EQUIPMENT_PURCHASE, cost)`, add to inventory (PURCHASE), and update the loadout to use it.
- [x] 5.4 Derive the equipment seed stream from the world hierarchy with a dedicated salt (isolated from other domain streams); age/season from the calendar.

## 6. Verification

- [x] 6.1 Inventory tests: every golfer starts owning one item per category; acquisition adds items and records history; ownership is queryable; supports future expansion (multiple per category).
- [x] 6.2 Loadout/bag tests: a standard loadout is valid; a loadout with an unowned item is invalid; the derived bag references only owned equipment and covers all categories.
- [x] 6.3 Catalogue/policy tests: generation is deterministic; upgrades cost more and have stronger characteristics than standard; the policy targets the weakest category and returns an improvement; `canAfford` gates on cost.
- [x] 6.4 Shot tests: a neutral bag reproduces prior shot outcomes exactly; a stronger bag yields dispersion no larger and reach no shorter.
- [x] 6.5 Boundary test: the Equipment domain computes no shots and writes no attributes/scores/finances (REQ-214); `sim.equipment` imports only `core`.
- [x] 6.6 World tests: golfers acquire equipment over seasons paid through the account; the active bag is applied to play; ownership history preserved; two worlds with the same seed produce identical inventories and history (reproducible).
- [x] 6.7 Run `openspec validate add-equipment --type change --strict` and resolve findings.
