## 1. Definition types

- [x] 1.1 Create framework-free package `com.progolf.sim.tournament` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Add `TournamentConstants` (field size default, cut line N, prize curve params) as the single tunables surface.
- [x] 1.3 Define `TournamentFormat` (round count = 4, hasCut flag) and `PrizeStructure` (payout amount by finishing position, non-increasing).
- [x] 1.4 Define the `Tournament` definition (name, Course, tier, entry requirements, prize structure, format, scheduled date) with immutability once play begins.
- [x] 1.5 Define `TournamentEntry` (one Player, belongs to one Tournament) and `Field` (ordered accepted entries; field index = golfer id).

## 2. Registration & field

- [x] 2.1 Implement registration/eligibility: evaluate the inline tier/entry requirements; create an Entry only when eligible; reject duplicates (one per Player).
- [x] 2.2 Return a clear, typed failure reason on ineligible/duplicate registration.
- [x] 2.3 Confirm the field (transition to Field Confirmed); once Round 1 begins, reject new entries and fix the field.

## 3. Scoring & leaderboard

- [x] 3.1 Implement round scoring: resolve 18 holes via `RoundResolver.resolveHole`, summing strokes; round score = strokes − par (from `GeneratedHole.par()`).
- [x] 3.2 Accumulate cumulative tournament score across rounds; completed rounds immutable.
- [x] 3.3 Implement the live leaderboard: rank active competitors by cumulative score (lower better) with shared positions for ties; recompute on score change.

## 4. Lifecycle state machine

- [x] 4.1 Define `TournamentState` enum in canonical order (Scheduled → Registration Open → Field Confirmed → R1 → R2 → Cut → R3 → R4 → Playoff → Completed).
- [x] 4.2 Implement a guarded `advance()` that performs the next stage's work and rejects out-of-order transitions; Playoff entered only when required.
- [x] 4.3 Resolve every competitor through the shared engine with a `SeedCoordinate` per (tournament, round, golfer=field index, hole, shot); pass `player.toGolferState(0.0)`.

## 5. Cut & playoff

- [x] 5.1 Implement cut evaluation exactly once after Round 2: top N plus ties advance; identical criteria for all; record a `CutResult`; missed-cut competitors retain their 36-hole result and play no further.
- [x] 5.2 Skip the cut for formats without one.
- [x] 5.3 Implement sudden-death playoff among competitors tied for the lead after Round 4; play seeded extra holes until exactly one winner; guarantee a single winner.

## 6. Completion & history

- [x] 6.1 Implement completion: require final scores, a single winner, prize distribution, and recorded statistics before finalising; make the Tournament read-only afterward.
- [x] 6.2 Implement prize distribution: map finishing position to payout amount via `PrizeStructure` (better position ≥ worse); record amounts only.
- [x] 6.3 Implement withdrawal (before/during play): record it, remove the competitor from the active field, keep the event valid and continuing.
- [x] 6.4 Produce an immutable `TournamentResult` / history value (course, competitors, scores, cut, winner, prize distribution); append-only, never overwritten.

## 7. Verification

- [x] 7.1 Reproducibility test: running the same tournament (seed, course, field) twice yields identical scores, cut, winner, and prize distribution.
- [x] 7.2 Lifecycle tests: states occur in order and cannot be skipped; out-of-order `advance()` rejected; Playoff only when required.
- [x] 7.3 Cut tests: evaluated exactly once after Round 2; identical criteria; missed-cut competitors excluded from Rounds 3–4 but retained in history.
- [x] 7.4 Playoff test: a crafted tie for the lead resolves to exactly one winner and is reproducible.
- [x] 7.5 Scoring/leaderboard tests: relative-to-par accumulation; lower ranks ahead; ties share a position.
- [x] 7.6 Withdrawal test: mid-event withdrawal keeps the tournament valid and completing; withdrawal recorded.
- [x] 7.7 Shared-engine test: no control-type branch in tournament resolution; human and simulation entries resolve through the same path.
- [x] 7.8 End-to-end test: draw a population field, generate a Course, run a full event to a single winner with permanent history.
- [x] 7.9 Run `openspec validate add-tournament-engine --type change --strict` and resolve findings.
