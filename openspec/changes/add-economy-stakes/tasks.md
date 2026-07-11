## 1. Tier purse scaling

- [x] 1.1 Add tier purse multipliers to `TournamentConstants` (`PURSE_DEVELOPMENT` 0.08, `PURSE_STANDARD` 0.22, `PURSE_PREMIER` 0.50, `PURSE_ELITE` 1.00) and `PAID_POSITIONS_FRACTION` (0.65).
- [x] 1.2 `Tier`: add `purseMultiplier()` reading the constants.
- [x] 1.3 `PrizeStructure.forEvent(Tier tier, EventPrestige prestige, int paidPositions)` = `TOP_PRIZE × tier.purseMultiplier() × prestige.purseWeight()`, standard decay. Leave `standard()` / `standard(prestige)` unchanged.

## 2. World builds the tier/field-aware purse

- [x] 2.1 `World.buildEvent`: `paidPositions = round(config.fieldSize() × PAID_POSITIONS_FRACTION)`; build the definition with `PrizeStructure.forEvent(tier, event.prestige(), paidPositions)` instead of `standard(prestige)`.

## 3. Verification

- [x] 3.1 Purse ordering: `forEvent` purses are ordered Development < Standard < Premier < Elite for the same prestige; a major (Elite × MAJOR) exceeds a regular Elite; `standard()` still equals `TOP_PRIZE`.
- [x] 3.2 Money cut: with a field larger than the paid positions, a tail finisher's prize is 0 while a top finisher's is positive.
- [x] 3.3 Scarcity in-world: over several seasons some golfers finish with funds below their starting funds (and some go negative), while top/Elite golfers are far ahead — the earnings spread is wide and money is no longer universal.
- [x] 3.4 Reproducibility: two same-seed worlds remain byte-identical in financial state; existing economy/staff/equipment world tests pass (direction-based), re-basing any golden value.
- [x] 3.5 Full suite + `ArchitecturePurityTest` pass.
- [x] 3.6 `openspec validate add-economy-stakes --type change --strict`.
