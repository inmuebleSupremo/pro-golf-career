## Context

The shot engine resolves per `Club` and already reads the golfer's temporary `GolferState` (fatigue, pressure) — which the World syncs from the Health domain via `player.state().setFatigue(...)`. The Economy's `FinancialAccount.spend` is unused. This change adds a `sim.equipment` domain that owns each golfer's inventory, loadout, and derived bag, then wires it into the World: acquisition flows through the Economy, and the active bag's aggregate characteristics are synced into shot resolution the same way fatigue is. Locked model: weekly turn-based world, seasonal seams, deterministic AI policies.

Framework-free, Java 21 / Spring Boot 3, deterministic and analytical: the shot effect is a pure factor; the only randomness (upgrade generation, acquisition choice) draws from the world seed hierarchy (`Seeds`/`SplitMix64Rng`), never `java.util.Random`.

## Goals / Non-Goals

**Goals:**
- A persistent per-golfer Equipment Inventory across the seven categories, with ownership + acquisition history.
- A prepared Tournament Loadout and a derived Golf Bag that is always owned and valid; integrity across inventory/loadout/bag.
- The shot engine consuming the bag: forgiveness reduces dispersion, power extends reach — modest, with standard gear exactly neutral.
- Economy-integrated acquisition (deterministic purchase policy gated by affordability).
- Wire into the World deterministically; keep it reproducible and preserve existing scoring behaviour for standard gear.

**Non-Goals:**
- Per-club (rather than bag-aggregate) shot effects, condition/wear, fitting, manufacturer relationships, cosmetics, extra categories, sponsor-supplied equipment (REQ-213 future extensibility).
- Equipment computing shots, writing attributes, or handling its own financial transactions (forbidden, REQ-214).
- Human-facing loadout UI (Presentation; a deterministic policy prepares loadouts now).

## Decisions

### D1. `sim.equipment` depends only on `core`; the World applies effects
The Equipment domain owns inventory/loadout/bag and exposes plain characteristics (`double`s). It imports only `core`. The World reads a bag's aggregate forgiveness/power, syncs them into the player's transient state, and charges the `FinancialAccount` itself. *Why:* REQ-214 independence and the established pattern (economy/health/staff depend only on core; the World orchestrates). *Alternative rejected:* equipment importing shot/economy to apply its own effects — inverts the boundary.

### D2. The bag feeds shots via the existing fatigue-sync channel (bag aggregate on `GolferState`)
`GolferState` gains `equipmentForgiveness` and `equipmentPower` (both ≥ 0, neutral 0); `ShotResolver` multiplies dispersion by `(1 - forgiveness)` and reach by `(1 + power)`. `PlayerState` carries a transient equipment profile set by the World before play (like fatigue), surfaced through `Player.toGolferState`. *Why:* reuses the exact channel Health uses (REQ-206 the engine consumes the bag; REQ-225-style dependent-reads) with the smallest surface — no `ShotContext`/`RoundResolver`/`Tournament` signature changes. *Alternative rejected:* threading a per-club equipment profile through `ShotContext`→`RoundResolver`→`Tournament` — a much wider change for per-club nuance deferred to a follow-up (REQ-213). *Alternative rejected:* passing equipment through the Tournament like weather — a wider Tournament API than reusing the player→shot seam.

### D3. Standard gear is exactly neutral
Every golfer starts with standard equipment whose characteristics are the baseline (0.5), yielding a bag bonus of exactly 0 for forgiveness and power, so `(1 - 0)` and `(1 + 0)` reproduce current shot maths bit-for-bit. Only above-baseline (purchased) gear produces a positive bonus. *Why:* preserves scoring calibration and world reproducibility for the pre-equipment baseline (REQ-299). *Alternative rejected:* a non-neutral baseline — silently shifts all existing scoring.

### D4. Inventory owns items; loadout selects; bag is derived — with integrity
`EquipmentInventory` holds owned `EquipmentItem`s (multiple per category allowed) and an append-only acquisition history. `TournamentLoadout` selects one item per category and `isValid(inventory)` requires every selected item owned and all categories present. `GolfBag.fromLoadout(loadout)` derives the active items and exposes the shot-facing aggregates; it is valid only if it references owned equipment. *Why:* REQ-205/206/211 — only owned equipment, consistent across inventory/loadout/bag, always valid. *Alternative rejected:* a single flat set — loses the inventory-vs-bag distinction REQ-206 requires.

### D5. Deterministic, affordability-gated acquisition through the Economy
`AcquisitionPolicy.chooseUpgrade(inventory, rng)` targets the category with the weakest current best item, generates a candidate via `EquipmentCatalogue.generateUpgrade`, and returns it only if it improves on what is owned — at most one purchase per season (gradual). The World buys it only if affordable (`FinancialAccount.spend` with a new `EQUIPMENT_PURCHASE` type — discretionary, declined if unaffordable), then adds it to the inventory, updates the loadout to use it, and records the acquisition. *Why:* REQ-210 (acquisition integrates with the Economy), REQ-209 (ownership history). *Alternative rejected:* buying every affordable upgrade at once — no gradual identity, unbounded spend.

### D6. World wiring: inventory at admit, seasonal acquisition, bag→shot sync
`admit` creates a standard inventory (INITIAL acquisitions) and a standard loadout. In `seasonalTransition` (the existing per-golfer financial loop) the acquisition step runs through the account, deterministically seeded per golfer/season. In `resolveEvent`, before play, each competitor's active bag is derived and validated, and its forgiveness/power bonus is synced into `player.state().setEquipment(...)`. *Why:* REQ-205/210/206 and the living-world goal; REQ-299 reproducibility. *Alternative rejected:* preparing the bag inside `Tournament` — the Tournament must not own equipment (REQ-214).

### D7. Characteristics defined, forgiveness/power applied
`EquipmentCharacteristics` carries forgiveness, power, workability, and feel (REQ-207 examples); V1 applies forgiveness and power to play and keeps workability/feel as data for future use. Item characteristics scale with a summary quality. *Why:* REQ-207 defines existence, not implementation; applying two keeps the effect legible and calibratable. *Alternative rejected:* applying all four now — workability/feel need shot-shape/putting context not yet World-driven.

## Risks / Trade-offs

- **[Equipment becoming a shot actor]** → `equipment` imports only `core`; it exposes characteristics; the World syncs them through a public op the shot engine already reads; an architecture/boundary test asserts equipment computes no shots and writes no attributes/results (REQ-214).
- **[Shifting existing scoring]** → standard gear is exactly neutral (bonus 0); a test asserts the neutral bag reproduces prior shot outcomes, and the scoring-calibration and world-reproducibility tests guard the baseline.
- **[Determinism]** → acquisition/generation use an isolated seeded stream; the effect is pure; two same-seed worlds stay identical, extended to equipment state.
- **[Uncalibrated magnitudes]** → all magnitudes isolated in `EquipmentConstants`; tests assert structural properties (better gear tightens dispersion / adds reach; purchases require funds; only-owned loadouts; history preserved), not magnitudes.
- **[Integrity violations]** → the World only ever selects owned items into loadouts; `TournamentLoadout.isValid` and `GolfBag` validity are asserted; a test covers rejecting an unowned selection.
- **[Bag-aggregate simplification]** → V1 applies a bag-wide forgiveness/power rather than per-club; documented and structured so per-club effects add later without fundamental change (REQ-213).

## Migration Plan

Greenfield `sim.equipment` + additive modifications (`GolferState`/`ShotResolver`/`PlayerState`/`Player.toGolferState`; one `TransactionType`; World wiring). Sequencing: (1) `EquipmentConstants`, `EquipmentCategory`, `EquipmentCharacteristics`, `EquipmentItem`; (2) `EquipmentInventory` (+ `EquipmentAcquisition` history), `TournamentLoadout` (validity), `GolfBag` (derivation + aggregates + validity); (3) `EquipmentCatalogue` (standard + upgrades) + `AcquisitionPolicy`; (4) shot changes — `GolferState` equipment fields (neutral default + back-compat constructor), `ShotResolver` applies forgiveness/power, `PlayerState` transient + `Player.toGolferState` (modifies shot-resolution + player-entity); add `EQUIPMENT_PURCHASE`; (5) wire the World — standard inventory/loadout at `admit`, acquisition through the account, bag→shot sync in `resolveEvent`, accessors; (6) tests — inventory/ownership/history; loadout validity + bag derivation + only-owned; catalogue/policy deterministic + affordability; neutral bag reproduces prior shots and better gear tightens/extends; boundary; world acquires equipment through the Economy and stays reproducible. Each layer testable before the next.

## Open Questions

- Exact magnitudes (upgrade quality distribution, item costs by category, forgiveness/power scaling, affordability buffer) — placeholder `EquipmentConstants`, tuned later.
- Whether acquisition should also come via sponsorship (REQ-210) — purchase only in V1; the acquisition method enum already includes SPONSORSHIP for later.
- Whether the bag effect should be per-club — bag-aggregate in V1; per-club is a documented future extension (REQ-213).
- Whether ownership history folds into Career history — kept in the inventory now; a later legacy/Media change can surface it.
