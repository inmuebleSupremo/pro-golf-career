## ADDED Requirements

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
