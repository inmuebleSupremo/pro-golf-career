## Why

The player-control seam already lets the human own their golfer's development, resting, and sponsorship decisions — but two of the management levers the vision promises are still taken by AI policies even for the player: **staff** (hiring a coach, caddie, fitness coach, physiotherapist, sports psychologist) and **equipment** (buying club/ball upgrades and choosing the bag). Today an assigned player's staff and equipment are auto-managed behind their back; the human never chooses their team or their gear. This change transfers both to the player, through the same configure-then-advance seam sponsorship already uses, so the human makes the calls the sim then lives with (player-experience §3/§4: development, staff, equipment, finances are all player decisions). Finances come along for free — every hire and purchase is a budget decision against the player's `FinancialAccount`, which already exists and is readable.

This is the first half of "management breadth" (the two seasonal market-purchase levers, which share one rhythm: offered options → the player hires/buys within budget). Event-by-event scheduling — the weekly lever the majors set up — is the immediately following change.

## What Changes

- **Player owns staff.** For the player's golfer, the World stops auto-hiring. Instead, at each season transition it generates a **staff market** of candidates for the player's open roles (deterministically, from the same seeds the AI uses) and holds them as **pending staff offers**. The player hires a candidate (spending its hiring cost within budget) or releases a current member. Salaries and under-debt release stay mandatory (economic integrity is not a choice).
- **Player owns equipment.** For the player's golfer, the World stops auto-buying. Instead it generates **equipment upgrade offers** (one candidate per category, deterministically) held as pending, and the player buys an upgrade (within budget), which is added to the inventory. The player also sets their **tournament loadout** by selecting an owned item per category.
- `PlayerControl` is unchanged in shape — the pending staff/equipment offers are World-held generated state (exactly like the existing pending sponsorship offers). Assigning a player clears them.
- New `World` actions: `pendingStaffOffers` / `hireStaff` / `releaseStaff`, `pendingEquipmentOffers` / `buyEquipment` / `selectLoadoutItem`; exposed on `WorldService`. Reads (`supportTeamOf`, `equipmentInventoryOf`, `tournamentLoadoutOf`, `financialAccountOf`) already exist.
- **Behavioural consequence:** an assigned player's staff and equipment now defer to the human exactly as sponsorship does. An *idle* assigned player therefore no longer auto-hires or auto-buys (they field no staff and standard gear until they act), so their competitive trajectory diverges from a fully autonomous golfer. The stated guarantee — **a world with no designated player is byte-identical to before** — is preserved; the emergent "idle assigned player equals autonomous" property is intentionally retired (staff/equipment are now the player's, like sponsorship). The one test asserting that property is updated to the real invariant.

Explicitly out of scope: event-by-event scheduling (the next change — the crude rest toggle stays for now); staff *effects* and the equipment shot model are unchanged (only *who decides* moves); no new staff/equipment market mechanics (same generators, same costs); no auto-advisor/recommendation.

## Capabilities

### Modified Capabilities
- `player-control`: the player additionally manages their golfer's staff (hire from a generated market / release) and equipment (buy generated upgrades / set the loadout), deferred from the AI to the human like sponsorship; every hire and purchase is a budget decision.
- `world-progression`: when a world has a designated player, the World additionally defers that player's staff hiring and equipment purchases to the player's pending offers (while keeping mandatory salaries and debt-release), using the automatic paths for all other golfers.

## Impact

- **Codebase**: `World` (player branch in `runStaffSeason`/`runEquipmentSeason`, pending-offer state, new player actions, clear-on-assign), `WorldService` (expose the actions). No changes to the staff/equipment/economy domains themselves — the same generators, policies' affordability checks, and ledger are reused.
- **Determinism**: offered candidates/upgrades are generated from the same isolated per-golfer/season/role seeds the AI uses, so the market is reproducible; the player's hires/buys are their own deterministic choices. A world with no player is byte-identical to before (the AI paths are untouched).
- **Tests**: the `anIdlePlayerDoesNotPerturbTheCompetitiveWorld` test is updated to assert the real invariant (an idle player's own staff/equipment are inert — no staff, standard loadout — and the AI no longer acts for them), plus new tests for hiring/releasing, buying, loadout selection, budget enforcement, and unassigned byte-identity.
- **Boundary**: `World` stays a pure coordinator — it generates options via the staff/equipment domains' public generators and applies the player's chosen spends through the `FinancialAccount`; it invents no costs or effects.
