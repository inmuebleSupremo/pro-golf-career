## 1. Ranking + earnings in the score

- [x] 1.1 `CareerConstants`: `HOF_UNRANKED`, `HOF_RANK_PEAK_CAP`, `HOF_SCORE_RANK_PEAK`, `HOF_SCORE_SEASON_AT_ONE`, `HOF_SCORE_EARNINGS_PER_MILLION`.
- [x] 1.2 `HallOfFameCredentials`: add `careerHighRanking`, `seasonsAtNumberOne`, `careerEarnings`; backward-compatible 7-arg constructor (defaults) + a ranking-aware `of` factory (earnings from stats).
- [x] 1.3 `HallOfFame.score`: add the peak-position bonus (linear to a cap), the per-season-at-#1 weight, and the earnings term — score only; `meetsBaseline` unchanged.

## 2. Fill the ranking in the World

- [x] 2.1 `World.hallOfFameCredentials`: fill career-high ranking and seasons-at-#1 from `RankingHistory.careerHighPosition` / `weeksAtNumberOne` over the season-ending snapshots.

## 3. Tests

- [x] 3.1 `HallOfFameTest`: ranking dominance raises the score; earnings raise the score; a reigning #1 can outscore a more-majored compiler; an unranked career gets no dominance bonus (scores exactly its wins); baseline still ignores ranking/earnings.
- [x] 3.2 Full suite green (440); the world HoF election still inducts baseline-eligible golfers and is reproducible.
