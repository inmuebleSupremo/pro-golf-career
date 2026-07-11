## ADDED Requirements

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
