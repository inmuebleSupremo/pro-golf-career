## Why

The world runs at a small, abstract scale — **160 golfers**, event fields capped at **40** (and only ~13 at the Elite tier, which has ~13 members). A real professional circuit is an order of magnitude larger: hundreds of pros per tour, 120–156-player fields, a cut that trims the field for the weekend. Two consequences of the small scale hollowed out real mechanics:

- **The cut never bit.** The tournament engine has a correct cut, but the World fed it `DEFAULT_CUT_SIZE = 60` into a 40-golfer field, so `60 > 40` meant *nobody was ever cut* — every competitor played all four rounds, and "make the cut" (and cut-streak records, cut-make rate) meant nothing.
- **Prize money was universal.** With 70 paid positions into a 40 field, everyone cashed — the root of the economy problem the last change patched with a flat pay fraction.

The engine's own defaults (field 120, cut 60, 70 paid) were sized for a realistic ~120–150 field; the World just ran smaller. This change scales the world to that intended size and makes the cut — and the money line that follows it — real. Measured cost: ~0.6 s to advance a full season at the new scale; tests are unaffected (they use a small config).

## What Changes

- **Realistic default scale.** `INITIAL_POPULATION` 160 → **640** and `FIELD_SIZE` 40 → **120** (Elite ~51, Primary ~109, Secondary ~192, Development ~288 across the tiers; fields up to 120). Tier fractions, events per season, and weeks are unchanged; the small test `WorldConfig` is unchanged, so the suite stays fast.
- **A cut that bites at any scale.** The World now sizes each event's cut to its actual field: `cutSize = round(field × CUT_FRACTION)` (0.45), instead of the fixed `DEFAULT_CUT_SIZE`. So roughly the top 45% play the weekend and the rest are cut after Round 2 — at every field size, including the small test world.
- **The money line is the cut.** Prize money is paid to those who make the cut: `paidPositions = cutSize`. Making the cut is the pay line (as in real golf); missing it earns nothing while entry and travel are still owed. This supersedes the previous flat `PAID_POSITIONS_FRACTION` (0.65-of-field) pay line with the more realistic cut-based one.

Explicitly out of scope: exact field sizes per tier/qualification model (still a simple cap on tour standings); changing events-per-season or the schedule shape; `PrizeStructure.standard()` and the standalone tournament-engine defaults (unchanged — only the World's per-event sizing changes); performance work beyond confirming the measured cost is comfortable.

## Capabilities

### Modified Capabilities
- `financial-strategy`: prize money is paid to the golfers who make the cut — making the cut is the pay line, and missing it earns nothing — so a poor week costs a golfer both the paycheck and the entry/travel spent.

## Impact

- **Codebase**: `WorldConstants` (population + field size), `TournamentConstants` (`CUT_FRACTION` replaces `PAID_POSITIONS_FRACTION`), `World.buildEvent` (size the cut and the pay line to the field, build the format with that cut). No change to the tournament cut/scoring engine, `PrizeStructure.standard()`, the shot engine, or the small test config.
- **Determinism / reproducibility**: all sizing is deterministic; two same-seed worlds stay byte-identical. Because the cut now bites and only made-cut golfers earn, the small test world's finances, health (missed-cut golfers play two rounds), and cut statistics shift from before — identically for same seeds; direction-based world tests hold, and any golden value is re-based.
- **Performance**: ~0.6 s per season advance at 640/120 (measured; ~0.3 s at the old 160/40), well within a turn-based career game. Tests are unaffected (small config).
- **Boundary**: `World` still only configures events and feeds results to the owning domains; the tournament engine performs the cut and the economy pays the defined prizes.
