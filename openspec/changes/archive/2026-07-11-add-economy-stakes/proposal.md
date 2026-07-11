## Why

We just made staff and equipment real *budget-gated* player decisions — but the budget never bites. Two leaner-route defaults hollow out the whole economy:

- **The purse is flat across tours.** Every event awards the same 1,000,000 top prize regardless of tier, while entry fees *are* tier-scaled (Development 8k → Elite 40k). So a Development golfer pays a small entry and can win an Elite-sized purse — money floods in everywhere.
- **Everyone cashes.** `PAID_POSITIONS` (70) exceeds the field (40), so *every* competitor earns prize money every event. There is no "out of the money," no risk that a bad week costs you.

Result: funds only ever grow, `spend(...)` is never declined for lack of money, and the `financial-strategy` spec's own promises — *"financial progression reflects success"* and *"financial stability is not guaranteed independent of results"* — are false in practice. This change makes money **scarce and earned**, so the management spends we built carry real weight, and a golfer (especially the player, starting at the bottom) must balance ambition against solvency.

## What Changes

- **Tour-tier purse scaling.** `Tier` gains a `purseMultiplier()` (Development ≪ Elite), and the World builds each event's purse as `TOP_PRIZE × tier.purseMultiplier() × prestige.purseWeight()`. A Development event now pays a modest purse, an Elite event the full one, and a major (Elite × MAJOR) the largest — so higher competitive tiers genuinely unlock greater earnings (`financial-strategy`: Financial Progression Reflects Success).
- **A money cut.** The World pays only a subset of the field — `paidPositions = round(fieldSize × PAID_POSITIONS_FRACTION)` — so finishing in the tail earns nothing. Combined with the tier-scaled purse and the existing tier-scaled entry + travel + salary costs, a poor week (or a poor tour) can cost more than it earns (`financial-strategy`: Career Sustainability).
- New tunables in `TournamentConstants` (`PURSE_DEVELOPMENT/STANDARD/PREMIER/ELITE`, `PAID_POSITIONS_FRACTION`) and a `PrizeStructure.forEvent(tier, prestige, paidPositions)` factory. The existing `PrizeStructure.standard()` / `standard(prestige)` are **unchanged** (standalone tournament/play tests keep their exact purses); only the World switches to the tier-aware factory.

Explicitly out of scope: explicit bankruptcy consequences (retirement/relegation on insolvency — debt is already permitted and now reachable, but has no forced outcome yet); changing the field cut (the play cut is still inert — `cutSize` exceeds the field — a separate calibration item); entry-fee changes (already tier-scaled); the `Economic Independence` guarantee is untouched — money still buys opportunity, never competitive outcomes.

## Capabilities

### Modified Capabilities
- `financial-strategy`: event purses scale with tour tier (higher tiers pay more), and only a subset of the field is paid, so competing does not guarantee income and financial stability depends on results — making the economy's own success-reflecting and sustainability promises hold in practice.

## Impact

- **Codebase**: `Tier` (+`purseMultiplier()`), `TournamentConstants` (+tier purse multipliers + paid fraction), `PrizeStructure` (+`forEvent(...)`), `World.buildEvent` (build the tier/field-aware purse). No change to `PrizeStructure.standard()`, the shot engine, ranking, or careers' non-financial state.
- **Determinism / ripple**: purses are deterministic; two same-seed worlds stay byte-identical. Because lower-tier golfers now earn less and some earn nothing, their funds — and therefore their AI staff/equipment spending, which the economy gates — shift, so multi-season *world* outcomes diverge from before (identically for same seeds). Economy tests assert direction (someone earns, spreads widen, reproducible), which still holds and now holds *more strongly*; any golden value is re-based.
- **Boundary**: still spec-clean — the Economy awards the prize amounts the tournament defines and never touches attributes/rankings/shots (`Economic Independence` preserved).
