# world-progression Specification

## Purpose
TBD - created by archiving change add-world-loop. Update Purpose after archive.
## Requirements
### Requirement: Automatic Event Resolution

When the World advances a week, it SHALL automatically resolve each tournament scheduled that week: draw the field from the event's tour's eligible members, run the Tournament to completion via the shared engine, and record the result. Resolution SHALL use the same rules for all competitors.

#### Scenario: Scheduled events resolve automatically

- **WHEN** a week containing scheduled tournaments is advanced
- **THEN** each such tournament SHALL be played to completion and produce a winner and full results

#### Scenario: Field is drawn from tour membership

- **WHEN** an event is resolved
- **THEN** its field SHALL consist of eligible members of the event's tour

### Requirement: Results Feed Ranking, Standings, and Careers

Each resolved tournament result SHALL be fed to the World Ranking, the event tour's Season Standings, and every competitor's Career. These consumers SHALL be updated from the same result.

#### Scenario: One result updates all consumers

- **WHEN** a tournament result is produced
- **THEN** the World Ranking, the tour's Season Standings, and each competitor's Career record SHALL all be updated from that result

### Requirement: Seasonal Transition

At season end the World SHALL run a seasonal transition that: takes a season-ending ranking snapshot; runs Tour promotion/relegation; advances every golfer's Career by one season (retiring those who reach the mandatory retirement age); replenishes the population for departures; and generates the next season's calendar.

#### Scenario: Transition fires every seam

- **WHEN** the seasonal transition runs
- **THEN** a ranking snapshot SHALL be taken, tour promotion/relegation SHALL be applied, every Career SHALL advance one season, departures SHALL be replenished, and the next season's schedule SHALL be generated

#### Scenario: Retirement occurs through the transition

- **WHEN** a golfer reaches the mandatory retirement age during the transition
- **THEN** that golfer SHALL retire and SHALL be replaced so the population remains sufficient

### Requirement: Coordination Boundary

The World SHALL drive only the public operations of the owning domains. It SHALL NOT compute shot outcomes, ranking points, prize finances, or promotion/relegation itself — it composes the domains that own those computations.

#### Scenario: World delegates domain work

- **WHEN** the World runs progression or a transition
- **THEN** shot outcomes, ranking values, and tour movement SHALL be produced by their owning domains, invoked by the World

