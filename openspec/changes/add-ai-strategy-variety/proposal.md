## Why

Every AI golfer plays a fixed `Strategy.BALANCED` on every shot — the whole field shares one `DEFAULT_POLICY = () -> BALANCED` and the `StrategyPolicy` is "intentionally basic." So the living world has no stylistic variety: no attackers, no grinders, no risk/reward temperament. That flattens statistical realism (Pillar 3) — the scoring spread and the way careers rise and fall all read the same — and it makes the human's own club/target/**risk** choice feel arbitrary, because no AI rival ever plays differently. This is the next depth-pass item: give the field genuine, attribute-driven playing styles.

## What Changes

- **Innate strategic disposition from attributes (population).** Each generated golfer is assigned a deterministic strategic disposition — their risk appetite — computed once at generation from their attributes: a golfer whose driving distance outstrips their course management and composure is a boom-or-bust attacker (`AGGRESSIVE`); a disciplined, composed, well-managed golfer protects par (`CONSERVATIVE`); most sit in between (`BALANCED`). The mapping (`StrategyDisposition.fromAttributes`) is pure and **skill-neutral** — its discipline weights sum to the distance weight, so a flat profile scores zero appetite at any overall skill level, and only relative strengths tip the disposition. The shared `DEFAULT_POLICY` is replaced by a per-golfer policy returning that disposition, which the tournament already reads via `DecisionPolicy.defaultStrategy()`, so it flows to every shot with no other wiring.
- **Recalibrate the strategy dispersion multipliers (shot).** Strategy scales shot dispersion, and the putting-model change roughly doubled the base ball-flight dispersion — so the old `0.80 / 1.00 / 1.30` spread now imposes a ~7-stroke mean penalty on aggressive play, making conservative strictly dominant (a losing caste, not a real choice). Narrow the spread to `0.88 / 1.00 / 1.16` so aggressive is a genuine risk/reward trade-off: clearly the highest-variance style (a wider boom/bust tail) while only modestly worse on average, and conservative the steady low-ceiling grinder. This is a coupled calibration the disposition-variety surfaces, exactly as the putting fix surfaced ball-striking.

Measured: the field splits ~25% Conservative / ~50% Balanced / ~25% Aggressive; per-disposition scoring means/variances order correctly (conservative steadiest & lowest, aggressive highest-variance & near par); a real 5-season World stays at ~+3 strokes/round with ~30 putts/round (the dispositions roughly offset, so the field mean is unchanged).

Explicitly out of scope (the deferred "situational" tier): per-shot strategy that adapts to lie / hazards / scoreboard position; playing-to-strengths (attacking reachable par 5s, laying up to a favoured wedge distance); giving aggression a compensating upside (aiming at tucked pins for more birdie chances) so it can be mean-neutral rather than a pure variance/mean trade-off. The human's own strategy is still their per-shot choice; the created player's sim fallback stays Balanced.

## Capabilities

### Modified Capabilities
- `golfer-population`: each generated golfer additionally carries an innate strategic disposition derived deterministically from their attributes, so the AI field plays a spread of risk/reward styles rather than a uniform Balanced.

## Impact

- **Codebase**: new `sim.population.StrategyDisposition` + `PopulationConstants.STRATEGY_APPETITE_THRESHOLD`; `PopulationGenerator` assigns a per-golfer disposition policy (drops the shared `DEFAULT_POLICY`); `Strategy` dispersion multipliers retuned. New `StrategyDispositionTest` + a field-spread test in `PopulationGeneratorTest`. No change to the shot pipeline, tournament wiring (already reads `defaultStrategy()`), or the DAG.
- **Determinism**: disposition is a pure function of attributes fixed at generation, so same-seed populations reproduce their dispositions; run-vs-run worlds stay reproducible. Deliberately non-neutral (world outcomes shift by design, like the putting model) — world tests are direction-based and hold; the `aggressive-widens-dispersion` shot property still holds (1.16 > 0.88).
- **Boundary**: `StrategyDisposition` lives in `population` and depends only on `core` + `shot.Strategy` (already a dependency); nothing lower imports it.
