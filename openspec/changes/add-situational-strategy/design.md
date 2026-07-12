## Context

Golfers carried an innate `Strategy` disposition, but `StrategyPolicy.decide(remaining)` used it as a fixed per-shot strategy whose only effect was a dispersion multiplier — a pure mean-negative penalty for aggression, and no adaptation to lie or pin. Two facts made a situational upgrade tractable: the ball's `lie` is already tracked through the resolvers (from the putting change), and each round's pin already carries a lateral offset (`PinPosition.lateralOffset`, generated but ignored in resolution).

## Goals / Non-goals

- **Goals**: make the pin lateral live (distance measured to the actual hole); a situational policy that recovers conservatively from bad lies and attacks tucked pins by disposition + confidence; aggression as a genuine risk/reward (mean-neutral, variance-positive) rather than a pure penalty.
- **Non-goals**: scoreboard-aware aggression, playing-to-strengths, lay-up vs go-for-it, pin depth within the green, a short-game/scrambling model.

## Decisions

### D1 — Make the pin lateral live in the shot model
`ShotContext` gains `pinLateral`; `ShotResolver` measures `distanceRemaining = hypot(pinDistance − carry, lateral − pinLateral)`. `HoleModel.pinLateral()` (default 0) is overridden by `RoundHole` to return the round's pin offset, and threaded through `RoundResolver`/`PlayableRound`/`PlayableHole`. Putts are unaffected (the putting model computes remaining from its own leave, not this geometry). Backward-compatible `ShotContext` constructors default `pinLateral` to 0, so a centre pin reproduces prior behaviour and every existing fixture compiles unchanged.

### D2 — Situational aim in the policy, keyed on lie / pin / distance
`decide(remaining, lie, pinLateral)`: from a lie whose recovery-difficulty ≥ `RECOVERY_CAUTION_THRESHOLD` the effective strategy is forced to Conservative; otherwise it is the disposition. The aim is `strategy.pinAttack() × confidence(remaining) × pinLateral`, where `pinAttack` is 0 (conservative) / 0.4 (balanced) / 1.0 (aggressive) and `confidence` fades linearly from full at `PIN_ATTACK_FADE_NEAR` (120 yд) to zero at `FAR` (190 yд). So a wedge hunts the flag while a long iron plays the centre — which is also why pin-attacking with a mid/long iron (dispersion ≫ pin offset) is not suicidal.

### D3 — Narrow the strategy dispersion spread; let pin-attacking carry the risk/reward
Pin-attacking supplies aggression's upside (closer approaches → birdie looks) and its risk (missed greens), so the crude dispersion spread from the AI-variety change is redundant and double-penalised aggression. Narrowing `0.88/1.16 → 0.91/1.12` lands the three dispositions within ~0.3 strokes of each other (mean-neutral) while aggression keeps the widest score variance and the lowest greens-in-regulation. **This is the calibration that makes disposition variety a true risk/reward** rather than a hidden handicap — the outcome the AI-variety change deferred.

### D4 — Expose the pin to the human
`ShotSituation` gains `pinLateral` so a human can make the same attack-or-play-safe decision the AI does.

## Risks / Trade-offs

- **World scoring drifts up** (~+3.6 → ~+4.2/round) because live tucked pins genuinely harden holes (centre-aimed approaches leave longer putts). Realistic; the `ScoringCalibrationTest` guard still holds (field mean ~−1 in calm). Reproducibility and playable-event fidelity are preserved (the aim is deterministic and identical across paths).
- **The pin-attack effect is subtle in scoring but clear in style** — aggressive golfers hit visibly fewer greens with more variance; the mean is near-neutral. This matches reality (aggressive vs conservative course strategy is a small mean effect but a real stylistic one), and is what makes disposition matter without a runaway winner.
- **Dispersion ≫ pin offset for long clubs** — attacking with a long iron would mostly miss, so the confidence fade restricts attacks to short approaches. Pin *depth* within the green is not modelled (only lateral); a fuller pin model is a later refinement.
