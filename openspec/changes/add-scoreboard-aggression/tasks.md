## 1. Scoreboard strategy model

- [x] 1.1 `TournamentConstants`: `SCOREBOARD_CLOSING_ROUND` (3), `SCOREBOARD_PRESS_BEHIND` (4), `SCOREBOARD_PROTECT_MARGIN` (4).
- [x] 1.2 `ScoreboardStrategy.adjust(disposition, roundNo, strokesBehind, leaderMargin)` — pure: opening rounds keep the disposition; a chaser presses (Aggressive); a comfortable leader protects (Conservative); the pack keeps the disposition.

## 2. Apply in the tournament

- [x] 2.1 `Tournament`: add `secondCumulative(roundNo)` (pre-round second place); in `playRound` compute each competitor's adjusted strategy and pass it into `playCompetitorRound` (no longer derives its own strategy).
- [x] 2.2 Expose `roundStrategyFor(fieldIndex, roundNo)` and use it in `PlayableEvent` for the player's per-round sim strategy (fidelity-preserving).

## 3. Tests

- [x] 3.1 `ScoreboardStrategyTest`: opening rounds keep the disposition; a chaser presses; a comfortable leader protects; the pack keeps the disposition.
- [x] 3.2 `ScoreboardTournamentTest`: over a played field, more competitors play aggressively on the final round than in an opening round (chasers pressing).
- [x] 3.3 Full suite green (433); reproducibility, playable-event fidelity, and the calibration guard hold.
