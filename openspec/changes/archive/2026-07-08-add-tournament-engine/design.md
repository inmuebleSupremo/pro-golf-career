## Context

Three domains exist and interlock but nothing runs a competition: `sim.shot` (`RoundResolver.resolveHole`), `sim.course` (`Course.holeModel(hole, round)`), and `sim.population` (`ProfessionalGolfer`/`Player`). This change adds `sim.tournament`, which *composes* them into one standalone event (REQ-086–100). Tour hierarchy, rankings, and economy are out of scope; the tournament is the producer those consumer domains will later read.

Framework-free, Java 21 / Spring Boot 3, consistent with existing `sim.*`. Determinism is preserved by mapping tournament resolution directly onto the existing `SeedCoordinate` (world→season→tournament→round→golfer→hole→shot).

## Goals / Non-Goals

**Goals:**
- A `Tournament` that draws a `Field` from a population, assigns a `Course`, and plays four rounds to exactly one winner.
- One shared resolution path for all competitors via `RoundResolver` — no human/AI scoring split.
- The lifecycle state machine, relative-to-par scoring, a live leaderboard with ties, one-time cut, and sudden-death playoff.
- Full reproducibility from seed; permanent append-only history.

**Non-Goals:**
- Tour hierarchy / membership / promotion-relegation (inline tier only).
- Rankings recomputation and any financial ledger (record prize amounts only).
- Weather generation (calm or per-tournament-seeded `Environment`).
- The human's interactive per-shot path (all competitors resolved headlessly here).
- Multi-tournament scheduling / calendar (a single event).

## Decisions

### D1. `sim.tournament` composes lower-level domains; it owns only competition state
The package depends on `population`, `course`, `shot`, `core`; none depend back on it (a clean DAG, REQ-278). Tournament owns definition, field, per-round scores, leaderboard, cut, playoff, result — nothing about shot math, course shape, or golfer identity. *Why:* REQ-100 boundary; keeps the engine a coordinator.

### D2. Every competitor resolved via `RoundResolver.resolveHole` — including the human
For each competitor × round × 18 holes, call the existing resolver with the golfer's `Attributes`, `player.toGolferState(pressure)`, the course's per-round `HoleModel`, and a seeded coordinate. The human is resolved the same way in this headless engine. *Why:* REQ-104 identical rules by construction; the interactive per-shot UI is a later presentation override, not a second engine. *Alternative rejected:* a separate human play loop now — premature and risks divergence.

### D3. Seed coordinate maps 1:1 onto tournament structure
`SeedCoordinate(worldSeed, seasonId, tournamentId, roundNo, golferId, holeNo, shotNo)` is exactly the resolution address. `golferId` is the competitor's stable **field index** (0-based), assigned when the field is confirmed. *Why:* reproducibility falls out for free (REQ-098); any hole of any competitor can be resolved in isolation. *Alternative rejected:* hashing the string player id into `golferId` — works, but field index is simpler and already unique within the event. Trade-off: a golfer's stream depends on field position; acceptable because the field ordering is itself deterministic.

### D4. Scoring is strokes-relative-to-par; par comes from the Course
A round score = Σ hole strokes − Σ hole pars for the 18 holes; tournament score accumulates across rounds. `RoundResolver` already returns `RoundOutcome.totalStrokes()` per hole; par comes from `GeneratedHole.par()`. *Why:* REQ-092 lower-is-better, and relative-to-par is the natural leaderboard unit. Ties are equal integer scores.

### D5. Lifecycle as an explicit ordered state machine with a guarded advance
A `TournamentState` enum in declared order with a single `advance()` that performs the work of the *next* stage (play a round, evaluate the cut, run the playoff, complete) and rejects out-of-order calls. The Playoff stage is conditional. *Why:* REQ-087 no skipped states; a guarded advance makes illegal sequencing impossible and keeps the flow inspectable/testable.

### D6. Cut = top-N-plus-ties, evaluated once; missed-cut competitors keep their 36-hole score
After Round 2, sort by score; the cut line is the score at position N (plus everyone tied with it). A `CutResult` is recorded once. Missed-cut golfers stop playing but retain a final result. *Why:* REQ-093 exactly once, identical criteria. N is a format parameter (a constant surface), tunable later.

### D7. Playoff = deterministic sudden-death holes among those tied for the lead
If ≥2 tie for first after Round 4, play additional holes (reusing the course's holes cycled, seeded at a distinct playoff round index) until one competitor is strictly ahead on a hole. *Why:* REQ-095 exactly one winner, reproducible (REQ-098). Cycling existing holes avoids needing new course data.

### D8. Prize distribution records amounts only; result/history are append-only value objects
A `PrizeStructure` (payout curve by position) yields a `Map<competitor, amount>` at completion; the `TournamentResult`/history are immutable records. No ledger, no mutation after completion. *Why:* REQ-096/099 and the Economy boundary — amounts are a contract the Economy domain consumes later.

## Risks / Trade-offs

- **[Performance: full field × 4 rounds × 18 holes of shot resolution]** → At Standard scale (~120 field) that's ~8.6k hole resolutions per event, each a short loop — well within budget; the resolver is allocation-light. Profile if fields grow.
- **[Field-index-as-golferId couples a golfer's RNG stream to field position]** → Acceptable and deterministic; documented in D3. If cross-event stream stability per golfer is later needed, switch to a hashed stable id (additive change).
- **[Stubbed conditions/prizes/eligibility could ossify as shortcuts]** → Each is an explicit seam (calm `Environment`, payout-amount map, inline tier check) with a spec note that the real domain replaces it — not hidden.
- **[Cut/field-size constants are uncalibrated]** → Keep them in one `TournamentConstants` surface (mirroring `SimConstants`); assert structural correctness now, tune later.
- **[Withdrawal mid-round bookkeeping]** → Model withdrawal as removing a competitor from the active field while preserving their partial result; leaderboard and completion ignore withdrawn competitors for ranking but retain history.

## Migration Plan

Greenfield addition — no rollback surface. Sequencing: (1) definition types (`Tournament`, `TournamentEntry`, `Field`, `PrizeStructure`, `TournamentFormat`, `TournamentConstants`); (2) registration/eligibility + field confirmation; (3) scoring + leaderboard; (4) the lifecycle state machine driving four rounds via `RoundResolver`; (5) cut evaluation; (6) playoff; (7) completion + prize distribution + result/history + withdrawal; (8) test suites (reproducibility, no-skip lifecycle, cut-once, exactly-one-winner incl. forced-tie playoff, withdrawal continuity, shared-engine/no-control-branch, end-to-end run on a generated course and population). Each layer testable before the next.

## Open Questions

- Field size and cut line (N) defaults — placeholder constants now; finalised with the Tour/World change that sizes fields per tier.
- Whether a forced-tie scenario for the playoff test is constructed via crafted inputs or discovered by seed search — lean on crafted equal-score inputs for a deterministic test.
- Pressure input to `toGolferState` during pressure situations (final round, cut line) — pass 0.0 for now; Composure-driven pressure modelling is a later refinement (the shot engine already supports a pressure input).
- Exact prize payout curve — a simple decreasing curve now; real monetary values arrive with the Economy domain.
