# golfer-population Specification

## Purpose
TBD - created by archiving change add-golfer-entities. Update Purpose after archive.
## Requirements
### Requirement: Persistent Population

The world SHALL maintain a persistent population of Professional Golfers. The human-controlled golfer SHALL be one member of this population, and every member SHALL exist independently with its own identity and state.

#### Scenario: Population contains independent members

- **WHEN** the population is generated
- **THEN** it SHALL contain multiple independent Professional Golfers, each with its own Player and identity

#### Scenario: Human golfer is one member

- **WHEN** a human-controlled golfer joins the world
- **THEN** it SHALL be one member of the same population, under the same representation as simulation golfers

### Requirement: Deterministic Diverse Generation

The initial population SHALL be generated deterministically from the world seed hierarchy and SHALL be diverse — members varying in overall skill and in their relative attribute strengths and weaknesses. Regenerating from the same seed SHALL produce the same population.

#### Scenario: Generation is reproducible

- **WHEN** a population is generated twice from the same seed
- **THEN** the two populations SHALL be identical

#### Scenario: Generation is diverse

- **WHEN** a population is generated
- **THEN** its members SHALL vary in overall skill and in their strongest/weakest attributes, rather than being homogeneous

### Requirement: Population Replenishment

As golfers leave the active population (e.g. retirement), the world SHALL introduce new Professional Golfers so the population remains sufficient to sustain competition. Replenishment SHALL operate independently of the human player's career and SHALL preserve continuity.

#### Scenario: Departures are replenished

- **WHEN** members leave the active population
- **THEN** new members SHALL be introduced so the population does not fall below the level needed to sustain competition

#### Scenario: Replenishment is independent of the human player

- **WHEN** replenishment occurs
- **THEN** it SHALL proceed regardless of the human player's participation or career state

### Requirement: Emergent Rivalries

The population SHALL NOT contain scripted rivals. Any rivalry SHALL be an emergent property of repeated competition rather than authored at generation time.

#### Scenario: No scripted rivals at generation

- **WHEN** the population is generated
- **THEN** no member SHALL be pre-assigned as another member's rival

### Requirement: Innate Strategic Disposition

Each generated golfer SHALL carry an innate strategic disposition — their risk appetite — derived deterministically from their attributes, so the population plays a spread of risk/reward styles rather than a uniform strategy. The disposition SHALL be a pure function of the golfer's attributes and SHALL favour a more aggressive style when the golfer's power (driving distance) exceeds their discipline (course management and composure), and a more conservative style in the opposite case. The derivation SHALL be neutral to overall skill, so that it reflects a golfer's relative strengths rather than how strong they are overall. The disposition SHALL flow to the golfer's play through the existing simulation decision seam.

#### Scenario: Disposition varies across the field

- **WHEN** a population is generated
- **THEN** its members SHALL exhibit a spread of strategic dispositions — some aggressive, some conservative, most balanced — rather than all sharing one strategy

#### Scenario: Disposition follows relative strengths

- **WHEN** a golfer's driving distance is high relative to their course management and composure
- **THEN** their disposition SHALL be more aggressive; and when the reverse holds, their disposition SHALL be more conservative

#### Scenario: Disposition is skill-neutral

- **WHEN** two golfers have flat (evenly balanced) attribute profiles at different overall skill levels
- **THEN** both SHALL receive the same (balanced) disposition, because the derivation reflects relative strengths, not overall skill

#### Scenario: Disposition is deterministic

- **WHEN** a population is generated twice from the same seed
- **THEN** each golfer SHALL receive the same disposition both times

