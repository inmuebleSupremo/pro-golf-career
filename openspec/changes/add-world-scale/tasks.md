## 1. Scale the default world

- [x] 1.1 `WorldConstants`: `INITIAL_POPULATION` 160 → 640, `FIELD_SIZE` 40 → 120. (Tier fractions, events, weeks unchanged; small test config unchanged.)

## 2. A biting, field-proportional cut with the money line on it

- [x] 2.1 `TournamentConstants`: replace `PAID_POSITIONS_FRACTION` (0.65) with `CUT_FRACTION` (0.45).
- [x] 2.2 `World.buildEvent`: compute `cutSize = max(1, round(field.size() × CUT_FRACTION))`; build the event's `TournamentFormat` with that cut (`new TournamentFormat(ROUNDS, true, cutSize)`) instead of `TournamentFormat.standard()`; set `paidPositions = cutSize` and build the purse with `PrizeStructure.forEvent(tier, prestige, cutSize)`.

## 3. Verification

- [x] 3.1 Cut bites: in a world (small or default), a resolved event cuts part of the field — some competitors record a missed cut and play only two rounds (previously impossible).
- [x] 3.2 Money = cut: made-cut golfers can earn prize money; a missed-cut golfer earns nothing from that event while still paying entry/travel.
- [x] 3.3 Scale: the default world has ~640 active golfers and fields up to 120; it advances a season without error and stays reproducible (two same-seed default worlds identical for a season).
- [x] 3.4 Reproducibility + suite: existing world tests pass (direction-based), re-basing any golden value; full suite + `ArchitecturePurityTest` pass.
- [x] 3.5 `openspec validate add-world-scale --type change --strict`.
