# world-schedule Specification

## Purpose
TBD - created by archiving change add-world-loop. Update Purpose after archive.
## Requirements
### Requirement: Season Schedule Generation

At the start of every season the World SHALL deterministically generate a competitive calendar: a schedule of tournaments, each allocated to exactly one tour, one course, and one week. The schedule SHALL be generated from the world seed so it is reproducible.

#### Scenario: A season has a generated schedule

- **WHEN** a season begins
- **THEN** the World SHALL produce a schedule of tournaments, each with a tour, a course, and a week

#### Scenario: Schedule is reproducible

- **WHEN** two Worlds with the same seed generate the same season's schedule
- **THEN** the two schedules SHALL be identical

### Requirement: Fixed and Archived Schedule

A season's schedule SHALL be fixed for the duration of that season and SHALL be archived when the season completes, remaining retrievable as history.

#### Scenario: Schedule does not change mid-season

- **WHEN** a season is in progress
- **THEN** its schedule SHALL not change

#### Scenario: Completed schedules are archived

- **WHEN** a season completes
- **THEN** its schedule and results SHALL be archived and remain accessible

### Requirement: Every Scheduled Tournament Belongs to a Tour and Course

Each scheduled tournament SHALL reference exactly one tour (determining its tier and eligible field) and exactly one course (its venue).

#### Scenario: Allocation is complete

- **WHEN** a scheduled tournament is inspected
- **THEN** it SHALL have exactly one tour and exactly one course

