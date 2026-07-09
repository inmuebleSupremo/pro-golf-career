## Why

The world plays thousands of tournaments across decades, and the moment each one ends its numbers are gone — there is no season-by-season stat line, no all-time record book, no way to ask "who won the 2031 Elite events" or "whose scoring average leads the era." The Media domain narrates the *stories*; nothing keeps the *ledger*. Statistics, Records & Historical Archives is the authoritative memory of the competitive world: it accumulates per-golfer statistics that survive retirement, recognises exceptional achievements as records that preserve their own progression, and keeps a permanent, queryable archive of champions and seasons. It is what lets the world be compared, remembered, and reasoned about — the backbone every later system (media, legacy, awards, the UI) draws its history from.

## What Changes

- Maintain competitive **Statistics** per golfer (REQ-251): events played, cuts made, wins, top-10s, best finish, scoring relative to par, and earnings — derived solely from real results (shot-level stats such as GIR and putts are deferred; the spec does not prescribe individual calculations).
- Preserve **Seasonal Statistics** for every season (REQ-252): a per-golfer, per-season stat line that remains available after the season and is **never overwritten** by later seasons.
- Accumulate **Career Statistics** (REQ-253) that represent a golfer's complete competitive history and remain **permanently available after retirement**.
- Maintain a **Record Book** (REQ-254/255): world records recognising exceptional achievements (most career wins, lowest tournament score, most consecutive cuts made, career longevity) that **emerge solely from gameplay**, **reference the event that established them**, and — when surpassed — **preserve the previous holder** as record progression (never fabricated, changes logged).
- Keep a permanent, internally consistent **Historical Archive** (REQ-256/257/260): a champions registry and season summaries that correspond to recorded gameplay and are **never removed** because a golfer retires, a season ends, rankings change, or a record breaks.
- Support **historical queries and comparison** (REQ-258/259): retrieve previous champions, seasonal and career statistics, and record progression, and compare careers meaningfully — all reading archived data without modifying it.
- **Wire the archive into the World** (modifies `world-progression`): the World feeds each result and season boundary into the Statistics archive, which accumulates statistics, registers champions, and updates records — deterministically, without the archive affecting any outcome.

Explicitly out of scope, per REQ-262: the Statistics domain is **not responsible for tournament simulation, rankings calculations, career progression, shot resolution, or media generation** — it observes real outcomes and stores/serves history, and never writes back to any domain. Also deferred (spec-optional, REQ-251 "not prescribed"): shot-level statistics (driving accuracy, greens in regulation, putts per round), which would require the tournament and shot engine to surface per-round shot data; and era/tournament comparison beyond golfer and season. The archive is deterministic and pure — every statistic and record is a function of real gameplay with no randomness — so a seeded world produces an identical history.

## Capabilities

### New Capabilities
- `competitive-statistics`: The World maintains per-golfer competitive statistics, preserved for every season (never overwritten) and accumulated across a career, remaining permanently available after retirement — derived solely from real results (REQ-251/252/253).
- `records-archive`: A world Record Book of exceptional achievements that emerge solely from gameplay, reference their establishing events, and preserve progression when surpassed; plus a permanent, internally consistent Historical Archive of champions and season summaries that is never removed (REQ-254/255/256/257/260).
- `historical-queries`: Retrieval of historical information (champions, seasonal and career statistics, record progression) and meaningful comparison between golfers and seasons, using archived data without modifying it — the authoritative source of historical competitive information, independent of the systems that consume it (REQ-258/259/261/262).

### Modified Capabilities
- `world-progression`: The World runs the Statistics archive — feeding each result and season boundary into it to accumulate statistics, register champions, and update records — deterministically and without the archive affecting any simulation outcome.

## Impact

- **Codebase**: New framework-free `com.progolf.sim.statistics` package (`StatLine`, `EventOutcome`, `SeasonStatistics`, `Championship`, `RecordType`, `RecordHolder`, `RecordBook`, `CareerComparison`, `StatisticsArchive`, `StatisticsConstants`). `World` gains a `StatisticsArchive` it feeds at event resolution and seasonal transition, plus query accessors. No existing domain changes.
- **Determinism**: statistics, records, and comparisons are pure functions of real results — no randomness — so a seeded world yields an identical archive; verified by extending the world reproducibility test to the archive.
- **DAG**: `statistics` depends only on `core`. The World feeds it primitives (ids, seasons, positions, scores, cuts, prizes, tournament names/tiers as strings) and reads back statistics/records, so `statistics` imports no other domain; only `world` imports `statistics`.
- **Downstream consumers (future changes)**: Media can cite records and stat leaders; a Legacy/awards system ranks careers from the archive; Persistence stores it; Presentation renders leaderboards and record books.
- **Boundary/risk**: the archive is a pure observer — it reads real outcomes and stores/serves history, never touching simulation, rankings, progression, shots, or media (REQ-262); the only World change is additive feeding at existing seams, guarded by the full-run reproducibility test.
