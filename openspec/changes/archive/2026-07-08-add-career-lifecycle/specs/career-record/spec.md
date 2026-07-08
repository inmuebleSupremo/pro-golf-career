## ADDED Requirements

### Requirement: Cumulative Career Statistics

A Career SHALL maintain cumulative statistics updated from tournament results: events played, cuts made, wins, runner-up finishes, top-10 finishes, average finish, and total earnings. Statistics SHALL update automatically as results are recorded and SHALL remain historically accurate.

#### Scenario: Statistics update from a result

- **WHEN** a tournament result is recorded for the golfer
- **THEN** events played SHALL increment, and wins / cuts made / top-10s / earnings SHALL update according to that result

#### Scenario: Average finish reflects recorded finishes

- **WHEN** multiple results have been recorded
- **THEN** the average finish SHALL equal the mean of the golfer's recorded finishing positions

### Requirement: Career Milestones

A Career SHALL automatically record objective milestones when the triggering gameplay event first occurs — for example first event, first made cut, first top-10, first win. Milestones SHALL be objective and SHALL prevent duplicate recording of the same milestone.

#### Scenario: First occurrence records a milestone

- **WHEN** the golfer makes a cut for the first time
- **THEN** a "first made cut" milestone SHALL be recorded

#### Scenario: Duplicate milestones are prevented

- **WHEN** the golfer wins a second tournament
- **THEN** no second "first win" milestone SHALL be recorded

### Requirement: Chronological Career History

A Career SHALL maintain a chronological, immutable history of tournament participation and significant events. Entries SHALL be ordered by date and SHALL not be altered once recorded.

#### Scenario: History is ordered and immutable

- **WHEN** results and milestones are recorded across dates
- **THEN** the career history SHALL present them in chronological order, and existing entries SHALL remain unchanged
