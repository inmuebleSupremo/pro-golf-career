# competitive-statistics Specification

## Purpose
TBD - created by archiving change add-statistics. Update Purpose after archive.
## Requirements
### Requirement: Competitive Statistics

The World SHALL maintain competitive statistics for Professional Golfers describing measurable aspects of competitive performance (for example events played, cuts made, wins, top-10 finishes, and scoring relative to par), **including shot-level performance — driving accuracy (fairways hit), greens in regulation, and putts per round — derived from the shots actually played**. The specification defines the existence of statistics; it does not prescribe individual calculations.

#### Scenario: Statistics describe competitive performance

- **WHEN** a golfer competes in events
- **THEN** the World SHALL maintain competitive statistics for them derived from those results

#### Scenario: Shot-level statistics are accumulated

- **WHEN** a golfer plays rounds
- **THEN** their driving accuracy, greens in regulation, and putts SHALL be accumulated from the shots played, and be available as season and career rates

### Requirement: Seasonal Statistics

The World SHALL preserve statistics for every competitive season. Seasonal statistics SHALL remain available after the season concludes and SHALL NOT be overwritten by later seasons.

#### Scenario: A season's statistics survive later seasons

- **WHEN** later seasons are played
- **THEN** a prior season's statistics SHALL remain available and unchanged

### Requirement: Career Statistics

Every Professional Golfer SHALL accumulate career statistics representing their complete competitive history, and those career statistics SHALL remain permanently available after retirement.

#### Scenario: Career statistics accumulate and persist after retirement

- **WHEN** a golfer plays across seasons and later retires
- **THEN** their career statistics SHALL reflect the accumulation and SHALL remain available after retirement

