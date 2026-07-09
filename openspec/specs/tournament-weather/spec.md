# tournament-weather Specification

## Purpose
TBD - created by archiving change add-weather. Update Purpose after archive.
## Requirements
### Requirement: Tournament Playing Conditions

Every Tournament SHALL be played under defined Playing Conditions, determined before play. Playing Conditions MAY evolve throughout the Tournament, and any changes SHALL occur consistently for all competitors according to Tournament scheduling.

#### Scenario: Conditions are determined before play

- **WHEN** a Tournament begins
- **THEN** its Playing Conditions SHALL already be defined for each round

#### Scenario: Conditions evolve consistently by round

- **WHEN** Playing Conditions differ between two rounds of a Tournament
- **THEN** the change SHALL apply to every competitor of a given round identically, following Tournament scheduling rather than per-golfer variation

### Requirement: Strategic Adaptation

Playing Conditions SHOULD encourage strategic adaptation: the conditions SHALL be made available to play so that decisions and outcomes can respond to them (for example club selection, shot shape, or risk tolerance). The specification does not prescribe how adaptation is implemented.

#### Scenario: Conditions influence play

- **WHEN** a round is resolved under adverse Playing Conditions (such as strong wind)
- **THEN** the conditions SHALL measurably affect resolution, so that adapting to them matters

### Requirement: Forecast Availability

The simulation MAY provide a Forecast of expected Playing Conditions prior to Tournament play to support strategic preparation. A Forecast SHALL represent a prediction rather than a guarantee. Future gameplay systems MAY expand forecasting without changing this requirement.

#### Scenario: Forecast approximates but does not guarantee conditions

- **WHEN** a Forecast is provided before a Tournament
- **THEN** it SHALL approximate the expected conditions without being guaranteed to equal the conditions actually experienced

### Requirement: Environmental History

The World MAY preserve historically significant Playing Conditions (for example severe weather events or exceptionally difficult, record-setting scoring conditions) as environmental history. When such conditions occur, the World SHALL be able to record their environmental context so that history contributes to World continuity.

#### Scenario: Significant conditions are preserved

- **WHEN** a Tournament is played under exceptionally severe conditions
- **THEN** the World MAY record that environmental context in its history for later reference

