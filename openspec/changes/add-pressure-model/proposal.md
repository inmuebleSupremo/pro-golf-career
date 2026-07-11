## Why

The shot engine already models competitive **pressure** — it widens dispersion and lowers putt make-% for a golfer under the gun, resisted by the **Composure** attribute — but world play always feeds it `0`. So Composure is a dead attribute in-world, the sports psychologist's mental-game value is invisible, and there is no on-course drama: a nervy journeyman closes a major exactly as calmly as a stone-cold veteran. This is the first Tier-3 depth item: make pressure real, which lights up Composure, the psychologist, and Sunday/major tension all at once.

## What Changes

- **Situational pressure generation (tournament).** A new `PressureModel.forRound(round, prestige, strokesBehind)` produces a pressure value in [0,1] from the competitive situation: it is zero in the opening rounds, builds on the closing rounds (moving day < Sunday), scales with event prestige (**a major's Sunday is the most**), and weighs only on those **in contention** — decaying to zero as a competitor falls out of the hunt. The Tournament feeds each auto-resolved competitor their round's pressure (computed from the pre-round leaderboard), and a sudden-death playoff is peak pressure.
- **The human player feels it too.** `PlayableEvent` computes the player's pressure each round from the same pre-round standings via the Tournament's `pressureFor`, so the player's own Composure finally matters on their Sundays and majors — and a fully-simmed event stays byte-identical to automatic resolution (the pressure inputs are shared and identical between the two paths).
- **The psychologist relieves pressure.** In the shot model, mental support (the sports psychologist) now softens **both** fatigue and pressure (`effectivePressure = pressure × (1 − mentalSupport)`), so a psychologist is a meaningful hire for a nervy golfer — not just a fatigue aid that rarely bites in-world.
- **Prestige now shapes closing play, not only rewards.** Because a major raises pressure, majors are genuinely harder to close. This deliberately evolves the event-prestige invariant "prestige never changes play": prestige still never enters the base shot mechanics, but it now scales the situational pressure of the closing stages.

Measured: a nervy golfer (Composure 25) plays ~6 strokes worse under peak pressure, a composed one (95) barely a stroke; a psychologist claws back roughly half the choke. Field-wide world scoring is essentially unchanged (~+3/round, ~30 putts) because pressure bites only contenders on closing rounds — it is situational drama, not a blanket penalty.

Explicitly out of scope: pressure from crowd/history/rivalries, first-tee nerves in opening rounds, pressure affecting off-course decisions, or a distinct "clutch gene" beyond Composure. The gap-to-nearest-competitor (a runaway leader feeling less than a one-shot lead) is a possible later refinement; V1 uses strokes-behind-the-lead as the contention proxy.

## Capabilities

### Modified Capabilities
- `shot-resolution`: shots consume a situational **pressure** input — widening dispersion and lowering putt make probability, resisted by Composure — and mental support (psychologist) now relieves pressure as well as fatigue. Neutral at zero, so calm/opening play is unchanged.
- `event-prestige`: prestige now also scales the **closing-round pressure** of an event (a major is the most pressure-filled to close), so it shapes closing play — while still never entering the base shot mechanics. Replaces the prior "prestige never changes play" invariant.
- `tournament-play`: the tournament generates **situational pressure** for each competitor from the round, the event's prestige, and their contention (strokes behind the leader, pre-round), applied to the whole field and the interactive player, and peaking in a sudden-death playoff.

## Impact

- **Codebase**: new `sim.tournament.PressureModel` + pressure tunables in `TournamentConstants`; `Tournament` computes per-competitor pressure (`playRound`/`playCompetitorRound`, `leaderCumulative`, public `pressureFor`/`playoffPressure`); `PlayableEvent` feeds the player their per-round/playoff pressure; `ShotResolver` folds mental support into effective pressure. New `PressureModelTest` + `PressureShotEffectTest`; `EventPrestigeTest` and `TournamentInteractivePlayoffTest` updated for the new (prestige-affects-closing-play, pressure-couples-the-field) reality.
- **Determinism / fidelity**: pressure is a pure, deterministic function of the pre-round standings (no RNG), and the auto and interactive paths compute it from identical inputs, so same-seed worlds reproduce and a simmed `PlayableEvent` stays byte-identical to automatic resolution. Opening rounds (pressure 0) are unchanged; only closing-round contenders' scores shift — by design.
- **DAG**: `PressureModel` lives in `tournament`; `PlayableEvent` already depends on `tournament`. No inversion.
