## 1. Live pin lateral (shot model)

- [x] 1.1 `HoleModel.pinLateral()` (default 0); `RoundHole` overrides it to the round's pin lateral offset.
- [x] 1.2 `ShotContext`: add `pinLateral` (backward-compatible constructors default it to 0).
- [x] 1.3 `ShotResolver`: measure `distanceRemaining` to the actual pin (`lateral − pinLateral`).
- [x] 1.4 Thread the pin lateral through `RoundResolver`/`PlayableRound`/`PlayableHole` into the context and the decision.

## 2. Situational policy

- [x] 2.1 `Strategy`: add `pinAttack()` (conservative 0 / balanced 0.4 / aggressive 1.0).
- [x] 2.2 `SimConstants`: `PIN_ATTACK_FADE_NEAR`/`FAR` and `RECOVERY_CAUTION_THRESHOLD`.
- [x] 2.3 `StrategyPolicy.decide(remaining, lie, pinLateral)`: force conservative from a difficult lie; aim `pinAttack × confidence(distance) × pinLateral`; keep a centre-pin convenience overload.

## 3. Recalibrate + expose

- [x] 3.1 Narrow the strategy dispersion spread (`0.88/1.16 → 0.91/1.12`) so pin-attacking carries the risk/reward and the dispositions are mean-neutral / variance-positive. Verify world scoring stays believable (~+4.2/round) and `ScoringCalibrationTest` holds.
- [x] 3.2 `ShotSituation`: expose `pinLateral` so a human can attack or play safe.

## 4. Tests

- [x] 4.1 `SituationalStrategyTest`: a difficult lie forces conservative; aggression aims at the pin, conservatism at centre; the attack fades with distance; the aim is signed toward the pin.
- [x] 4.2 `PinAttackingTest`: attacking the pin leaves closer approaches (more birdie-range) but fewer greens; distance is measured to the actual pin, not the centre.
- [x] 4.3 Full suite green (424); playable-event fidelity and reproducibility hold.

## 5. Verify

- [x] 5.1 Measure the three dispositions at fixed attributes: mean-neutral (within ~0.3 strokes), aggressive with the highest score variance and lowest GIR (~64% vs ~72%); a wedge attack nearly doubles inside-3yd approaches (~11% vs ~6%) at the cost of ~15% fewer greens.
