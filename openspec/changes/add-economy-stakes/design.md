# Design — add-economy-stakes

## Context

Entry/travel/salary costs are tier-scaled, but the purse is flat and pays 70 positions into a 40-golfer field, so income dwarfs cost everywhere and never runs out. The fix is two calibration levers — scale the purse by tour tier, and pay only part of the field — applied where the World builds the event, leaving the standalone `PrizeStructure` helpers (and the tests that use them) untouched.

## Decisions

### D1 — Purse scales by tour tier × event prestige
`Tier` gains `purseMultiplier()` (Development `0.08`, Standard `0.22`, Premier `0.50`, Elite `1.00`, on the `TournamentConstants` tunables surface). The World's purse becomes `TOP_PRIZE × tier.purseMultiplier() × prestige.purseWeight()`. This composes with the existing prestige scaling (a major is Elite × MAJOR = the largest purse) and directly realises "financial progression reflects success": climbing tiers is how you unlock real money. Entry fees are already tier-scaled, so the *net* of a Development week is now small or negative unless you contend.

### D2 — A money cut: only part of the field is paid
The World builds `paidPositions = round(fieldSize × PAID_POSITIONS_FRACTION)` (`0.65`), so with a 40-golfer field ~26 are paid and the tail earns nothing. This is the "out of the money" that makes a bad week cost you (entry + travel spent, nothing earned). It is distinct from the play cut (who plays the weekend), which stays inert for now — the money cut alone creates the scarcity the budget decisions need.

### D3 — Isolate the blast radius: only the World changes
`PrizeStructure.standard()` and `standard(prestige)` keep their exact TOP_PRIZE / 70-position curve, so the tournament-, playable-event-, and weather-integration tests that build events directly are byte-identical. A new `PrizeStructure.forEvent(tier, prestige, paidPositions)` factory is used **only** by `World.buildEvent`. This keeps the change to the *lived world economy* and out of the standalone engine tests.

## Risks

- **Too harsh at the bottom** — Development golfers could spiral into deep debt. Debt is already permitted with no forced consequence, so this is survivable (and realistic for a mini-tour grind); the player escapes it by performing and being promoted. Magnitudes are isolated tunables; tests assert direction, not exact balances.
- **AI-spending ripple breaks a golden test** — expected; lower-tier golfers afford less, so multi-season world state shifts. Economy/staff/equipment world tests assert *direction* (someone earns, someone hires, spreads widen) and reproducibility, which still hold; any exact-value assertion is re-based.
- **Accidentally scaling `standard()`** — avoided by adding a separate `forEvent` factory and leaving `standard*` intact; a test asserts `standard()` still equals TOP_PRIZE and that tier purses are ordered Development < Standard < Premier < Elite.
