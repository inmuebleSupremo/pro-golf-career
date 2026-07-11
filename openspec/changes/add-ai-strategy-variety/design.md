## Context

`Strategy` (Conservative/Balanced/Aggressive) shapes a shot's risk/reward by scaling dispersion, but the AI never varied it: every generated golfer shared `DEFAULT_POLICY = () -> BALANCED`, and `Tournament` read that one strategy for every shot. The tournament already resolves each golfer's strategy via `g.policy().map(DecisionPolicy::defaultStrategy)`, so the seam to vary it per golfer already existed — only the policy was flat.

## Goals / Non-goals

- **Goals**: the AI field plays a realistic spread of attribute-driven styles; aggressive is a genuine risk/reward trade-off (higher variance) rather than a hidden handicap; deterministic and reproducible; no new wiring in the shot/tournament pipeline.
- **Non-goals**: per-shot situational strategy (lie/hazard/scoreboard), playing-to-strengths, a compensating upside for aggression, or any change to the human's per-shot strategy choice. These are the deferred "situational" tier.

## Decisions

### D1 — Per-golfer disposition, fixed at generation, as a pure function of attributes
Disposition is `StrategyDisposition.fromAttributes(attributes)`, computed once in `generateOne` and captured by the golfer's `DecisionPolicy`. Appetite = `norm(DRIVING_DISTANCE) − 0.5·(norm(COURSE_MANAGEMENT) + norm(COMPOSURE))`; above `+STRATEGY_APPETITE_THRESHOLD` → Aggressive, below `−threshold` → Conservative, else Balanced. **Skill-neutral by construction**: the discipline weights (0.5 + 0.5) sum to the distance weight (1.0), so a flat profile yields appetite 0 at any skill level — only *relative* strengths tip the disposition, which is what makes it a temperament rather than a proxy for overall skill. **Alternatives**: (a) subtract the golfer's mean attribute explicitly — rejected, the weight-balancing cancels skill more cleanly and cheaply; (b) recompute disposition from live attributes each call so it drifts as a career evolves — rejected for V1, temperament as a fixed birth trait is simpler and deterministic. Threshold `0.10` was tuned to a ~25/50/25 split (Balanced the plurality, extremes the minority), which reads as realistic golf.

### D2 — Recalibrate the strategy dispersion multipliers (coupled to the putting change)
Because strategy scales dispersion and the putting-model change ~doubled base ball-flight dispersion, the old `0.80/1.30` spread now imposes a ~7-stroke mean penalty on aggression — Conservative would strictly dominate (better mean *and* lower variance), so no disposition variety would be meaningful. Narrowing to `0.88/1.16` restores a real trade-off: measured pure-strategy effect at uniform attributes is Conservative −4.6 (sd 2.56) / Balanced −2.9 (sd 2.86) / Aggressive −0.6 (sd 3.12) — aggressive is clearly the highest-variance style while staying near par. **This is the model's ceiling**: strategy is a single dispersion scalar, so mean and variance move together (a wider tail always costs mean via the convex distance-to-hole). A mean-neutral, variance-only aggression would need a compensating upside (aiming at pins) — that is the deferred situational tier, called out as a non-goal.

### D3 — No pipeline change; disposition flows through the existing seam
`Tournament` already reads `defaultStrategy()`; giving each golfer a policy that returns their disposition is the whole integration. The shot engine, `RoundResolver`, and `StrategyPolicy` are untouched (`StrategyPolicy` still applies one strategy per round — it is per-golfer now, not per-shot).

## Risks / Trade-offs

- **World outcomes shift by design** (non-neutral), like the putting model. Reproducibility (same seed → same world) holds; direction-based world tests hold; the `aggressive-widens-dispersion` shot property holds (1.16 > 0.88). The field mean is ~unchanged (~+3/round) because conservative and aggressive roughly offset.
- **Aggressive is still mean-negative**, so attribute-derived attackers score a touch worse on average and rarely top the scoring average over four rounds; their value is the wider low-round tail. Framed honestly — the change delivers *variety with realistic variance and a modest mean trade-off*, not "aggressive golfers win more." The fuller win-more dynamic needs the deferred situational upside.
- **Retuning `Strategy` also changes the human's options** (aggressive less punishing, conservative slightly looser) — a net improvement to the human's risk/reward feel, and the only place those multipliers are consumed besides the AI.
