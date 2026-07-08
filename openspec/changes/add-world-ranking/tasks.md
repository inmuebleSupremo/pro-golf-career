## 1. Ledger foundation

- [x] 1.1 Create framework-free package `com.progolf.sim.ranking` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Add `RankingConstants` (base points per tier, position-curve params, full-value days, window days) as the single tunables surface.
- [x] 1.3 Define an immutable `RankingAward` (golferId, date, points, tournamentId) and a `RankingLedger` that appends awards (append-only).

## 2. Points model

- [x] 2.1 Implement `positionWeight(position)` — a decreasing curve (winner most, declining by place).
- [x] 2.2 Implement `basePoints(tier)` from `RankingConstants`.
- [x] 2.3 Implement `fieldStrengthFactor(field, asOf)` using current ranking values when available, falling back to a tier-based bootstrap proxy before convergence.
- [x] 2.4 Implement `record(TournamentResult, tier, date)` — award points to each finisher (position × tier × field strength) and append to the ledger.

## 3. Decay & ranking

- [x] 3.1 Implement `decay(points, ageDays)` — full value within the full-value period, declining linearly to zero at the window edge; expired points contribute nothing.
- [x] 3.2 Implement `rankingValue(golferId, asOf)` = sum of decayed awards within the window.
- [x] 3.3 Implement `rankingAsOf(asOf)` — ordered standings by ranking value, with a deterministic tie-break (e.g. golfer id), as a total order.

## 4. Eligibility

- [x] 4.1 Filter the active ranking to eligible golfers; keep historical award/records for ineligible (e.g. retired) golfers intact.

## 5. History & recognition

- [x] 5.1 Define an immutable `RankingSnapshot` (as-of date + ordered standings) captured on demand.
- [x] 5.2 Implement history derivations: career-high ranking, weeks at World #1 (over dated snapshots), season-ending ranking.
- [x] 5.3 Implement ranking movement and milestone entry/exit between two snapshots.
- [x] 5.4 Implement `RankingRecognition` — best milestone reached (Top 100 / Top 50 / Top 10 / World #1) from a golfer's history.
- [x] 5.5 Expose read-only standing types so dependent systems consume without reaching the ledger's mutation surface.

## 6. Verification

- [x] 6.1 Points monotonicity tests: better finish ≥ worse; higher tier / stronger field awards more for the same position.
- [x] 6.2 Decay tests: older results contribute less; points beyond the window contribute nothing.
- [x] 6.3 Reproducibility test: same results + as-of date yields identical ranking; control type does not affect points/standing.
- [x] 6.4 Eligibility test: ineligible golfers removed from active ranking; their history preserved.
- [x] 6.5 History tests: career-high preserved after decline; weeks-at-#1 accumulate and never decrease; snapshots immutable.
- [x] 6.6 Recognition tests: reaching a milestone is recorded; recognition reflects true best position.
- [x] 6.7 End-to-end test: feed several `TournamentResult`s (from generated events) through the ledger and assert a sensible, reproducible ranking with a plausible World #1.
- [x] 6.8 Run `openspec validate add-world-ranking --type change --strict` and resolve findings.
