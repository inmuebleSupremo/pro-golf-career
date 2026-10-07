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

Every resolved shot SHALL produce a complete Outcome containing at minimum: final surface, distance remaining to the pin, lateral result, any hazard entered, penalties incurred, resulting shot count, canonical contact position, and resulting playable settlement position. For a penalty, the outcome SHALL distinguish the reported physical contact from the legal recovery or replay position. The final playable lie SHALL equal canonical `surfaceAt` at the playable settlement position. The Outcome SHALL be explainable — the dominant contributing factors (e.g., strong crosswind, poor lie, aggressive strategy, fatigue, excellent execution) SHALL be recoverable from the resolution.

#### Scenario: Outcome includes spatial settlement

- **WHEN** a shot is resolved on a canonical production hole
- **THEN** the produced Outcome SHALL include finite contact and playable-settlement positions, final surface, distance remaining, penalties, and shot count, with no field left undefined

#### Scenario: Penalty contact and recovery are distinct

- **WHEN** a shot enters water or out of bounds
- **THEN** the Outcome SHALL retain the contact position and identify the distinct legal recovery or replay position used for the next shot

#### Scenario: Dominant factors are recoverable

- **WHEN** a resolved shot deviates notably from its expected result
- **THEN** the resolution SHALL expose the dominant contributing factors so the result can be explained rather than appearing arbitrary

### Requirement: Deterministic Sampling via Seed Hierarchy

Every sampled value in resolution SHALL be drawn from a generator derived through the world seed hierarchy at the shot's coordinate. Resolution SHALL contain no ambient randomness.

#### Scenario: Resolution is reproducible

- **WHEN** the same shot coordinate and world state are resolved more than once
- **THEN** the sampled outcome SHALL be identical each time

### Requirement: Shots Consume Equipment Characteristics

A shot SHALL additionally consume the golfer's active equipment characteristics, carried on the golfer's temporary state: forgiveness SHALL reduce dispersion, power SHALL extend reach, **workability SHALL improve ball-flight control in wind (raising effective wind resistance), and feel SHALL improve distance control (tightening distance dispersion)**. These characteristics SHALL be neutral by default, so that standard (baseline) equipment reproduces prior shot behaviour exactly and never persists into permanent attributes.

#### Scenario: Standard equipment is neutral

- **WHEN** a shot is resolved with neutral (baseline) equipment characteristics
- **THEN** the outcome SHALL match the outcome with no equipment influence

#### Scenario: Stronger equipment tightens and extends

- **WHEN** a shot is resolved with above-baseline forgiveness and power
- **THEN** its dispersion SHALL be no larger and its reach no shorter than with neutral equipment

#### Scenario: Workability helps in wind and feel controls distance

- **WHEN** a shot is resolved in wind with above-baseline workability, or with above-baseline feel
- **THEN** the wind's effect SHALL be no greater with workability, and the distance dispersion SHALL be no larger with feel, than with neutral equipment

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

### Requirement: Putting Resolution

A shot played from the green SHALL be resolved by a dedicated putting model rather than the full ball-flight model. The putting model SHALL determine whether the ball is holed from an explicit make probability that increases as distance to the hole decreases and as putting skill increases, so that short putts hole out near-certainly and holing rates fall off realistically with distance. A putt that is not holed SHALL finish a short distance from the hole that converges toward it, so that a hole always holes out in a realistic number of putts without reaching the per-hole shot cap. Because the ball rolls along the green, a putt SHALL be immune to wind and to lie penalties that apply to full ball-flight shots. A putt SHALL still be resolved through the single shared, deterministic model and SHALL produce a complete outcome like any other shot.

#### Scenario: Short putts hole out near-certainly

- **WHEN** a putt is resolved from tap-in range
- **THEN** it SHALL be holed with very high probability, and a hole SHALL NOT accumulate an unrealistic number of putts or reach the per-hole shot cap

#### Scenario: Holing rate falls off with distance and rises with skill

- **WHEN** putts are resolved over a large sample
- **THEN** the fraction holed SHALL decrease as the distance to the hole increases, and a golfer with higher putting attributes SHALL hole a greater fraction than a golfer with lower putting attributes from the same distance

#### Scenario: Putts are immune to wind and lie

- **WHEN** the same putt is resolved under calm conditions and under strong wind (or a poor course lie)
- **THEN** the putt outcome distribution SHALL be unchanged, because a putt is sheltered from wind and played from the putting surface

#### Scenario: A missed putt leaves a converging tap-in

- **WHEN** a putt is not holed
- **THEN** the ball SHALL finish nearer the hole than it started, leaving a distinct short putt, so the hole holes out in a realistic number of strokes

### Requirement: Shots Consume Situational Pressure

A shot SHALL additionally consume a situational **pressure** value in [0,1] carried on the golfer's temporary state. Pressure SHALL worsen the shot — widening dispersion and lowering putt make probability — and its effect SHALL be resisted by the golfer's **Composure** attribute, so a more composed golfer is less affected by the same pressure. Mental support (a sports psychologist) SHALL relieve pressure as well as fatigue. Pressure SHALL be neutral at zero: a shot at zero pressure SHALL be resolved identically regardless of Composure, so calm and opening-round play is unchanged and reproduces prior behaviour exactly.

#### Scenario: Pressure worsens a shot, resisted by composure

- **WHEN** the same golfer's shots are resolved at high pressure versus zero pressure
- **THEN** the high-pressure outcomes SHALL be worse on average, and a golfer with higher Composure SHALL be less affected than one with lower Composure

#### Scenario: A psychologist relieves pressure

- **WHEN** a golfer under pressure is resolved with above-zero mental support
- **THEN** the pressure's effect on the shot SHALL be no greater than the same shot with no mental support

#### Scenario: Zero pressure is neutral

- **WHEN** a shot is resolved at zero pressure
- **THEN** its outcome SHALL NOT depend on the Composure attribute, and SHALL match the outcome the model produced before pressure was activated

### Requirement: Per-Club Dispersion Profiles

Each club SHALL carry its own dispersion profile — an independent lateral (offline) dispersion and distance (long/short) dispersion — rather than a single shared multiplier. The profiles SHALL be calibrated so that a full-swing club's accuracy character is realistic relative to the others: the driver SHALL scatter the widest offline of any club, and the wedge SHALL be the most precise (tightest lateral and distance control). Because lateral and distance dispersion are independent per club, driving accuracy (fairways hit) and greens in regulation SHALL be tunable separately. The profiles SHALL be neutral to determinism — they change only the calibrated spread, not how randomness is sampled.

#### Scenario: A wedge is more precise than a longer club

- **WHEN** a wedge and an iron are resolved over many shots at the same distance
- **THEN** the wedge's lateral and distance spreads SHALL both be tighter than the iron's

#### Scenario: The driver scatters widest off the tee

- **WHEN** a full round is resolved
- **THEN** the driver SHALL have the widest offline dispersion of any club used

#### Scenario: Driving accuracy and greens in regulation are independently calibrated

- **WHEN** the per-club dispersion is tuned
- **THEN** a generated field SHALL produce realistic fairway-hit and green-in-regulation rates, and one SHALL be adjustable without forcing the other

### Requirement: Distance Measured to the Pin

The distance remaining after a shot SHALL be measured to the actual pin — including the pin's lateral offset from the green centre — not to the green centre. A tucked pin therefore plays harder (a centre-aimed shot leaves a longer approach to it), and finishing near the pin, not merely on the green, is what leaves a short putt. A centre pin (zero lateral offset) SHALL reproduce prior behaviour exactly.

#### Scenario: A tucked pin is farther from a centre-aimed shot

- **WHEN** the same centre-aimed shots are resolved to a tucked pin versus a centre pin
- **THEN** the distance remaining SHALL be greater to the tucked pin

#### Scenario: A centre pin is neutral

- **WHEN** a shot is resolved to a pin with zero lateral offset
- **THEN** the distance remaining SHALL match the pre-pin behaviour (measured to the green centre)

### Requirement: Situational Shot Decisions

The simulation decision policy SHALL choose each shot from the situation — the remaining distance, the ball's lie, the pin placement, the golfer's attributes, and the hole's par — not from a fixed per-golfer strategy alone. From a difficult lie (deep rough, bunker, recovery, trees) the golfer SHALL play conservatively to recover, regardless of disposition; from a clean lie it SHALL keep its disposition. On a scoring approach the golfer SHALL aim a fraction of the way toward the pin determined by its disposition and by shot confidence (the fraction fading to zero as the shot lengthens). On a long approach to a **par 5** (never a tee shot, and never a par 4/3, which have no stroke to spare) the golfer SHALL decide whether to go for the green or lay up: an aggressive disposition, or any golfer who can reach the green comfortably given their own reach, SHALL go for it; otherwise the golfer SHALL lay up to leave a comfortable full-wedge distance, aimed at the green centre. The policy SHALL remain pure and deterministic.

#### Scenario: A difficult lie forces conservative recovery

- **WHEN** the policy decides a shot from a bunker or deep rough
- **THEN** it SHALL play conservatively regardless of the golfer's disposition

#### Scenario: Aggression attacks the pin, conservatism plays the centre

- **WHEN** an aggressive and a conservative golfer decide the same confident approach to a tucked pin
- **THEN** the aggressive golfer SHALL aim toward the pin and the conservative golfer SHALL aim at the green centre

#### Scenario: Pin attack fades with distance

- **WHEN** the same aggressive golfer decides a short approach versus a long approach to a tucked pin
- **THEN** it SHALL aim more toward the pin on the short approach, and play the centre on a long approach

#### Scenario: Lay up or go for it on a par 5

- **WHEN** a golfer faces a long approach to a par-5 green that is not comfortably within their reach
- **THEN** an aggressive golfer SHALL go for the green while a conservative golfer SHALL lay up to a full-wedge distance; on a par 4 or 3, or from the tee, the golfer SHALL always go for the green

#### Scenario: A long hitter goes for a green a short hitter lays up

- **WHEN** a long-hitting and a short-hitting golfer of the same conservative disposition face the same par-5 approach
- **THEN** the long hitter (who can reach comfortably) SHALL go for the green while the short hitter SHALL lay up

### Requirement: Penalty Hazard Recovery

When a resolved shot finishes in a penalty hazard, the round loop SHALL apply a recovery rule determined by the **kind** of hazard, adding exactly one penalty stroke in every case. The two hazard kinds SHALL recover differently:

- **Out of bounds** (and equivalently a lost ball) SHALL use **stroke-and-distance**: the next shot replays from the same spot the hazard shot was played from, losing the distance that shot gained.
- **Water** SHALL use a **water-drop**: the ball is dropped near where it entered the hazard and played forward, so the next shot's distance to the pin is the hazard shot's own distance-remaining plus a small fixed setback representing near-edge and lateral relief, and it is played from a rough lie. The drop SHALL advance the ball toward the hole relative to the previous spot (a water carry costs a stroke and some distance, not a full shot).

This rule SHALL be applied identically by every entry point that resolves a sequence of shots (`resolveRound` and the interactive playable round/hole), so distribution and fidelity equivalence across entry points is preserved.

#### Scenario: Out of bounds replays from the previous spot

- **WHEN** a shot finishes out of bounds
- **THEN** one penalty stroke SHALL be added and the next shot SHALL be played from the same spot as the shot that went out of bounds (stroke-and-distance)

#### Scenario: Water is dropped near the hazard and played forward

- **WHEN** a shot finishes in water
- **THEN** one penalty stroke SHALL be added and the next shot SHALL be played from a dropped position nearer the hole than the previous spot — its distance to the pin equal to the water shot's distance-remaining plus a small fixed setback — from a rough lie

#### Scenario: Same recovery in every resolution entry point

- **WHEN** the same hazard outcome occurs under `resolveRound` versus an interactive playable round or hole for the same inputs and seed
- **THEN** the recovery rule applied SHALL be identical, so a fully-simmed round remains identical to the automatic round resolution

### Requirement: Injury Impairment

Shot resolution SHALL accept an injury-impairment condition input in [0,1] (0 = uninjured, neutral) alongside the existing condition inputs (fatigue, pressure). A positive impairment SHALL degrade the outcome — widening dispersion, shortening carry, and lowering putt make-rate — with the degradation growing as the impairment rises. Because the impairment is physical, staff mental support SHALL NOT relieve it (unlike fatigue and pressure). An impairment of zero SHALL reproduce prior shot behaviour exactly, so uninjured play is unchanged.

#### Scenario: Impairment degrades the shot

- **WHEN** the same shot is resolved at a higher injury impairment versus zero, all else equal
- **THEN** the higher-impairment outcome SHALL be worse on average — wider dispersion and reduced expected proximity/putting

#### Scenario: Zero impairment is neutral

- **WHEN** a shot is resolved at zero injury impairment
- **THEN** its outcome SHALL match the outcome the model produced before the impairment input existed

#### Scenario: Mental support does not relieve impairment

- **WHEN** two otherwise-identical shots are resolved at the same positive impairment, one with staff mental support and one without
- **THEN** the injury impairment's effect SHALL be identical in both (mental support relieves fatigue and pressure, not physical injury)

### Requirement: Canonical ball-strike intent
The simulation SHALL accept immutable `ShotIntent` values. Its first supported variant, `BallStrikeIntent`, SHALL contain a stable individual `ClubId` and an `AimPoint`.

#### Scenario: Intent contains no human risk policy
- **WHEN** a human submits a ball-strike intent
- **THEN** the authoritative input contains the selected club and literal aim point
- **AND THEN** it contains no strategy, target-distance, or target-lateral field.

### Requirement: AimPoint means intended first contact
`AimPoint(x,y)` SHALL be finite absolute canonical-hole yard coordinates for intended carry/first contact from the current ball state. It SHALL NOT mean final resting position.

#### Scenario: Compatibility resolution derives direction
- **WHEN** a valid intent is resolved
- **THEN** direction and requested travel are derived from current ball position to its aim point
- **AND THEN** existing reach caps, dispersion, conditions, and surface settlement determine the outcome.

### Requirement: Individual club catalogue compatibility
The simulation SHALL provide a static stable catalogue of individual clubs, including driver, fairway woods, hybrids, numbered irons, wedges, and putter. Every club SHALL map to an existing equipment family so current aggregate bag/loadout effects remain applicable.

#### Scenario: Club ID survives compatibility use
- **WHEN** a valid catalogue club is selected
- **THEN** the resolver uses its calibrated characteristics plus applicable family equipment effects
- **AND THEN** no new equipment ownership or progression system is required.

### Requirement: Bounded but expressive aim validation
The simulation SHALL reject non-finite points and points outside a server-derived finite aim envelope based on hole boundary plus documented margin. It SHALL permit points within that envelope even when they lie in hazards, trees, out of bounds, or beyond selected-club reach.

#### Scenario: Unreachable hazardous target remains a golf decision
- **WHEN** a player aims a short club at a reachable-coordinate point in water or beyond its reach
- **THEN** the intent is valid
- **AND THEN** existing execution and settlement rules determine the result.

### Requirement: Simplified resolver remains authoritative
This change SHALL preserve the existing simplified resolver beneath intent adaptation. It SHALL NOT add shot types, bounce/roll, flight-terrain intersection, forced carries, directional wind, shape, trajectory control, or a flight trace.

#### Scenario: Intent does not create unimplemented physics
- **WHEN** the resolver processes a ball-strike intent
- **THEN** it produces the same class of existing settlement/outcome data
- **AND THEN** it does not claim an unimplemented path or final-resting target guarantee.

