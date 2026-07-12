## Why

The Hall-of-Fame election scored a career purely by its wins (majors ≫ signature > regular > development). But two golfers with the same win total are not equal Hall candidates: one may have reigned at World #1 for years while the other quietly compiled wins without ever topping the world. Sustained ranking dominance is one of the sport's strongest Hall credentials, and it was deferred from the original HoF-election change as a noted extension point. This adds it (plus earnings) to the score.

## What Changes

- **Ranking dominance in the HoF score (career).** The election score now adds, on top of the prestige-weighted wins: a **peak-position bonus** greatest for reaching World #1 and fading to zero by a cap, plus a **weight per season finishing at World #1** (sustained dominance). A small **earnings** credential is also added. So a golfer who reigned at the top outscores a compiler of the same win total.
- **Score only, not the baseline (kept the hard floor).** Ranking and earnings feed only the score — eligibility still requires the age/retired status gate AND ≥15 professional wins AND ≥2 majors. A long-reigning #1 with too few majors still does not qualify; ranking dominance decides who gets in *among the qualified*, not who qualifies.
- **The World fills the ranking.** `HallOfFameCredentials` gains career-high ranking, seasons-at-#1, and earnings; the World computes the first two from the season-ending ranking snapshots (`RankingHistory.careerHighPosition` / `weeksAtNumberOne`, both already present) and the third from career statistics. Backward-compatible constructors default the new fields (unranked, no #1 seasons, no earnings) so the baseline evaluation and existing callers are unchanged.

## Capabilities

### Modified Capabilities
- `career-legacy`: the Hall-of-Fame election score additionally rewards ranking dominance (a peak-World-#1 bonus and a weight per season at #1) and career earnings — affecting only the score, never the baseline — so a golfer who dominated the world rankings outscores a compiler of the same win total.

## Impact

- **Codebase**: `HallOfFameCredentials` gains `careerHighRanking` / `seasonsAtNumberOne` / `careerEarnings` (with a backward-compatible 7-arg constructor and a ranking-aware `of` factory); `HallOfFame.score` adds the dominance + earnings terms; `CareerConstants` gains the weights (`HOF_SCORE_RANK_PEAK`, `HOF_RANK_PEAK_CAP`, `HOF_SCORE_SEASON_AT_ONE`, `HOF_SCORE_EARNINGS_PER_MILLION`, `HOF_UNRANKED`); `World.hallOfFameCredentials` fills the ranking from the snapshot history. New tests in `HallOfFameTest`.
- **Determinism**: the score is a pure, deterministic function of recorded stats + the deterministic ranking snapshots, so same-seed worlds induct identically. Inductions may differ from before (dominant golfers now rank higher), by design; every inductee still meets the unchanged baseline, so `WorldHallOfFameTest` holds.
- **DAG**: `World` already depends on `ranking` and `career`; `HallOfFame`/`HallOfFameCredentials` stay in `career` and take ranking as primitives. No inversion.
