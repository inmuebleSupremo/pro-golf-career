# playing-conditions Specification

## Purpose
TBD - created by archiving change add-weather. Update Purpose after archive.
## Requirements
### Requirement: Playing Conditions Model

The Weather System SHALL generate Playing Conditions that represent the environmental state experienced during play, including at least wind, rain, temperature, humidity, ground firmness, green speed, and visibility. Future versions MAY introduce additional Playing Conditions without changing this requirement.

#### Scenario: Conditions expose the environmental state

- **WHEN** Playing Conditions are generated
- **THEN** they SHALL expose wind, rain, temperature, humidity, ground firmness, green speed, and visibility as their environmental state

### Requirement: Internal Consistency

Playing Conditions SHALL be internally consistent — coupled properties SHALL cohere rather than being assembled arbitrarily — and the simulation SHALL NOT generate conditions solely to increase difficulty. Conditions SHALL remain internally consistent throughout a Tournament, changing only in step with World progression rather than arbitrary gameplay events.

#### Scenario: Coupled properties cohere

- **WHEN** Playing Conditions include heavy rain
- **THEN** the ground SHALL be softer and greens slower (and visibility no better) than in dry conditions, so the state is believable

#### Scenario: Conditions are not fabricated for difficulty

- **WHEN** conditions are generated for a Tournament
- **THEN** they SHALL follow the climate model and World progression, not be produced solely to make scoring harder

### Requirement: Shared Environment

Competitors playing under equivalent Tournament conditions SHALL experience the same Playing Conditions. The simulation SHALL NOT generate player-specific weather, and human- and simulation-controlled golfers SHALL be evaluated under identical conditions.

#### Scenario: All competitors share the conditions

- **WHEN** two golfers play the same round of the same Tournament
- **THEN** they SHALL experience identical Playing Conditions regardless of control type

### Requirement: Course Interaction

Playing Conditions SHALL interact with the Course (for example softer or firmer ground, faster or slower greens, surface moisture, environmental exposure), while the Course SHALL remain the authoritative description of the environment and Playing Conditions SHALL describe its current state.

#### Scenario: Conditions modulate course exposure

- **WHEN** Playing Conditions are applied to a course with a given environmental exposure
- **THEN** the effect of wind on play SHALL scale with that exposure, while the Course itself remains unchanged as the authoritative environment

### Requirement: Conditions Do Not Modify Other Domains

The Playing Conditions domain SHALL NOT directly modify Player Attributes, Rankings, Tournament rules, or Career progression. Dependent systems SHALL consume Playing Conditions without the Weather domain becoming their authoritative source.

#### Scenario: Consuming conditions changes no owned state

- **WHEN** a dependent system reads Playing Conditions
- **THEN** the Weather domain SHALL not have modified any Player Attribute, Ranking, Tournament rule, or Career value

### Requirement: Resolver wind is signed canonical local flow
The weather-to-shot translation SHALL provide a finite signed wind-flow vector in the active hole's canonical local coordinates, along with existing lie-quality effects. Weather generation MAY retain speed, bearing, exposure, and its deterministic per-hole orientation mapping, but the resolver SHALL preserve wind side rather than reduce crosswind to an unsigned scalar before a shot direction is known.

#### Scenario: Equal opposite crosswinds remain distinguishable
- **WHEN** two otherwise identical shot contexts receive equal-magnitude wind flow on opposite sides of the actual shot axis
- **THEN** their resolver inputs SHALL retain opposite signed cross-axis components.

### Requirement: Wind decomposes against actual aim
For a non-putting canonical shot, the resolver SHALL decompose signed local wind flow against that shot's actual origin-to-`AimPoint` frame. It SHALL not treat a per-hole precomputed head/cross decomposition as the final directional-wind answer for an off-axis aim.

#### Scenario: Re-aiming changes wind components
- **WHEN** one fixed local wind vector is resolved for two different valid aim points from the same origin
- **THEN** the resulting along-axis and cross-axis wind components MAY differ according to the two actual aim frames.

### Requirement: Effective canonical wind remains available for observation
The active playable hole SHALL retain its current effective resolver wind flow as a finite read-only vector in
canonical hole coordinates. The vector SHALL describe the direction air moves toward and its magnitude in the
resolver's effective wind units after the existing weather, exposure, and synthetic per-hole orientation conversion.
It SHALL not claim geographic north or replace the documented synthetic orientation model.

#### Scenario: Wind observation preserves signed flow
- **WHEN** active-hole effective wind has a nonzero signed canonical flow
- **THEN** a read projection SHALL retain both signed components and magnitude without converting it to an unsigned
  crosswind or a geographic compass bearing.

