## 1. Richer win tracking (career)

- [x] 1.1 `CareerStatistics`: add `signatureWins` and `developmentWins`; classify each win at fold time by priority major → signature → development-tier → regular (disjoint buckets; regular pro wins derived). Add getters.
- [x] 1.2 `Career.recordTournament`: accept the tour `Tier`; keep backward-compatible overloads (default `Tier.STANDARD`) so existing call sites/fixtures are unchanged. Thread the tier from `World.feedConsumers` (already in scope) into the record call.

## 2. Two-phase evaluator (career, pure)

- [x] 2.1 `CareerConstants`: replace the placeholder thresholds with baseline (`HOF_MIN_PRO_WINS=15`, `HOF_MIN_MAJORS=2`, `HOF_MIN_AGE=45`, `HOF_RETIRED_SEASONS=3`), score weights (`HOF_SCORE_MAJOR/SIGNATURE/REGULAR/DEVELOPMENT`), and election tunables (`HOF_ELECTION_CYCLE_SEASONS=2`, `HOF_INDUCTEES_PER_CYCLE=1`).
- [x] 2.2 `HallOfFameCredentials` record (primitives: majors, signatureWins, totalWins, developmentWins, age, seasonsSinceRetirement, retired) with `proWins()`/`regularProWins()` derivations.
- [x] 2.3 `HallOfFame.meetsBaseline(credentials)` (Phase 1: status AND stats) and `HallOfFame.score(credentials)` (Phase 2: prestige-weighted). Repoint the retirement path to record baseline eligibility; update `HallOfFameResult` summary.
- [x] 2.4 `HallOfFameInduction(golferId, season, score)` pure record.

## 3. Biennial election (world)

- [x] 3.1 World state: a Hall registry (inductions + inducted-id set) and a retirement-season map (populated when a career retires).
- [x] 3.2 `seasonalTransition`: after retirements, if `season % HOF_ELECTION_CYCLE_SEASONS == 0`, build credentials for every not-yet-inducted career, filter by `meetsBaseline`, induct the single highest `score` (id tie-break), record the induction, and publish `NewsFactory.hallOfFameInduction`.
- [x] 3.3 `NewsType.HALL_OF_FAME_INDUCTION` + `NewsFactory.hallOfFameInduction`.
- [x] 3.4 Rewire the `HALL_OF_FAME` goal in `evaluateGoal` to registry membership (inducted, not merely eligible).
- [x] 3.5 Accessors: `World.hallOfFameMembers()` / `hallOfFameInductions()` / `isInHallOfFame(id)`; expose on `WorldService`.

## 4. Tests

- [x] 4.1 `HallOfFameTest` (career): baseline needs BOTH status and stats (majors alone / wins alone fail); score orders majors > signature > regular > development; deterministic.
- [x] 4.2 `WorldHallOfFameTest`: no induction off-cycle; exactly one (top-scored) inducted per election with carryover; active and retired both electable; induction news published; `HALL_OF_FAME` goal true only after induction; reproducible across two same-seed worlds.
- [x] 4.3 Update `CareerLegacyTest` / `CareerMajorsTest` for the stricter two-phase baseline (majors required alongside pro wins).
- [x] 4.4 Full suite green.

## 5. Verify

- [x] 5.1 Run a long World (enough seasons for legends to emerge) and confirm inductions occur on the biennial cycle, one per cycle, favouring major-rich careers; reproducible.
