# course-identity Specification

## Purpose
TBD - created by archiving change add-course-domain. Update Purpose after archive.
## Requirements
### Requirement: Course Identity

Every Course SHALL possess a unique, stable identity comprising a unique identifier, a name, a region, an environment classification, and a style. Identity SHALL persist for the life of the Course and SHALL be usable to recognise the Course across seasons.

#### Scenario: Course has a unique stable identity

- **WHEN** a Course is generated
- **THEN** it SHALL have a unique identifier plus a name, region, classification, and style that do not change over the Course's lifetime

#### Scenario: Identity supports long-term recognition

- **WHEN** the same Course reappears in a later season
- **THEN** it SHALL present the same identity, allowing players to recognise it

### Requirement: Environment Classification

Every Course SHALL belong to exactly one persisted environment classification from: Links, Parkland, Desert, Mountain, Coastal, Woodland, Heathland. Classification is data that other domains read to influence weather generation and strategic character; the Course domain SHALL NOT itself generate weather.

New worlds SHALL select pool courses only from Links, Parkland, Desert, Woodland, and Heathland. Their four core course families are Parkland (open Parkland and Woodland variations), Links, Heathland, and Desert. The snapshot-pinned pool policy SHALL determine whether persisted Woodland is a Parkland or Heathland presentation; historical worlds SHALL retain their previous Woodland interpretation. Mountain and Coastal SHALL remain valid for restored worlds.

#### Scenario: Exactly one classification per course

- **WHEN** a Course is generated
- **THEN** it SHALL declare exactly one environment classification from the defined set

#### Scenario: New world selects the core course set

- **WHEN** a new world generates its course pool
- **THEN** every course SHALL be classified as Links, Parkland, Desert, Woodland, or Heathland
- **AND THEN** Woodland SHALL belong to the Parkland family while Heathland SHALL have its own persisted identity

#### Scenario: Historical Woodland interpretation is retained

- **WHEN** a world with a historical course-pool policy is restored
- **THEN** its Woodland courses SHALL still present as Heathland without changing their identity or geometry

#### Scenario: Historical classifications remain valid

- **WHEN** a world created before core-family consolidation is restored
- **THEN** Mountain and Coastal course identities SHALL remain loadable and playable

#### Scenario: Classification is read, not acted upon, by the course

- **WHEN** weather or strategic character is determined for a Course
- **THEN** the consuming domain SHALL read the Course's classification, and the Course domain SHALL NOT generate weather itself

### Requirement: Identity Is Not Tournament State

Course identity SHALL NOT embed tournament-specific information. It SHALL reference historical records rather than containing competitive results.

#### Scenario: No competitive data in identity

- **WHEN** a Course's identity is inspected
- **THEN** it SHALL contain no leaderboards, prize money, rankings, competitors, or round scores

