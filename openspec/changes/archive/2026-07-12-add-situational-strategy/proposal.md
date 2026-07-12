## Why

AI golfers had an innate strategic disposition, but it only ever *widened dispersion* — so aggression was a pure mean-negative penalty (more spread, no reward), and the golfer never adapted within a round: it played the same way from the fairway as from a bunker, and aimed at the green centre whether the pin was tucked or middle. The AI-variety change flagged this exact gap: aggression needed a *compensating upside* to be a real risk/reward, and strategy needed to respond to the *situation*. This delivers both by making the pin position live in the shot model.

## What Changes

- **The pin's lateral position is live (shot).** Each hole's pin carries a lateral offset from the green centre (already generated per round, previously ignored). Distance-to-the-pin is now measured to the **actual hole**, not the green centre — so a tucked pin plays harder, and finishing near it (not merely on the green) is what leaves a short putt. `ShotContext` carries `pinLateral`; `HoleModel`/`RoundHole` expose it; it threads through the auto and interactive paths.
- **Aggression attacks the flag; conservatism plays safe (policy).** On a confident scoring approach an aggressive disposition aims toward the tucked pin (birdie looks, but the flanking hazard bites), while a conservative one aims at the safe green centre — the attack scaled by shot confidence, so a wedge hunts the flag but a long iron plays the middle. Measured: attacking a tucked pin nearly doubles birdie-range approaches (~11% vs ~6% inside 3 yд) at the cost of ~15% fewer greens held.
- **The golfer adapts to the lie (policy).** From a difficult lie — deep rough, bunker, recovery, trees — the golfer plays conservatively to recover, whatever their disposition; from a clean lie it keeps its disposition. The ball's lie (already tracked) now feeds the decision.
- **Aggression is now a genuine risk/reward.** Because pin-attacking supplies the upside, the crude strategy dispersion spread is narrowed (`0.88/1.16 → 0.91/1.12`): the three dispositions now score within ~0.3 strokes of each other (mean-neutral) while aggressive carries clearly the widest score variance and the lowest greens-in-regulation (~64% vs ~72% conservative) — boom-or-bust flag-hunting instead of a hidden handicap.
- **The human sees the pin.** `ShotSituation` now exposes `pinLateral`, so a human player can choose to attack a tucked flag or play the centre — the same decision the AI makes.

Explicitly out of scope: scoreboard-aware aggression (pressing when behind), playing-to-specific-strengths, lay-up vs go-for-it on par 5s, and pin depth within the green (only lateral is live). Field-wide world scoring drifts up modestly (~+3.6 → ~+4.2/round) because live tucked pins make holes genuinely harder — realistic, and the scoring-calibration guard still holds.

## Capabilities

### Modified Capabilities
- `shot-resolution`: distance-to-pin is measured to the actual pin (its lateral offset), and the AI decision policy is now situational — adapting strategy to the lie and aiming a disposition- and distance-scaled fraction toward a tucked pin — so aggression is a real risk/reward (attack the flag for birdie looks at the cost of greens) rather than only wider dispersion.

## Impact

- **Codebase**: `ShotContext` gains `pinLateral`; `ShotResolver` measures distance-to-pin to the actual hole; `HoleModel`/`RoundHole` expose the pin lateral; `StrategyPolicy.decide` gains lie + pin and does the situational aim/strategy; `Strategy` gains `pinAttack()` and retuned dispersion multipliers; `SimConstants` gains pin-attack fade + lie-caution constants; `RoundResolver`/`PlayableRound`/`PlayableHole` thread the pin/lie; `ShotSituation` exposes the pin. New `SituationalStrategyTest` + `PinAttackingTest`.
- **Determinism / fidelity**: the situational aim is a pure, deterministic function of lie/pin/distance; the auto and interactive paths compute it identically, so reproducibility and playable-event fidelity hold. Scores shift by design (pins live); opening/centre-pin behaviour where `pinLateral = 0` is unchanged.
- **DAG**: unchanged — all within `sim.shot` / `sim.course` / `sim.play`, which already depend as needed.
