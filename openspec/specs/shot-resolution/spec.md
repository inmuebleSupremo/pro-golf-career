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
The simulation SHALL accept immutable `ShotIntent` values. Its supported non-putting variant, `BallStrikeIntent`, SHALL contain a stable individual `ClubId`, an `AimPoint`, one non-null `ShotFamily`, and one non-null `ShotShape`; its putting variant SHALL be minimal `PuttIntent` and SHALL preserve existing non-spatial putting behavior. Human intent SHALL contain no strategy, target-distance, target-lateral, carry, rollout, launch, height, wind, or random-execution control.

#### Scenario: Intent states technique and shape but not execution knobs
- **WHEN** a human submits a ball-strike intent
- **THEN** the authoritative input contains selected club, literal intended landing point, intended technique, and shape
- **AND THEN** the resolver derives reach, dispersion, wind response, airborne path, and any permitted ground response.

### Requirement: AimPoint means intended first contact
`AimPoint(x,y)` SHALL be finite absolute canonical-hole yard coordinates for intended carry/first contact from the current ball state for STRAIGHT, FADE, and DRAW. It SHALL NOT mean initial launch line, final resting position, or a player-selected curvature endpoint.

#### Scenario: Compatibility resolution derives direction
- **WHEN** a valid intent is resolved
- **THEN** direction and requested travel are derived from current ball position to its aim point
- **AND THEN** existing reach caps, execution dispersion, directional wind, shape profile, and surface settlement determine actual contact.

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
This change SHALL preserve one deterministic simplified resolver beneath intent adaptation. It MAY add directional wind, eligible shot shape, resolver-derived height, and one bounded authoritative parametric airborne path whose exact endpoint becomes first contact. It MAY retain the explicitly bounded authoritative post-contact ground response needed for supported shot families. It SHALL NOT add flight-terrain or obstacle intersection, forced carries, bounce, spin, launch-velocity or full aerodynamic physics, player-controlled trajectory height, slope, firmness, arbitrary terrain traversal, or richer ground response.

#### Scenario: Flight remains bounded rather than full physics
- **WHEN** the resolver processes a supported family and shape intent
- **THEN** it produces calculated airborne contact/path facts, bounded permitted ground response, and settlement data
- **AND THEN** it does not calculate terrain collision, forced carry, bounce, spin, slope, terrain traversal, or a player-controlled flight parameter.

### Requirement: Truthful semantic shot trace
For a trace-materialized canonical shot, the simulation SHALL produce an immutable `ShotTrace` containing executing `ClubId`, pre-shot origin, exact intended `AimPoint`, optional authoritative airborne path, actual canonical `ShotContact`, optional authoritative `ShotTraceRoll`, optional recovery/replay transition, and final legal point. An airborne path SHALL contain only samples evaluated by the resolver's `FlightSolution`; the trace SHALL contain only values the resolver and settlement actually calculate.

#### Scenario: Normal contact has a truthful flight phase
- **WHEN** a canonical non-putting shot contacts a playable ordinary surface and has no calculated ground response
- **THEN** trace origin SHALL equal pre-shot `BallState.position`
- **AND THEN** any returned airborne path SHALL start at origin and end at resolved contact
- **AND THEN** roll and recovery transition SHALL be absent and final point SHALL equal settlement ball position.

### Requirement: Trace derives legal state from settlement
The resolver SHALL calculate contact and any permitted bounded ground-response endpoint before settlement. `ShotSettlement` SHALL remain the authority for recovery and final playable `BallState`; trace SHALL derive final point and any recovery/replay transition from that result and SHALL NOT reimplement penalty, drop, or replay logic. Contact surface SHALL remain independently observable.

#### Scenario: Recovery preempts rollout
- **WHEN** contact is WATER or OUT_OF_BOUNDS
- **THEN** settlement SHALL apply the applicable legal recovery/replay rule from the contact
- **AND THEN** trace SHALL contain no roll phase and final point SHALL equal settlement ball position.

### Requirement: Trace materialization is observational
The resolver SHALL support trace materialization without changing sampling, geometry lookup, settlement, score, deterministic random consumption, or `FlightSolution` contact. Summary-only resolution SHALL remain available for non-observable consumers and SHALL not require airborne sample allocation.

#### Scenario: Trace mode preserves resolved result
- **WHEN** identical canonical context and seed are resolved once with trace materialization and once summary-only
- **THEN** carry, lateral, contact, settlement, penalties, score, final ball state, and random consumption SHALL be equal
- **AND THEN** only trace presence and its resolver-derived samples may differ.

### Requirement: No speculative trajectory physics
The resolver and trace MAY represent deterministic shape curvature, derived height, and aim-relative directional-wind response only through an authoritative `FlightSolution` and its derived samples. They SHALL NOT claim or calculate launch velocity, spin, bounce, slope, firmness, terrain crossing effects, time-of-flight, forced carry, tree collision, obstacle collision, player-controlled trajectory, or full aerodynamic physics.

#### Scenario: Airborne path does not become collision physics
- **WHEN** a resolver emits an authoritative airborne path
- **THEN** it SHALL expose only resolver-derived position/progress/height facts needed for contact and playback
- **AND THEN** it SHALL not use sample points as terrain-intersection authority or expose uncomputed physical events.

### Requirement: Intentional surface-aware shot family
Each non-putting `BallStrikeIntent` SHALL contain one non-null `ShotFamily`: `FULL`, `CONTROLLED`, `PITCH`, `CHIP`, or `BUNKER`. The family SHALL express intended technique only; sampled carry, rollout quantity, launch, spin, and randomness remain resolver-derived. A minimal separate `PuttIntent` SHALL retain existing putting behavior without spatial aim, line, speed, break, or family control.

#### Scenario: Aim remains intended first contact
- **WHEN** a player submits any supported ball-strike family
- **THEN** its `AimPoint` SHALL remain the intended canonical first-contact point
- **AND THEN** it SHALL NOT be interpreted as the desired final resting point.

### Requirement: Authoritative lie and club eligibility
The simulation SHALL evaluate family eligibility from actual starting surface, selected individual club, and `ShotFamily` before resolving a stroke. Green shall use the putting path; BUNKER family shall require bunker lie and compatible wedges; invalid combinations SHALL be rejected rather than silently substituted.

#### Scenario: Poor choice remains distinct from invalid technique
- **WHEN** a compatible family/club is played from deep rough or recovery area
- **THEN** the resolver MAY apply its lie execution profile
- **AND THEN** it SHALL not reject the stroke merely because another choice would be safer.

### Requirement: Compact club-family compatibility
FULL and CONTROLLED SHALL support driver, woods, hybrids, irons, and wedges; PITCH and BUNKER SHALL support pitching, gap, and sand wedges; CHIP SHALL support irons and wedges. Individual `ClubId` / `ClubSpec` values SHALL retain their existing calibrated identity within these compact family groups.

#### Scenario: Bunker does not use ordinary full-shot authority
- **WHEN** a ball starts in a bunker
- **THEN** the first-slice resolver SHALL require BUNKER family with a compatible wedge
- **AND THEN** ordinary FULL behavior SHALL not be the primary bunker interaction.

### Requirement: Derived surface-aware execution profile
The resolver SHALL derive internal execution from starting surface, `ClubSpec`, `ShotFamily`, existing player attributes, and `Environment`. Weather conditions and surface-lie effects SHALL remain separate inputs.

#### Scenario: Controlled exchanges reach for control
- **WHEN** equivalent FULL and CONTROLLED strokes are resolved with the same club and context
- **THEN** CONTROLLED SHALL have lower effective reach and calibrated lower variance
- **AND THEN** it SHALL not be universally superior to FULL.

### Requirement: Each selectable family has a distinct first-slice mechanic
`FULL` SHALL retain the ordinary-swing baseline subject only to documented lie constraints and penalties. `PITCH` SHALL use WEDGES, short bounded carry, and limited release; `CHIP` SHALL use iron-or-wedge compatibility, a short intended landing point, and meaningfully larger release than PITCH; `BUNKER` SHALL use WEDGES, short bounded carry, a wider execution distribution than comparable PITCH, and zero or negligible release. No selectable `ShotFamily` SHALL be a nominal alias of another in its intended compatible context.

#### Scenario: Pitch and chip are not renamed full shots
- **WHEN** compatible PITCH and CHIP intents are resolved with equivalent short-game context
- **THEN** their profiles SHALL use the submitted `AimPoint` as intended first contact
- **AND THEN** their bounded carry/release behavior SHALL be mechanically distinguishable from FULL and from each other.

#### Scenario: Bunker is distinct from pitch
- **WHEN** a compatible BUNKER and comparable PITCH profile are evaluated
- **THEN** BUNKER SHALL have the documented wider execution distribution and zero or negligible release
- **AND THEN** it SHALL not reuse ordinary FULL-shot behavior.

### Requirement: Bounded authoritative ground response
After a playable canonical contact, the resolver MAY calculate one bounded forward ground-response endpoint according to family, club, and contact surface. Water/OB contacts SHALL receive no roll; BUNKER response SHALL be zero or negligible in this change.

#### Scenario: Chip has a real release distinction
- **WHEN** compatible PITCH and CHIP strokes land under equivalent intended short-game conditions
- **THEN** both SHALL preserve their submitted landing aim semantics
- **AND THEN** CHIP SHALL have a measurably greater authoritative bounded release than PITCH in its intended context.

#### Scenario: Unsupported boundary prevents rollout
- **WHEN** a calculated ground-response candidate does not remain on the supported contact surface under canonical geometry classification
- **THEN** the deterministic conservative result SHALL settle at contact
- **AND THEN** the resolver SHALL not begin chained terrain traversal.

### Requirement: Contact and final surface remain distinct
Canonical contact surface SHALL describe first landing; final surface SHALL describe the settled playable ball after any authoritative response or legal recovery. Existing scoring/statistics consumers SHALL use the documented fact appropriate to their metric.

#### Scenario: Roll changes final lie without rewriting contact
- **WHEN** a playable shot releases from its contact to a different final canonical surface
- **THEN** contact surface SHALL remain recorded at contact
- **AND THEN** settlement ball lie and final-surface semantics SHALL describe the final settled state.

### Requirement: Ground response excludes rich physics
The first-slice response SHALL not calculate or claim bounce, spin, slope, firmness, speed, path collision, arbitrary terrain traversal, or flight-terrain interaction.

#### Scenario: Ground endpoint is not a trajectory model
- **WHEN** a family produces post-contact movement
- **THEN** the resolver SHALL expose only its calculated bounded endpoint
- **AND THEN** it SHALL not expose invented physical samples or events.

### Requirement: Intentional shot shape
Each non-putting `BallStrikeIntent` SHALL contain non-null `ShotShape` of `STRAIGHT`, `FADE`, or `DRAW`, in addition to club, `AimPoint`, and `ShotFamily`. Shape SHALL express deliberate airborne curvature; it SHALL NOT be a player-selected carry, lateral-offset, launch, height, spin, wind, or randomness control. Compatibility adapters MAY default omitted legacy/API shape to `STRAIGHT`; the engine-level intent SHALL be complete.

#### Scenario: Shape does not replace landing intent
- **WHEN** a player submits a legal shaped ball strike
- **THEN** its `AimPoint` SHALL remain the intended canonical first-contact target
- **AND THEN** the player SHALL not submit a separate launch-line or curvature-magnitude field.

#### Scenario: Ideal shaped flight returns to its intended landing target
- **WHEN** ideal zero-error execution resolves under calm wind for otherwise equivalent STRAIGHT, FADE, and DRAW intents with the same submitted `AimPoint` that is within the resolver-derived effective reach
- **THEN** each `FlightSolution.positionAt(1)` SHALL equal that submitted `AimPoint`
- **AND THEN** FADE and DRAW SHALL have distinguishable correctly handedness-directed intermediate curvature.

#### Scenario: Observable handedness determines bulge and return direction
- **WHEN** zero-error, zero-wind RIGHT-handed and LEFT-handed intents resolve for an arbitrary non-degenerate
  origin-to-`AimPoint` axis
- **THEN** a RIGHT-handed DRAW SHALL have an intermediate position golfer-right of that axis before returning
  golfer-left toward `AimPoint`, and a RIGHT-handed FADE SHALL have an intermediate position golfer-left before
  returning golfer-right
- **AND THEN** LEFT-handed DRAW and FADE sides SHALL be the exact inverse
- **AND THEN** STRAIGHT SHALL have no deliberate intermediate shape bulge
- **AND THEN** each shape's endpoint at progress `1` SHALL equal `AimPoint`.

#### Scenario: Intent is not a landing guarantee
- **WHEN** ordinary execution error or nonzero directional wind is present
- **THEN** the authoritative first-contact position MAY differ from `AimPoint`
- **AND THEN** the resolver SHALL retain that actual `FlightSolution.positionAt(1)` as contact.

### Requirement: Authoritative parametric flight solution
The resolver SHALL create one pure deterministic parametric `FlightSolution` for each canonical non-putting ball strike. The solution SHALL be the sole authority for both authoritative first-contact position and any sampled airborne playback path. It SHALL evaluate finite canonical horizontal positions and non-negative derived heights over normalized progress from origin to contact.

#### Scenario: Flight solution owns contact
- **WHEN** a canonical ball strike is resolved
- **THEN** the resolver SHALL classify `FlightSolution.positionAt(1)` as the actual first-contact position
- **AND THEN** it SHALL not calculate a second independent contact projection.

### Requirement: Derived height has no player control
Flight height SHALL be derived deterministically from resolver-owned family, club, resolved travel, and bounded flight profile inputs. A player SHALL not select launch height, apex, spin, or trajectory parameters. Putt resolution SHALL remain outside airborne-flight behavior.

#### Scenario: Putt remains non-airborne
- **WHEN** the dedicated putting path resolves a stroke
- **THEN** wind and airborne-flight parameters SHALL not change its result
- **AND THEN** it SHALL not create an airborne path.

### Requirement: Shape eligibility and bounded execution tradeoff
FULL and CONTROLLED ball strikes SHALL permit STRAIGHT, FADE, and DRAW when otherwise legal. PITCH, CHIP, and BUNKER SHALL permit STRAIGHT only in this feature. A shaped eligible strike SHALL have a calibrated, bounded execution-control tradeoff using existing club control, player attributes, dispersion, and equipment workability inputs; it SHALL not introduce a new progression statistic, a fixed universal carry penalty, or a universally dominant shape.

#### Scenario: Ineligible short-game shape is rejected
- **WHEN** a player submits FADE or DRAW for a PITCH, CHIP, or BUNKER family
- **THEN** authoritative eligibility validation SHALL reject the stroke without changing ball, stroke count, or round state.

#### Scenario: Shape is not cosmetic or free
- **WHEN** equivalent legal STRAIGHT and shaped strikes resolve under fixed calm contexts
- **THEN** the shaped strike SHALL have a distinct authoritative airborne curve
- **AND THEN** calibrated execution-control behavior SHALL remain bounded and explainable through existing execution systems.

### Requirement: Separate wind and execution channels
The resolver SHALL keep ordinary execution dispersion, deterministic signed wind displacement, and wind-related uncertainty as separate named/calibrated channels. Signed wind displacement SHALL affect actual flight/contact through the aim-relative wind components. Wind-related uncertainty MAY widen outcome uncertainty but SHALL not duplicate deterministic displacement or legacy unsigned-crosswind logic. A calm wind vector and STRAIGHT shape SHALL preserve established baseline behavior.

#### Scenario: Crosswind has direction and uncertainty without double counting
- **WHEN** a legal ball strike resolves in nonzero crosswind
- **THEN** the resolver SHALL calculate signed cross-axis displacement and separately apply only its calibrated wind uncertainty
- **AND THEN** it SHALL not apply an obsolete second unsigned-crosswind penalty to the same physical effect.

#### Scenario: Calm straight compatibility
- **WHEN** a pre-feature-compatible canonical context resolves with calm wind and STRAIGHT shape
- **THEN** fixed-seed carry, lateral displacement, authoritative first contact, settlement/final ball state, score, and deterministic random consumption SHALL match the approved baseline result.
- **AND THEN** corpus-level calm STRAIGHT carry, distance-dispersion, lateral-dispersion, and scoring distributions SHALL remain within the approved baseline calibration bounds.

### Requirement: Contact and settlement remain authoritative boundaries
After deriving contact from `FlightSolution`, the resolver SHALL retain existing canonical surface classification, bounded post-contact roll, and `ShotSettlement` authority. Water/out-of-bounds recovery/replay remains a rules transition after contact. Airborne terrain along the path SHALL not alter a result in this feature.

#### Scenario: Path crossing has no premature collision effect
- **WHEN** an authoritative airborne curve geometrically passes over or through terrain before its computed first contact
- **THEN** only the computed first-contact endpoint SHALL be classified for this feature
- **AND THEN** no flight collision, forced carry, or intermediate recovery SHALL be invented.

### Requirement: Existing family rollout boundaries are preserved
This change SHALL preserve the existing bounded ground-response configuration: FULL, CONTROLLED, and BUNKER
families SHALL have no configured post-contact roll, while PITCH and CHIP MAY retain only their existing bounded
authoritative roll. It SHALL not introduce new rollout physics or extend roll across arbitrary terrain.

#### Scenario: Family rollout remains authoritative and bounded
- **WHEN** an observable FULL, CONTROLLED, or BUNKER strike settles without a recovery transition
- **THEN** its trace SHALL not contain authoritative roll and its final point SHALL equal first contact.
- **AND WHEN** an observable PITCH or CHIP strike has existing calculated roll
- **THEN** its trace SHALL expose only the bounded authoritative roll endpoints returned by the resolver.

### Requirement: Deterministic condition-sensitive ground response
After an eligible canonical non-putting shot first contacts a playable surface, the resolver SHALL calculate a finite deterministic desired release from the authoritative flight/contact facts, shot family, club/category, contact surface, lie, and ground firmness. FULL, CONTROLLED, PITCH, and CHIP SHALL be eligible; BUNKER and putts SHALL remain outside this response.

The response SHALL preserve meaningful ordering: equivalent eligible firm conditions SHALL not release less than soft conditions, and the response model SHALL retain distinct calibrated behaviour for low-lofted long shots, higher-lofted approaches, PITCH, and CHIP. The requirement does not prescribe uncalibrated yard constants.

#### Scenario: Firm fairway driver releases more than soft fairway driver
- **WHEN** identical deterministic driver strikes have equal airborne first contact on a playable fairway and differ only between firm and soft ground
- **THEN** the firm result SHALL finish no nearer its contact point than the soft result
- **AND THEN** both final positions and surfaces SHALL be resolver-authored and reproducible.

#### Scenario: Higher-lofted approach remains more restrained than long shot
- **WHEN** equivalent eligible firm-condition long-shot and higher-lofted approach fixtures contact comparable playable surfaces
- **THEN** the calibrated higher-lofted approach SHALL not receive the long shot's release entitlement
- **AND THEN** both outcomes SHALL retain their authoritative first-contact facts.

#### Scenario: Pitch and chip remain distinct
- **WHEN** eligible PITCH and CHIP fixtures resolve on the same playable surface and firmness
- **THEN** each SHALL use its own bounded response profile
- **AND THEN** neither SHALL silently become a FULL, CONTROLLED, BUNKER, or putt response.

### Requirement: Authoritative first contact precedes ground response
The resolver SHALL derive non-putting first contact exclusively from `FlightSolution.positionAt(1)` before applying ground response. Display samples, client geometry, and a separate contact calculation SHALL NOT influence release, settlement, scoring, or final ball state.

#### Scenario: Trace materialization does not alter ground response
- **WHEN** an identical canonical context and seed resolve summary-only and trace-materialized
- **THEN** their first contact, ground response, settlement, score, and final ball state SHALL be equal
- **AND THEN** only observable trace allocation may differ.

### Requirement: Bounded ordinary-surface boundary response
For the approved first-milestone policy, release SHALL follow one deterministic straight ground segment from first contact. It MAY cross at most one boundary between ordinary playable canonical surfaces. If the desired segment would cross a second boundary, WATER, or OUT_OF_BOUNDS, the resolver SHALL clamp the final point to the last deterministic valid ordinary playable point before that boundary. It SHALL NOT create a penalty, drop, replay, or general multi-surface traversal from release.

#### Scenario: One ordinary boundary transition is retained
- **WHEN** an eligible desired release crosses exactly one boundary from a playable ordinary surface to another playable ordinary surface and reaches no further boundary
- **THEN** the final ball SHALL settle at the deterministic released endpoint on its canonically classified final surface
- **AND THEN** the outcome SHALL retain distinct contact and final facts.

#### Scenario: Non-playable boundary is not treated as rollout hazard traversal
- **WHEN** an eligible desired release would reach WATER or OUT_OF_BOUNDS after first playable contact
- **THEN** the resolver SHALL clamp before that non-playable boundary
- **AND THEN** it SHALL not apply a release-created penalty, recovery, or replay.

#### Scenario: Second boundary caps first-milestone response
- **WHEN** an eligible desired release would cross more than one canonical surface boundary
- **THEN** the resolver SHALL clamp before the second boundary
- **AND THEN** it SHALL not continue with arbitrary surface traversal.

### Requirement: Ground response preserves deterministic compatibility
Ground response SHALL consume no uncontrolled randomness and SHALL preserve deterministic execution for identical context, seed, conditions, policy, and intended shot. Existing completed historical results SHALL not be re-resolved; loaded careers SHALL follow the approved future-stroke compatibility policy.

#### Scenario: Identical inputs repeat exactly
- **WHEN** an eligible canonical shot resolves twice with identical deterministic inputs
- **THEN** its contact, release endpoint, final ball, scoring, and settlement SHALL be equal
- **AND THEN** the response SHALL not consume an additional random draw.

