## Context

The World produces everything a statistician needs — each `TournamentResult.Finish` carries a position, a cumulative score relative to par, a made-cut flag, a withdrawal flag, and prize money, and each event has a name, tier, and season — but nothing indexes it: the moment a `SeasonArchive` is stored the numbers are inert. This change adds a `sim.statistics` domain that observes those real outcomes and builds the authoritative competitive memory of the world: per-season and career statistics, a record book with progression, a permanent champions archive, and comparison. It is the second purely observational domain (after Media): it reads and serves, and writes back to nothing.

Framework-free, Java 21 / Spring Boot 3, deterministic and analytical: every statistic, record, and comparison is a pure function of real results — there is no randomness in the domain.

## Goals / Non-Goals

**Goals:**
- Per-golfer, per-season statistics preserved permanently and never overwritten; career aggregates available after retirement.
- A record book of exceptional achievements that emerge from gameplay, reference their establishing event, and preserve progression when surpassed.
- A permanent, internally consistent historical archive (champions, season summaries) with queries.
- Meaningful career comparison from archived data, without modifying it.
- Wire into the World deterministically; the archive affects no outcome (independence).

**Non-Goals:**
- Shot-level statistics (driving accuracy, greens in regulation, putts per round) — deferred; would need the tournament/shot engine to surface per-round shot data (REQ-251 "not prescribed").
- Rankings, career progression, tournament simulation, or media generation (forbidden, REQ-262).
- Era/tournament comparison beyond golfer and season; award ceremonies (a later Legacy/awards domain).
- Any randomness — the archive is a deterministic function of history.

## Decisions

### D1. `sim.statistics` depends only on `core`; the World feeds it primitives
The archive is constructed and driven by the World, which passes primitives — ids, seasons, positions, scores, cut/withdrawal flags, prizes, and tournament names/tiers as strings — into the archive and reads back statistics and records. `statistics` imports only `core`; it never imports `tournament`, `ranking`, `career`, `world`, etc. *Why:* REQ-262 responsibility boundary and the established pattern (Media and every recent domain depend only on core; the World orchestrates). *Alternative rejected:* the archive importing the other domains to read their state — couples the authoritative history to everything it records and risks it reaching back.

### D2. The archive is a pure observer — it writes back to nothing
`StatisticsArchive` accumulates from `observeEvent(...)`/`observeSeason(...)` and answers queries. It exposes no operation that mutates another domain, and the World never lets it influence resolution, ranking, or progression. *Why:* REQ-261/262 — it is the authoritative source others consume, not a participant in gameplay. *Alternative rejected:* statistics feeding back into ranking/awards — violates independence.

### D3. Statistics are keyed by (golfer, season); career is the aggregate
An immutable `StatLine` (events, cuts, wins, runner-ups, top-10s, best finish, total score vs par, earnings) is accumulated per `(golferId, season)` via `StatLine.plus`. A season's line is frozen simply by being keyed to that season — later seasons write different keys, so history is never overwritten (REQ-252). A golfer's career statistics are the sum of their season lines (REQ-253), so retirement removes nothing. *Why:* one immutable accumulator gives seasonal preservation and career aggregation for free. *Alternative rejected:* a single mutable career tally — loses per-season history and the no-overwrite guarantee.

### D4. Records emerge from gameplay, reference their event, and preserve progression
A `RecordBook` holds the current `RecordHolder` per `RecordType` (MOST_CAREER_WINS, LOWEST_TOURNAMENT_SCORE, MOST_CONSECUTIVE_CUTS, LONGEST_CAREER) plus an append-only progression log per type. A record is only ever set by `challenge(type, golferId, value, season)` from an observed outcome, in the type's direction (lower is better for score, higher otherwise); when beaten, the new holder is appended and the previous one remains in the log. *Why:* REQ-254 (reference the establishing event), REQ-255 (emerge from gameplay, never fabricated, previous holder preserved, changes logged). *Alternative rejected:* storing only the current record — loses progression the spec requires.

### D5. Consecutive-cut and longevity records need light per-golfer sequence state
The archive tracks each golfer's current consecutive-cuts streak (reset on a missed cut) and the set of seasons they have appeared in (career longevity), updating the corresponding records as those grow. All other records derive from the per-event or career aggregates already maintained. *Why:* REQ-254 lists consecutive cuts and career longevity; both require order/appearance state beyond a simple tally. *Alternative rejected:* recomputing streaks from a full event log each time — wasteful; the incremental state is small and deterministic.

### D6. A permanent champions archive backs the historical queries
Each event's winner is recorded as a `Championship(season, tournamentName, tier, winnerId)`; the archive answers `championsOfSeason`, `championshipsOf(golfer)`, seasonal/career statistics, and `recordProgression(type)`. Nothing is ever removed (REQ-256/257), and every entry corresponds to a real result (REQ-260). *Why:* REQ-258 historical queries over authoritative, permanent data. *Alternative rejected:* deriving champions on demand from the World's archives — duplicates traversal and splits authority.

### D7. Comparison reads archived data without modifying it
`compareCareers(a, b)` returns a `CareerComparison` (both career `StatLine`s and simple leader helpers — more wins, higher earnings, better scoring average) computed from the archive. It never mutates records. *Why:* REQ-259 meaningful comparison from archived information. *Alternative rejected:* baking comparison into the records — comparison is a read, not a stored fact.

### D8. World wiring: feed results and season boundaries, expose queries
In `resolveEvent`, the World feeds each finish (`observeEvent`) and registers the champion; records update inside the archive as outcomes arrive. In `seasonalTransition`, it advances the archive's per-golfer longevity for the completed season. Read accessors expose seasonal/career statistics, champions, records and their progression, and comparison. All deterministic. *Why:* REQ-251/252/254 and the living-world goal; REQ-299 reproducibility. *Alternative rejected:* aggregating inside `Tournament`/`Career` — those must not own the world archive (REQ-262).

## Risks / Trade-offs

- **[Statistics leaking into gameplay]** → `statistics` imports only `core`; the archive exposes no external mutation; an architecture/boundary test asserts it changes no score/ranking/progression state, and the World never reads it back into resolution (REQ-261/262).
- **[Overwriting or losing history]** → season lines are keyed by season and never rewritten; records append progression and never delete prior holders; a test asserts a surpassed record keeps its previous holder and that a retired golfer's stats remain (REQ-252/255/257).
- **[Authenticity]** → every entry is created only from an observed outcome; a test asserts every champion and record references a real observed event and that no entry exists without one (REQ-260).
- **[Determinism]** → no randomness; the world reproducibility test is extended to assert two same-seed worlds produce identical archives.
- **[Duplication with `CareerStatistics`]** → the archive builds its own tallies from observed events rather than importing `sim.career`; the value is the world-level records/archive/queries/comparison layer, which `CareerStatistics` (a career's own tally for milestones) does not provide.

## Migration Plan

Greenfield `sim.statistics` + one additive modification (World feeding at existing seams). Sequencing: (1) `StatisticsConstants`, `EventOutcome`, `StatLine` (+ `plus`, derived reads); (2) `SeasonStatistics`, `Championship`; (3) `RecordType`, `RecordHolder`, `RecordBook` (challenge + progression); (4) `CareerComparison` + `StatisticsArchive` (observeEvent/observeSeason, streak + longevity state, queries, comparison); (5) wire the World — feed finishes and champions in `resolveEvent`, advance longevity in `seasonalTransition`, expose accessors; (6) tests — stat accumulation + seasonal preservation + career aggregation after retirement; records emerge/reference/progress and keep previous holders; champions archive queryable and permanent; comparison reads without mutating; boundary; world builds a diverse, reproducible archive from real events. Each layer testable before the next.

## Open Questions

- Exact record set and any thresholds — placeholder `StatisticsConstants`; shot-level records await shot-data plumbing.
- Whether career longevity is counted in seasons or events — seasons in V1 (matches "career longevity"); revisit if events read better.
- Whether season summaries should include leaders (money list, wins) — champions + per-golfer season lines in V1; a leaders view can be derived later without schema change.
- Whether comparison should span seasons/eras/tournaments — careers and seasons in V1; broader comparison is a later extension (REQ-259).
