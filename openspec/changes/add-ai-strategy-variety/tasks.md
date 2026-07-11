## 1. Per-golfer strategic disposition

- [x] 1.1 Add `PopulationConstants.STRATEGY_APPETITE_THRESHOLD` (0.10) — the appetite magnitude beyond which disposition tips off Balanced.
- [x] 1.2 Add `com.progolf.sim.population.StrategyDisposition.fromAttributes(Attributes)` — pure, skill-neutral: appetite = `norm(DRIVING_DISTANCE) − 0.5·(norm(COURSE_MANAGEMENT) + norm(COMPOSURE))`, thresholded into AGGRESSIVE / CONSERVATIVE / BALANCED.
- [x] 1.3 `PopulationGenerator.generateOne`: compute the disposition once and give the golfer a `DecisionPolicy` returning it; drop the shared `DEFAULT_POLICY`.

## 2. Recalibrate strategy multipliers

- [x] 2.1 `Strategy`: narrow the dispersion multipliers from 0.80/1.00/1.30 to 0.88/1.00/1.16 so aggressive is a real risk/reward trade-off (highest variance, only modestly worse mean) under the wider post-putting-model base dispersion, instead of a strictly-dominated handicap.

## 3. Tests

- [x] 3.1 `StrategyDispositionTest`: a bomber-with-weak-discipline → AGGRESSIVE; a disciplined/composed golfer → CONSERVATIVE; a flat profile → BALANCED; skill-neutral (flat profile Balanced at low and high skill).
- [x] 3.2 `PopulationGeneratorTest`: the generated field shows a spread of dispositions (all three present) and reproduces them from the seed.
- [x] 3.3 Full suite green (397): reproducibility, the `aggressive-widens-dispersion` shot property, and all world/tournament direction tests hold.

## 4. Verify

- [x] 4.1 Measure the field: ~25/50/25 Conservative/Balanced/Aggressive split; per-disposition scoring orders correctly (conservative steadiest & lowest mean, aggressive highest variance & near par); a real 5-season World stays ~+3 strokes/round with ~30 putts/round (dispositions offset, field mean unchanged).
