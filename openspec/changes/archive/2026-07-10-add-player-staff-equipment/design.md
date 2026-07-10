# Design — add-player-staff-equipment

## Context

`add-player-control` established the seam: for the player's golfer only, the World replaces an AI policy with the human's decision, byte-identical when unassigned. Sponsorship already works this way — offers are generated as *pending* and the human accepts them between advances. Staff and equipment are the two remaining seasonal levers still run by AI even for the player (`runStaffSeason` auto-hires; `runEquipmentSeason` auto-buys). This change moves both to the sponsorship pattern.

## Decisions

### D1 — Player owns staff/equipment (deferred), consistent with sponsorship
On assignment the human takes over staff and equipment immediately, exactly as they take over sponsorship. The World generates the *options* (candidates, upgrades) and the human chooses; nothing is auto-hired or auto-bought for the player. This is the correct "the player is responsible for decisions" model (player-experience §1.1) and is consistent with sponsorship — not an "AI autopilot until the player intervenes" model, which would be inconsistent and would hide the decision.

- **Consequence:** an idle assigned player no longer auto-hires/buys, so their trajectory diverges from a fully autonomous golfer, which ripples into the wider world (one golfer's scores shift others' finishes — the same butterfly the playable-event fidelity is scoped around). The **stated** guarantee (no player assigned ⇒ byte-identical to before) is preserved because the AI paths are untouched. The emergent "idle assigned player equals autonomous" property is retired; its test is rewritten to the real invariant.
- **Alternative — keep AI autopilot for the player until they engage:** preserves the idle-equals-autonomous property but is inconsistent with sponsorship and silently makes decisions for the player. Rejected.

### D2 — Mandatory vs discretionary split preserved
`runStaffSeason` does three things: (1) charge salaries, (2) release the costliest member while in debt, (3) hire one candidate. (1) and (2) are **economic integrity**, not choices — they stay automatic for everyone including the player. Only (3), the discretionary hire, becomes the player's decision. Equipment has only the discretionary purchase, which becomes the player's. So the player controls the discretionary spends; the mandatory obligations still apply.

### D3 — Pending offers generated deterministically, regenerated each season
Mirrors sponsorship exactly. In the season transition, for the player:
- **Staff:** generate one candidate for each currently-unfilled role via `StaffMarket.generate(role, rng)`, using the same isolated per-golfer/season/role seed the AI hire path uses, and hold them as `playerPendingStaff`. (Unfilled roles only — a starting player has up to five candidates to choose among; a partly-staffed player sees the gaps.)
- **Equipment:** generate one upgrade per category via `EquipmentCatalogue.generateUpgrade(category, rng)` with the same isolated per-golfer/season seed, held as `playerPendingEquipment`.
Offers are cleared and regenerated each season (unaccepted ones lapse), and cleared on `assignPlayer`. The generation is reproducible; the choices are the human's.

### D4 — Player actions go through the existing domain operations and the ledger
- `hireStaff(index)`: if affordable (`HiringPolicy.canAfford` = funds ≥ hiring cost + first salary) spend `STAFF_HIRING` and `SupportTeam.hire`; remove the offer. Rejected (unaffordably) leaves state unchanged.
- `releaseStaff(role)`: `SupportTeam.release(role, season)` — a voluntary, recorded departure.
- `buyEquipment(index)`: if affordable (`AcquisitionPolicy.canAfford`) spend `EQUIPMENT_PURCHASE`, `EquipmentInventory.add(PURCHASE)`, and select it into the loadout; remove the offer.
- `selectLoadoutItem(item)`: require the item is owned, then `loadout.with(item)` — choosing the bag from owned gear.
The World invents nothing; affordability is the economy's existing rule (`spend` declines an unaffordable discretionary charge), and effects (coach development bonus, bag shot bonuses) already flow from the resulting team/loadout at their existing seams.

## Risks

- **The retired idle-equals-autonomous invariant** — addressed by rewriting that one test to assert the true behavior (idle player's own management inert; unassigned world unchanged) and by keeping the AI paths byte-identical for unassigned worlds (covered by a reproducibility assertion).
- **Budget edge cases** — hiring/buying when unaffordable must be a no-op, not a partial state; the economy's `spend` returns false and we branch on it, and tests cover the decline path.
