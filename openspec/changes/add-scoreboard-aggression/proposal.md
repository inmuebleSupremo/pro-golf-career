## Why

AI golfers played their innate disposition on every hole of every round — a golfer six back on Sunday pressed no harder than one leading by six. Real golf's closing-round drama is exactly this: chasers attack to make up ground while front-runners protect a lead. This is the second deferred situational-strategy refinement, and it reuses the strokes-behind-the-leader the pressure model already computes.

## What Changes

- **Scoreboard-aware strategy (tournament).** On the closing rounds a competitor's strategy bends to their position (computed from the pre-round leaderboard): a golfer at least `SCOREBOARD_PRESS_BEHIND` strokes behind the lead **presses** (plays aggressively, whatever their disposition); a front-runner leading the field by at least `SCOREBOARD_PROTECT_MARGIN` **protects** (plays conservatively); everyone in the pack, and every opening round, plays their innate disposition. `ScoreboardStrategy.adjust` is a pure function of the situation.
- **The human bends too.** `PlayableEvent` sets the player's per-round sim strategy from `Tournament.roundStrategyFor`, computed from the same pre-round standings — so a fully-simmed event stays byte-identical to automatic resolution, and the human's own closing-round tactics reflect the scoreboard.

Measured: on the final round noticeably more of the field plays aggressively than in an opening round (chasers pressing), and comfortable leaders drop to conservative. Combined with the pressure model, Sunday now has real texture — a nervy chaser pressing can charge or blow up, a leader protecting plays safe under the gun.

Explicitly out of scope: shot-by-shot mid-round scoreboard reaction (a birdie run changing tactics on the back nine); giving-up behaviour when hopelessly behind; scoreboard effects on off-course decisions.

## Capabilities

### Modified Capabilities
- `tournament-play`: on the closing rounds a competitor's strategy is bent by the scoreboard — chasers press, comfortable leaders protect — for the whole field and the interactive player, computed deterministically from the pre-round standings.

## Impact

- **Codebase**: new `sim.tournament.ScoreboardStrategy` + scoreboard tunables in `TournamentConstants`; `Tournament` computes the pre-round second-place score, applies the adjusted strategy in `playRound`/`playCompetitorRound`, and exposes `roundStrategyFor`; `PlayableEvent` uses it for the player's per-round strategy. New `ScoreboardStrategyTest` + `ScoreboardTournamentTest`.
- **Determinism / fidelity**: the adjustment is a pure, deterministic function of the pre-round standings, computed identically on the auto and interactive paths, so reproducibility and playable-event fidelity hold. Opening rounds are unchanged; closing-round scores shift by design.
- **DAG**: `ScoreboardStrategy` lives in `tournament` and depends only on `shot.Strategy` (already used); `PlayableEvent` already depends on `tournament`. No inversion.
