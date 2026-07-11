# Design — add-world-scale

## Context

The tournament engine's cut and prize mechanisms are correct; the World fed them small-world sizes (field 40, cut 60, 70 paid) so both went inert. Scaling the world to the engine's intended size (field ~120) and sizing the cut/pay line to the field makes them real. Benchmarking confirmed the cost is comfortable, so the decision is which numbers, and how the cut and money relate.

## Decisions

### D1 — Scale the defaults, not the test config
`INITIAL_POPULATION` 160 → 640 and `FIELD_SIZE` 40 → 120 (the "Balanced" target: realistic fields, ~0.6 s/season measured). Tier fractions are relative, so they need no change; the population simply distributes into larger tiers (Elite ~51 … Development ~288). The small test `WorldConfig` (pop 40, field 20) is untouched, so test speed is unchanged and only the shipped game runs at scale.

### D2 — Size the cut to the field so it bites at any scale
The World computes `cutSize = max(1, round(field.size() × CUT_FRACTION))` (0.45) from the *actual* field (which can be below the cap for a small tier) and builds the event's `TournamentFormat` with that cut, instead of the fixed `DEFAULT_CUT_SIZE`. The engine's existing cut ("top N plus ties advance; the rest are eliminated") then trims ~55% of the field after Round 2 — at 120 or at the 20-golfer test field alike. `DEFAULT_CUT_SIZE` and `TournamentFormat.standard()` stay for the standalone tournament tests.

### D3 — The money line is the cut
`paidPositions = cutSize`, and the purse is `PrizeStructure.forEvent(tier, prestige, cutSize)`. Because finishing order ranks all made-cut golfers ahead of all missed-cut golfers, paying the top `cutSize` positions pays (essentially) the golfers who made the cut and no others — the real-golf rule that making the cut is the pay line. This replaces the previous flat `PAID_POSITIONS_FRACTION` (0.65-of-field), which paid some missed-cut golfers; `CUT_FRACTION` now drives both the cut and the pay line as a single lever. (Ties exactly at the cut line are a negligible edge — a tied 45th in a field whose cut rounds to 44 would make the cut but fall just outside the paid list; acceptable for V1.)

## Risks

- **Ripple in the small test world** — a biting cut changes finances (only made-cut earn), health (missed-cut play two rounds), and cut statistics for the test config. These are deterministic and direction-based tests still hold; any exact-value assertion is re-based. This is desirable coverage — the missed-cut and cut-streak paths are now exercised.
- **Performance at scale** — measured at ~0.6 s/season (640/120); linear in field size, no pathological growth. Advancing week-by-week (the player's normal cadence) is a fraction of that.
- **Tie-at-cut money edge** — paying exactly `cutSize` can leave a golfer tied at the cut line just outside the money; negligible and documented, not engineered around.
