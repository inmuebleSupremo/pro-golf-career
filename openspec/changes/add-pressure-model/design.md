## Context

The shot model already reads a `pressure` value from `GolferState` (widening dispersion via `pressureMult`, lowering putt make-% ), resisted by Composure — but `Player.toGolferState(...)` was always called with `0.0` in world play, so Composure and the psychologist's mental-game value were dead in-world. Two facts make activation clean: (1) the Tournament plays rounds **field-wide sequentially** (round 1 for all, cut, then rounds 2-4), so the leaderboard *going into* a round is known; (2) `PlayableEvent` orchestrates the human's rounds against the same Tournament, so it can read the same standings.

## Goals / Non-goals

- **Goals**: pressure that is situational (Sunday/major/contention), lights up Composure and the psychologist, applies to the AI field and the human, is deterministic, and keeps field-wide scoring realistic.
- **Non-goals**: crowd/rivalry/history pressure, opening-round nerves, off-course pressure, a clutch attribute beyond Composure, or gap-to-nearest-competitor contention (deferred).

## Decisions

### D1 — Pressure = base × round × prestige × contention, computed from the pre-round leaderboard
`PressureModel.forRound(round, prestige, strokesBehind)` (pure): round weight is 0 for rounds 1-2, `0.4` (moving day), `1.0` (Sunday); prestige weight `0.6/0.8/1.0` (regular/signature/major); contention decays linearly from 1 at the lead to 0 by `PRESSURE_CONTENTION_STROKES` (8) back. `strokesBehind` uses the competitor's cumulative minus the leader's, **both pre-round** — computed once per round before anyone posts a score, so it isn't polluted as the field plays. **Alternative** (gap to nearest competitor, so a runaway leader feels less than a one-shot lead) is more realistic but needs richer standings maths; deferred, noted as a non-goal. Opening rounds weighting to 0 keeps rounds 1-2 byte-identical to before, limiting the blast radius.

### D2 — The same pressure function feeds the AI field and the human, preserving fidelity
The auto path (`playCompetitorRound`) and the human path (`PlayableEvent`) both derive the player's pressure from the identical pre-round standings via `Tournament.pressureFor(fieldIndex, round)`. Because opening rounds carry no pressure and each round's pressure depends only on the prior rounds' standings (which match between the paths by induction), a fully-simmed `PlayableEvent` stays byte-identical to automatic resolution — the existing fidelity guardrail still holds. A sudden-death playoff uses `playoffPressure()` = peak (final-round, leader-level) pressure, applied identically in both paths.

### D3 — The psychologist relieves pressure as well as fatigue
`ShotResolver` now computes `effectivePressure = pressure × (1 − mentalSupport)` and uses it in both the dispersion multiplier and the putt make-% (mirroring the existing `effectiveFatigue`). This finally makes the sports psychologist a meaningful hire in-world — fatigue is low in the small-world cadence, but pressure bites on Sundays. Neutral at `mentalSupport = 0`.

### D4 — Prestige now shapes closing play (a deliberate invariant change)
Feeding prestige into pressure means a major plays harder to close, which contradicts the old event-prestige invariant "prestige never changes play". That invariant is evolved: prestige still never enters the base shot mechanics (the shot model has no prestige input), but it scales the situational pressure of the closing stages. The `event-prestige` spec's "prestige does not change play" scenario is replaced with "higher prestige raises closing-round pressure".

## Risks / Trade-offs

- **World outcomes shift on closing rounds** (non-neutral, by design, like the putting/AI-variety changes). Reproducibility and playable-event fidelity hold; field-wide scoring is essentially unchanged (~+3/round) because pressure is situational. Tests asserting cross-prestige score identity (`EventPrestigeTest`) and a pressure-free leaderboard (`TournamentInteractivePlayoffTest`) were updated — the latter now forces its tie with a two-pass fixed point, since the player's leaderboard position now couples the field's pressure.
- **Extreme choke magnitude**: at peak pressure a Composure-25 golfer plays ~6 strokes worse — large, but it is the extreme corner (max pressure × very low composure); typical situations and attributes are far milder. Tunable in `TournamentConstants`.
- **Contention proxy**: strokes-behind-the-lead treats a runaway leader as maximally pressured; a gap-based model is the noted refinement.
