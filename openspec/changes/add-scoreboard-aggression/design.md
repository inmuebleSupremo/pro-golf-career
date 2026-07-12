## Context

The tournament resolved each competitor's round with their fixed innate `Strategy` (disposition). It already plays rounds field-wide sequentially and, for the pressure model, computes each competitor's strokes behind the pre-round leader — exactly the signal a scoreboard-aware strategy needs.

## Goals / Non-goals

- **Goals**: closing-round strategy that presses chasers and protects leaders, for the AI field and the human, deterministic and fidelity-preserving, reusing the pressure model's leaderboard machinery.
- **Non-goals**: shot-by-shot mid-round reaction, giving-up behaviour, off-course scoreboard effects.

## Decisions

### D1 — Bend the strategy from the pre-round leaderboard; override only in the extremes
`ScoreboardStrategy.adjust(disposition, roundNo, strokesBehind, leaderMargin)` (pure): before `SCOREBOARD_CLOSING_ROUND` it returns the disposition; on the closing rounds, `strokesBehind ≥ SCOREBOARD_PRESS_BEHIND` → Aggressive (press), a leader (`strokesBehind == 0`) with `leaderMargin ≥ SCOREBOARD_PROTECT_MARGIN` → Conservative (protect), else the disposition. Overriding only in the extremes keeps temperament meaningful in the pack while delivering the Sunday charge/protect drama. `strokesBehind` reuses the pressure model's pre-round leader; `leaderMargin` needs the pre-round second-place score, computed once per round (like the leader) so it is not polluted as competitors post.

### D2 — Compute it in `playRound`, expose it for the interactive path
`playRound` computes the leader and second-place pre-round, and passes each competitor's adjusted strategy into `playCompetitorRound` (which no longer derives the strategy itself). `Tournament.roundStrategyFor(fieldIndex, roundNo)` exposes the same computation so `PlayableEvent` sets the player's per-round sim strategy identically — the pattern the pressure model already uses (`pressureFor`). Because both paths read the same pre-round standings and the opening rounds are unbent, a fully-simmed event stays byte-identical to automatic resolution (fidelity by construction). A sudden-death playoff is all-tied (0 behind, 0 margin), so the adjustment is a no-op there — the disposition stands.

## Risks / Trade-offs

- **Closing-round scores shift by design** (non-neutral, like the other situational changes); reproducibility and playable-event fidelity hold; opening rounds are unchanged. Field-wide scoring is largely unaffected (pressing and protecting roughly offset, and only closing rounds are touched).
- **Override, not blend**: a conservative chaser plays fully aggressive (not "a bit more aggressive"). This is deliberate for the drama and simplicity; a graded blend is a possible later refinement.
- **Round-level, not shot-level**: the scoreboard is read once per round from the pre-round standings, not re-evaluated mid-round on a birdie run — a deferred refinement noted as a non-goal.
