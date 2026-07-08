# ranking-history Specification

## Purpose
TBD - created by archiving change add-world-ranking. Update Purpose after archive.
## Requirements
### Requirement: Historical Ranking Records

The system SHALL preserve historical ranking information for every golfer, including previous rankings, career-high ranking, weeks spent at World #1, and season-ending ranking. Historical records SHALL be append-only and permanent.

#### Scenario: Career-high is preserved

- **WHEN** a golfer's ranking improves to a new best and later declines
- **THEN** their career-high ranking SHALL still reflect the best position ever held

#### Scenario: Weeks at number one accumulate

- **WHEN** a golfer holds World #1 across multiple ranking dates
- **THEN** their recorded weeks at #1 SHALL accumulate and SHALL NOT decrease

### Requirement: Ranking Movement

The system SHALL record changes in a golfer's competitive standing over time, including improvement, decline, and entry into or exit from ranking milestones.

#### Scenario: Movement is recorded

- **WHEN** a golfer's ranking position changes between two ranking dates
- **THEN** the change SHALL be recorded as ranking movement

#### Scenario: Milestone crossing is recorded

- **WHEN** a golfer crosses a milestone boundary (into or out of Top 10, etc.)
- **THEN** that entry or exit SHALL be recorded

### Requirement: Ranking Snapshots

The system SHALL preserve ranking snapshots at significant moments (e.g. season conclusion). A snapshot SHALL capture the ranking as it stood, and snapshots SHALL NOT be altered by later ranking changes.

#### Scenario: Snapshot is immutable

- **WHEN** a ranking snapshot is taken and the ranking later changes
- **THEN** the snapshot SHALL still reflect the ranking as it stood when taken

