## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Canonical ball-strike intent
The simulation SHALL accept immutable `ShotIntent` values. Its supported non-putting variant, `BallStrikeIntent`, SHALL contain a stable individual `ClubId`, an `AimPoint`, and one non-null `ShotFamily`; its putting variant SHALL be minimal `PuttIntent` and SHALL preserve existing non-spatial putting behavior. Human intent SHALL contain no strategy, target-distance, target-lateral, carry, rollout, launch, or random-execution control.

#### Scenario: Intent states technique but not execution knobs
- **WHEN** a human submits a ball-strike intent
- **THEN** the authoritative input contains selected club, literal intended landing point, and intended technique
- **AND THEN** the resolver derives reach, dispersion, and any permitted ground response.

### Requirement: Simplified resolver remains authoritative
This change SHALL preserve one deterministic simplified resolver beneath intent adaptation. It MAY add the explicitly bounded, authoritative post-contact ground response needed for supported shot families, but SHALL NOT add flight-terrain intersection, arbitrary terrain traversal, forced carries, directional wind, shape, trajectory control, or full physical flight simulation.

#### Scenario: Family does not create unimplemented physics
- **WHEN** the resolver processes a supported family intent
- **THEN** it produces calculated contact, bounded permitted ground response, and settlement data
- **AND THEN** it does not claim an unimplemented flight path, apex, spin, bounce, or final-target guarantee.

### Requirement: Truthful semantic shot trace
For a trace-materialized canonical shot, the simulation SHALL produce an immutable `ShotTrace` containing executing `ClubId`, pre-shot origin, exact intended `AimPoint`, actual canonical `ShotContact`, optional authoritative `ShotTraceRoll`, optional recovery/replay transition, and final legal point. The trace SHALL contain only values the current resolver and settlement actually calculate.

#### Scenario: Normal contact has no invented phase
- **WHEN** a canonical shot contacts a playable ordinary surface and has no calculated ground response
- **THEN** trace origin SHALL equal pre-shot `BallState.position`
- **AND THEN** trace contact SHALL equal resolved contact
- **AND THEN** roll and recovery transition SHALL be absent and final point SHALL equal settlement ball position.

### Requirement: Trace derives legal state from settlement
The resolver SHALL calculate contact and any permitted bounded ground-response endpoint before settlement. `ShotSettlement` SHALL remain the authority for recovery and final playable `BallState`; trace SHALL derive final point and any recovery/replay transition from that result and SHALL NOT reimplement penalty, drop, or replay logic. Contact surface SHALL remain independently observable.

#### Scenario: Recovery preempts rollout
- **WHEN** contact is WATER or OUT_OF_BOUNDS
- **THEN** settlement SHALL apply the applicable legal recovery/replay rule from the contact
- **AND THEN** trace SHALL contain no roll phase and final point SHALL equal settlement ball position.

### Requirement: No speculative trajectory physics
The trace and resolver SHALL NOT model or claim apex, curve, launch velocity, spin, bounce, slope, firmness, terrain crossing, time-of-flight, directional wind response, forced carry, tree collision, shot shape, or trajectory control. It MAY expose only the bounded calculated post-contact roll endpoint defined by this capability.

#### Scenario: Bounded roll does not become a trajectory model
- **WHEN** a resolver emits a trace with a roll phase
- **THEN** it SHALL expose the authoritative roll endpoints only
- **AND THEN** it SHALL not expose uncomputed flight or detailed physical events.
