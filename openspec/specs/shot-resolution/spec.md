# shot-resolution Specification

## Purpose
TBD - created by archiving change add-shot-resolution-core. Update Purpose after archive.
## Requirements
### Requirement: Single Shared Resolution Model

The simulation SHALL resolve all shots through one probability model. Human and simulation-controlled golfers SHALL be resolved by the same model with no separate scoring, dispersion, or outcome logic. Any difference in results SHALL arise only from inputs (attributes, state, decisions, environment, seed), never from control type.

#### Scenario: Identical inputs yield identical distribution regardless of control type

- **WHEN** a human-controlled golfer and a simulation-controlled golfer present identical attributes, state, decision, environment, and seed coordinate
- **THEN** the resolution SHALL produce the identical outcome distribution and the identical sampled outcome

#### Scenario: No control-type branch in the model

- **WHEN** the resolution model executes
- **THEN** it SHALL NOT read control type as an input to shaping, sampling, or safety-net steps

### Requirement: Two Resolution Entry Points

The shared model SHALL expose two entry points that compose the same per-shot core:

- **resolveShot** — resolves exactly one shot from an explicit decision (club, target, strategy) plus game state, for interactive per-shot play.
- **resolveRound** — resolves a full hole and/or round for a golfer without an interactive loop, by repeatedly composing the per-shot core with automatically-derived decisions.

`resolveRound` SHALL produce outcomes drawn from the same distributions as an equivalent sequence of `resolveShot` calls.

#### Scenario: resolveRound composes resolveShot

- **WHEN** a hole is resolved via `resolveRound`
- **THEN** each shot within it SHALL be produced by the same per-shot core used by `resolveShot`, seeded by the same shot-level coordinate

#### Scenario: Distribution equivalence across entry points

- **WHEN** the same golfer plays the same hole under the same conditions and seed via `resolveRound` versus an equivalent series of `resolveShot` calls with matching decisions
- **THEN** the resulting shot outcomes SHALL be identical

### Requirement: Required Shot Decision Inputs

A shot SHALL NOT be resolved unless a complete decision is supplied: exactly one **club**, one **target**, and one **strategy** (Conservative, Balanced, or Aggressive). Strategy SHALL adjust the risk/reward shape of the outcome distribution but SHALL NOT override attribute influence.

#### Scenario: Incomplete decision rejected

- **WHEN** `resolveShot` is invoked without a club, target, or strategy
- **THEN** resolution SHALL be rejected before any outcome is produced

#### Scenario: Aggressive strategy widens risk

- **WHEN** strategy changes from Conservative to Aggressive with all else held constant
- **THEN** the outcome distribution SHALL shift toward both greater potential reward and greater dispersion, without changing the underlying attribute contributions

### Requirement: Continuous Controlled Randomness

Shot outcomes SHALL be sampled from continuous probability distributions that cluster around the expected outcome. Average outcomes SHALL be the most common; exceptional and catastrophic outcomes SHALL occur but SHALL be rare. Over large samples, higher attributes SHALL yield better mean outcomes while lower attributes SHALL remain capable of occasional exceptional results.

#### Scenario: Central tendency dominates

- **WHEN** a large number of identical shots are resolved
- **THEN** results SHALL concentrate near the expected outcome, with extreme results significantly less frequent than average ones

#### Scenario: Skill expresses over sample size

- **WHEN** two golfers differing only in a relevant attribute each resolve a large number of identical shots
- **THEN** the higher-attribute golfer SHALL show a better mean outcome, while the lower-attribute golfer SHALL still occasionally produce an exceptional result

### Requirement: Safety-Net Bounding

Resolution SHALL apply safety-net mechanics that reduce unrealistic or excessively punishing outcomes without eliminating the possibility of poor shots. Safety-net mechanics SHALL be applied after sampling and SHALL never guarantee a good outcome.

#### Scenario: Extreme punishment dampened

- **WHEN** a sampled outcome would be unrealistically catastrophic for the given decision and conditions
- **THEN** the safety net SHALL dampen it toward a plausible poor result rather than emit the raw extreme

#### Scenario: Poor shots remain possible

- **WHEN** the safety net is active
- **THEN** genuinely poor outcomes SHALL still occur at a believable frequency; the net SHALL NOT floor outcomes to "acceptable"

### Requirement: Complete Explainable Outcome

Every resolved shot SHALL produce a complete Outcome containing at minimum: final surface, distance remaining to the pin, lateral result, any hazard entered, penalties incurred, and the resulting shot count. The Outcome SHALL be explainable — the dominant contributing factors (e.g., strong crosswind, poor lie, aggressive strategy, fatigue, excellent execution) SHALL be recoverable from the resolution.

#### Scenario: Outcome is fully populated

- **WHEN** a shot is resolved
- **THEN** the produced Outcome SHALL include final surface, distance remaining, penalties, and shot count, with no field left undefined

#### Scenario: Dominant factors are recoverable

- **WHEN** a resolved shot deviates notably from its expected result
- **THEN** the resolution SHALL expose the dominant contributing factors so the result can be explained rather than appearing arbitrary

### Requirement: Deterministic Sampling via Seed Hierarchy

Every sampled value in resolution SHALL be drawn from a generator derived through the world seed hierarchy at the shot's coordinate. Resolution SHALL contain no ambient randomness.

#### Scenario: Resolution is reproducible

- **WHEN** the same shot coordinate and world state are resolved more than once
- **THEN** the sampled outcome SHALL be identical each time

### Requirement: Shots Consume Equipment Characteristics

A shot SHALL additionally consume the golfer's active equipment characteristics, carried on the golfer's temporary state: forgiveness SHALL reduce dispersion and power SHALL extend reach. These characteristics SHALL be neutral by default, so that standard (baseline) equipment reproduces prior shot behaviour exactly and never persists into permanent attributes.

#### Scenario: Standard equipment is neutral

- **WHEN** a shot is resolved with neutral (baseline) equipment characteristics
- **THEN** the outcome SHALL match the outcome with no equipment influence

#### Scenario: Stronger equipment tightens and extends

- **WHEN** a shot is resolved with above-baseline forgiveness and power
- **THEN** its dispersion SHALL be no larger and its reach no shorter than with neutral equipment

### Requirement: Shots Consume Support Characteristics

A shot SHALL additionally consume the golfer's active staff-support characteristics, carried on the golfer's temporary state: strategic support SHALL reduce the likelihood of a mishit, and mental support SHALL reduce the effect of fatigue on the shot. These characteristics SHALL be neutral by default, so that a golfer with no support reproduces prior shot behaviour exactly, and they SHALL never persist into permanent attributes.

#### Scenario: No support is neutral

- **WHEN** a shot is resolved with neutral (zero) staff-support characteristics
- **THEN** the outcome SHALL match the outcome with no staff-support influence

#### Scenario: Strategic support reduces mishits

- **WHEN** shots are resolved with above-zero strategic support
- **THEN** the mishit likelihood SHALL be no greater than with no strategic support

#### Scenario: Mental support softens fatigue

- **WHEN** a fatigued golfer's shot is resolved with above-zero mental support
- **THEN** its dispersion SHALL be no larger than the same fatigued shot with no mental support

