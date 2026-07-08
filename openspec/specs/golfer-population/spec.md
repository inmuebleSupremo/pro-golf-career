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

