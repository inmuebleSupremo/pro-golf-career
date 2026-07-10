## 1. World: pending offer state + clear-on-assign

- [x] 1.1 Add `playerPendingStaff` (List<StaffMember>) and `playerPendingEquipment` (List<EquipmentItem>) fields to `World`.
- [x] 1.2 `assignPlayer` clears both (mirrors the existing `playerPendingOffers.clear()`).

## 2. Staff: defer hiring for the player

- [x] 2.1 In `runStaffSeason`, keep salaries and under-debt release for everyone; branch the discretionary hire: `if (isPlayer(id))` generate a candidate for each currently-unfilled role via `StaffMarket.generate(role, rng)` (same isolated per-role seed as the AI path) into `playerPendingStaff` (cleared first); else keep the AI auto-hire.
- [x] 2.2 `pendingStaffOffers()` (read), `hireStaff(int index)` (require player; `HiringPolicy.canAfford` + `account.spend(STAFF_HIRING, ...)`; on success `team.hire(candidate, season)` and remove the offer), `releaseStaff(StaffRole role)` (require player; `team.release(role, currentSeason)`).

## 3. Equipment: defer purchases for the player

- [x] 3.1 In `runEquipmentSeason`, branch: `if (isPlayer(id))` generate one upgrade per category via `EquipmentCatalogue.generateUpgrade(category, rng)` (same isolated per-golfer/season seed) into `playerPendingEquipment` (cleared first); else keep the AI auto-buy.
- [x] 3.2 `pendingEquipmentOffers()` (read), `buyEquipment(int index)` (require player; `AcquisitionPolicy.canAfford` + `account.spend(EQUIPMENT_PURCHASE, ...)`; on success `inventory.add(item, season, PURCHASE)`, select into loadout, remove the offer), `selectLoadoutItem(EquipmentItem item)` (require player; require owned; `loadouts.put(id, loadouts.get(id).with(item))`).

## 4. App seam

- [x] 4.1 `WorldService`: expose `pendingStaffOffers` / `hireStaff` / `releaseStaff` and `pendingEquipmentOffers` / `buyEquipment` / `selectLoadoutItem`, delegating through `World`.

## 5. Verification

- [x] 5.1 Staff: after a season transition the player has pending staff candidates and none were auto-hired; hiring an affordable candidate adds the member and charges the account; an unaffordable hire is a no-op; releasing vacates the role and records history.
- [x] 5.2 Equipment: after a season transition the player has pending upgrades and none were auto-bought; buying an affordable upgrade adds it, charges the account, and makes it loadout-selectable; selecting an owned item sets the loadout.
- [x] 5.3 Unassigned byte-identity: a world with no designated player is byte-identical across seasons to before this change (AI staff/equipment paths untouched) — reproducibility assertion.
- [x] 5.4 Update `anIdlePlayerDoesNotPerturbTheCompetitiveWorld`: an idle assigned player fields no staff and keeps the standard loadout (the AI no longer acts for them), while the world still advances.
- [x] 5.5 Full suite + `ArchitecturePurityTest` pass; existing `WorldStaffTest`/`WorldEquipmentTest` (no player) unchanged.
- [x] 5.6 `openspec validate add-player-staff-equipment --type change --strict`.
