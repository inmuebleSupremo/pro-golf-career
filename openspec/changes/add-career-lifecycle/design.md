## Context

Golfers exist (`Player`/`ProfessionalGolfer`), tournaments produce `TournamentResult`, and the world ranks golfers — but the `ProfessionalGolfer`'s Career reference points at nothing. This change adds `sim.career`: the Career entity that ties a golfer's events into a timeline (start → seasonal aging → mandatory retirement at 65) plus a cumulative record and a Hall-of-Fame hook. Progression/aging and World calendar generation stay deferred; the Career exposes a seasonal-advance the World will drive.

Framework-free, Java 21 / Spring Boot 3, consistent with existing `sim.*`.

## Goals / Non-Goals

**Goals:**
- A `Career` owned by a `Player`: start age 16–22, whole-year Age advancing once per season, never decreasing, mandatory retirement at 65.
- A seasonal-advance mechanism that ages and archives a season (no calendar generation).
- Cumulative statistics + auto milestones (duplicate-safe) + chronological immutable history from `TournamentResult`.
- Retirement → read-only + automatic Hall-of-Fame evaluation (placeholder criteria) recorded permanently.
- Append-only integrity; a completed career reconstructible from its records; analytical only.

**Non-Goals:**
- Progression, aging of attributes, regression (separate domain).
- Calendar / schedule generation and season cadence (World loop).
- The real Hall-of-Fame criteria (placeholder seam).
- Any write-back to player attributes or tournament results.

## Decisions

### D1. `Career` is a mutable aggregate coordinating status with its Player
`Career` holds the owning `Player`, a start age, current Age, a `SeasonArchive`, a `CareerStatistics`, a milestone set, a chronological history, and (post-retirement) a Hall-of-Fame result. It coordinates retirement through the Player's existing `CareerStatus` state machine (ACTIVE → RETIRED). *Why:* REQ-025 (one golfer's whole history) and REQ-024 single ownership — the Career doesn't duplicate player data, it references the Player and drives its status. *Alternative rejected:* a Career detached from the Player — breaks the "belongs to exactly one Player" rule and status coordination.

### D2. Age and retirement live in the seasonal-advance operation
`advanceSeason()` archives the completed season, increments Age by one, and — if Age reaches 65 — transitions the Player to RETIRED and runs the Hall-of-Fame evaluation. Age has no setter; it only advances. *Why:* REQ-027 (advance once per season, never decrease) and REQ-028 (mandatory retirement, not bypassable) become structural — there is no path to decrement age or skip retirement. *Alternative rejected:* an external age setter — invites bypassing retirement.

### D3. Statistics and milestones are derived by folding recorded results
`recordTournament(result, date)` updates `CareerStatistics` (events, cuts, wins, runner-ups, top-10s, sum-of-finishes for average, earnings) and checks each milestone's first-occurrence, recording it once. It also appends a history entry. Recording on a RETIRED career is rejected. *Why:* REQ-031/032/033 — statistics update automatically, milestones are objective with duplicate prevention, history is append-only. *Alternative rejected:* recomputing stats from a stored result list each read — fine functionally, but a folded running total is simpler for consumers and still reproducible from the ordered inputs.

### D4. Milestones as an enum with a first-occurrence guard
`CareerMilestone` (FIRST_EVENT, FIRST_MADE_CUT, FIRST_TOP_10, FIRST_WIN, …) recorded into a `Set`; a milestone fires only if absent. The record stores the milestone plus the date it occurred. *Why:* objective and duplicate-safe by construction (REQ-031). Milestone list is extensible without touching the fold logic.

### D5. History is an append-only list of immutable dated entries; reproducibility by replay
`CareerHistoryEntry(date, type, description)` records are appended, never mutated; the list is exposed as an unmodifiable, date-ordered view. A completed career is reconstructible by replaying its recorded results and season advances in order. *Why:* REQ-033/037. *Trade-off:* ordering assumes results are recorded in date order; the (future) World loop feeds them chronologically, which we document as a precondition and assert in tests.

### D6. Hall-of-Fame evaluation is a placeholder function over the statistics
`HallOfFame.evaluate(CareerStatistics)` returns a recorded `HallOfFameResult` (eligible + a reason/threshold summary) using placeholder thresholds in a `CareerConstants` surface. It runs once at retirement and never mutates history. *Why:* REQ-036 requires automatic evaluation recorded permanently while the criteria are defined elsewhere later — a clean seam. *Alternative rejected:* leaving it unimplemented — fails REQ-036's "evaluation occurs automatically".

### D7. Runtime states are a separate lightweight enum, orthogonal to progression
A `CareerRuntimeState` (ACTIVE, SAVED, LOADED, PAUSED) is tracked but gates nothing in the domain logic — it is execution metadata (REQ-034). *Why:* the spec is explicit that these affect execution only, never progression; keeping them out of the advance/record logic makes that guarantee structural.

## Risks / Trade-offs

- **[Out-of-order result recording would corrupt "chronological" history]** → Document date-ordered recording as a precondition; assert ordering in tests; the World loop feeds results in order. A defensive insert-in-order is a cheap later hardening if needed.
- **[Placeholder Hall-of-Fame thresholds are arbitrary]** → Isolate in `CareerConstants`; assert only that evaluation runs, is derived from stats, and is recorded — not specific pass/fail magnitudes; the Legacy spec refines later.
- **[Average finish as running mean vs. stored finishes]** → Keep sum-of-finishes + count so the mean is exact and cheap; equivalent to recomputing from stored finishes, and reproducible.
- **[Career age vs. Player date-of-birth]** → Career owns competitive Age (advances per season) independent of the Player's real DOB; the two are not conflated (Age is the gameplay clock).
- **[Retirement coordination with an INJURED player]** → Retirement at 65 goes ACTIVE→RETIRED; if the player is INJURED at 65, transition INJURED→RETIRED (both permitted by the existing status machine); tests cover it.

## Migration Plan

Greenfield addition — no rollback surface. Sequencing: (1) `CareerConstants`, `CareerMilestone`, `CareerRuntimeState`, `CareerHistoryEntry`, `CareerStatistics`, `HallOfFameResult`; (2) `Career` entity with start validation (age 16–22) + ownership; (3) `advanceSeason()` (archive + age + mandatory retirement at 65 + HoF evaluation); (4) `recordTournament()` folding statistics + milestones + history, rejected when retired; (5) runtime-state tracking; (6) test suites (start validation, age monotonic + never-decrease, mandatory retirement at 65 + read-only, seasonal archive, statistics/milestone folding + duplicate prevention, chronological append-only history, HoF evaluation on retirement, reproducibility by replay). Each layer testable before the next.

## Open Questions

- Whether `advanceSeason()` also rolls up season-level statistics into a per-season archive record now, or just marks the boundary — start with a lightweight season record (index + end age); the Statistics/records domain can enrich later.
- Season length / how many tournaments per season — irrelevant here (the World decides); the Career only counts season boundaries.
- Exact placeholder Hall-of-Fame thresholds (e.g. wins/major-equivalents) — pick simple defaults; the Legacy spec owns the real rule.
- Whether milestones like "first major-equivalent win" belong here or with the Milestones/Legacy domain (REQ-166–176) — record the objective competitive firsts here; richer legacy milestones are a later domain.
