# course-difficulty Specification

## Purpose
TBD - created by archiving change add-course-domain. Update Purpose after archive.
## Requirements
### Requirement: Analytical Difficulty Profile

Every Course SHALL expose an overall difficulty profile that emerges from its physical characteristics — length, surface layout, hazard placement, green complexity, and environmental exposure. The difficulty profile SHALL be analytical and read-only.

#### Scenario: Difficulty is derived from characteristics

- **WHEN** a Course's difficulty profile is computed
- **THEN** it SHALL be a function of the Course's length, surfaces, hazards, green complexity, and exposure

#### Scenario: Difficulty reflects meaningful differences

- **WHEN** two Courses differ materially in length and hazard density
- **THEN** their difficulty profiles SHALL differ accordingly

### Requirement: Difficulty Does Not Alter Gameplay

The difficulty profile SHALL NOT modify player attributes or directly change shot outcomes. It is descriptive information consumed by other systems.

#### Scenario: Difficulty never mutates attributes

- **WHEN** a Course with any difficulty profile is played
- **THEN** no player's permanent attributes SHALL be changed by the difficulty profile

#### Scenario: Difficulty is an input, not an authority

- **WHEN** another domain uses difficulty (e.g. for presentation or analysis)
- **THEN** it SHALL read the profile without the Course altering shot resolution on its behalf

