# tour-movement Specification

## Purpose
TBD - created by archiving change add-tours. Update Purpose after archive.
## Requirements
### Requirement: Season Standings Ledger

A Tour SHALL maintain a Season Standings ledger of cumulative season points, awarded to members from tournament results by finishing position. The ledger SHALL reset at the start of each season. It is distinct from the rolling World Ranking.

#### Scenario: Points accumulate within a season

- **WHEN** a member's tournament results are recorded during a season
- **THEN** their season points SHALL accumulate, with better finishes awarding more points

#### Scenario: Standings reset each season

- **WHEN** a new season begins
- **THEN** the Season Standings SHALL reset to zero for all members

### Requirement: Season-End Membership Review

At the end of a season the Tour hierarchy SHALL review membership: the top performers by Season Standings SHALL be promoted to the next-higher tier and the bottom performers SHALL be relegated to the next-lower tier, per published rules. Movement SHALL take effect at the following season's start and SHALL be recorded in history.

#### Scenario: Top performers are promoted

- **WHEN** the season-end review runs on a tier that has a higher tier above it
- **THEN** the defined number of top-standings golfers SHALL be moved to that higher tier for the next season

#### Scenario: Bottom performers are relegated

- **WHEN** the season-end review runs on a tier that has a lower tier below it
- **THEN** the defined number of bottom-standings golfers SHALL be moved to that lower tier for the next season

#### Scenario: Movement is recorded

- **WHEN** a golfer is promoted or relegated
- **THEN** the movement SHALL be recorded in history

### Requirement: Performance-Based and Deterministic Movement

Promotion and relegation SHALL result solely from competitive performance (season standings) per published rules. Movement SHALL NEVER be random and SHALL NEVER be applied artificially merely to increase variety. Given the same recorded results, the review SHALL produce the same movement.

#### Scenario: No random movement

- **WHEN** the season-end review runs
- **THEN** the promoted and relegated golfers SHALL be determined by standings order, not by any random draw

#### Scenario: Reproducible movement

- **WHEN** the same season's results are reviewed twice
- **THEN** the resulting promotions and relegations SHALL be identical

### Requirement: Upward Migration Over Time

The movement system SHALL let stronger golfers migrate toward higher tiers over multiple seasons while giving developing golfers a real route upward.

#### Scenario: Sustained strong performance rises

- **WHEN** a golfer repeatedly finishes at the top of their tour's standings across seasons
- **THEN** they SHALL progress toward higher tiers rather than being held down artificially

