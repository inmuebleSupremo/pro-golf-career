## 1. Value types

- [x] 1.1 Create framework-free package `com.progolf.sim.career` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Add `CareerConstants` (start-age min/max 16–22, mandatory retirement age 65, placeholder Hall-of-Fame thresholds) as the single tunables surface.
- [x] 1.3 Define `CareerMilestone` enum (FIRST_EVENT, FIRST_MADE_CUT, FIRST_TOP_10, FIRST_WIN, …) and `CareerRuntimeState` enum (ACTIVE, SAVED, LOADED, PAUSED).
- [x] 1.4 Define immutable `CareerHistoryEntry` (date, type, description) and a `SeasonRecord` (index, end age).
- [x] 1.5 Define `CareerStatistics` (events, cuts, wins, runner-ups, top-10s, sum-of-finishes + count for average, total earnings) with read accessors incl. average finish.
- [x] 1.6 Define `HallOfFameResult` (eligible + summary) and a `HallOfFame.evaluate(CareerStatistics)` using placeholder thresholds.

## 2. Career entity & lifecycle

- [x] 2.1 Implement `Career` = owning `Player` + start age (validate 16–22) + current Age + statistics + milestones + history + season archive + runtime state.
- [x] 2.2 Start the Career on activation (career ACTIVE, records starting age); reject transfer to another Player.
- [x] 2.3 Implement `advanceSeason()` — archive the completed season, increment Age by one (no setter; never decreases).
- [x] 2.4 Enforce mandatory retirement at 65: reaching 65 via advance transitions the Player to RETIRED (handles ACTIVE and INJURED); retirement cannot be bypassed.
- [x] 2.5 Make a RETIRED career read-only for competitive updates; track runtime states that never affect progression.

## 3. Record folding

- [x] 3.1 Implement `recordTournament(result, date)` for the golfer — increment events; update cuts/wins/runner-ups/top-10s/earnings/sum-of-finishes from the result; reject if retired.
- [x] 3.2 Fire objective milestones on first occurrence with duplicate prevention (first event/cut/top-10/win).
- [x] 3.3 Append a chronological, immutable `CareerHistoryEntry`; expose a date-ordered unmodifiable history view.

## 4. Legacy

- [x] 4.1 On retirement, automatically run `HallOfFame.evaluate(...)` and record the result permanently; ensure evaluation never mutates history.

## 5. Verification

- [x] 5.1 Start tests: age outside 16–22 rejected; career begins ACTIVE with starting age; not transferable.
- [x] 5.2 Age/retirement tests: age advances by exactly one per season and never decreases; reaching 65 retires automatically; retirement cannot be bypassed.
- [x] 5.3 Read-only tests: recording on a RETIRED career is rejected; runtime states do not advance progression.
- [x] 5.4 Statistics tests: events/cuts/wins/top-10s/earnings update from results; average finish equals the mean of recorded finishes.
- [x] 5.5 Milestone tests: first occurrences recorded; duplicates prevented.
- [x] 5.6 History tests: chronological order; entries immutable/append-only.
- [x] 5.7 Legacy test: Hall-of-Fame evaluation runs automatically on retirement, is derived from statistics, is recorded, and mutates no history.
- [x] 5.8 Reproducibility test: replaying the same ordered results + season advances yields an identical career record.
- [x] 5.9 Run `openspec validate add-career-lifecycle --type change --strict` and resolve findings.
