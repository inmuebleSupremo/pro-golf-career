## 1. Situational pressure model (tournament)

- [x] 1.1 `TournamentConstants`: pressure tunables (base, round-3/round-4 weights, prestige weights, contention strokes).
- [x] 1.2 `PressureModel.forRound(round, prestige, strokesBehind)` — pure [0,1]: 0 in opening rounds, building on closing rounds, scaled by prestige, decaying with strokes behind the lead.

## 2. Apply pressure in the Tournament

- [x] 2.1 `playRound`: compute the pre-round leader once (`leaderCumulative`); pass each competitor's pressure (from their pre-round strokes-behind) into `playCompetitorRound`, which feeds `toGolferState(pressure)`.
- [x] 2.2 Public `pressureFor(fieldIndex, round)` and `playoffPressure()` so the interactive path computes identical pressure; apply peak pressure in `playPlayoffHole`.

## 3. Human player + psychologist

- [x] 3.1 `PlayableEvent`: build the player's per-round state with `toGolferState(tournament.pressureFor(playerFieldIndex, roundNo))`, and the playoff hole with `playoffPressure()`; drop the fixed zero-pressure state.
- [x] 3.2 `ShotResolver`: `effectivePressure = pressure × (1 − mentalSupport)` in the dispersion multiplier and the putt make-% (psychologist relieves pressure as well as fatigue).

## 4. Tests

- [x] 4.1 `PressureModelTest`: opening rounds 0; builds to the final round; higher prestige more; decays with contention; always within [0,1].
- [x] 4.2 `PressureShotEffectTest`: pressure worsens a nervy golfer's scores; Composure resists; a psychologist relieves it; zero pressure is neutral (composure-independent).
- [x] 4.3 Update `EventPrestigeTest` (prestige scales rewards; closing play now differs) and `TournamentInteractivePlayoffTest` (two-pass fixed point to force the tie under pressure coupling).
- [x] 4.4 Full suite green (415); playable-event fidelity and reproducibility hold.

## 5. Verify

- [x] 5.1 Measure: a Composure-25 golfer plays ~6 strokes worse at peak pressure, a Composure-95 golfer barely a stroke, a psychologist claws back ~half; a real 5-season World stays ~+3/round with ~30 putts (pressure is situational, not a blanket penalty).
