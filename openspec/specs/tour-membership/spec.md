# tour-membership Specification

## Purpose
TBD - created by archiving change add-tours. Update Purpose after archive.
## Requirements
### Requirement: Single Primary Membership

A Professional Golfer SHALL be a member of exactly one Tour at a time. Membership SHALL determine which Tours' tournaments the golfer is eligible for.

#### Scenario: Exactly one membership

- **WHEN** a golfer's Tour membership is queried
- **THEN** it SHALL identify exactly one Tour

#### Scenario: Changing membership replaces the previous one

- **WHEN** a golfer's membership changes to a new Tour
- **THEN** they SHALL no longer be a member of the previous Tour, and SHALL be a member of exactly the new Tour

### Requirement: Membership History

Membership changes SHALL be recorded in the golfer's history, preserving previous memberships.

#### Scenario: Membership changes are preserved

- **WHEN** a golfer is promoted, relegated, or otherwise changes Tour
- **THEN** the change SHALL be recorded in history and prior memberships SHALL remain retrievable

### Requirement: Qualification Pathways

A golfer SHALL be able to earn Tour membership through defined qualification pathways (e.g. season performance, qualifying, development tours). Qualification rules SHALL be transparent — determinable from published criteria rather than hidden.

#### Scenario: Qualification grants membership

- **WHEN** a golfer meets a Tour's published qualification criteria
- **THEN** they SHALL become eligible for membership of that Tour

#### Scenario: Qualification is transparent

- **WHEN** qualification is evaluated
- **THEN** the outcome SHALL be derivable from published criteria and the golfer's record, not from hidden state

### Requirement: Membership Removal on Departure

A golfer MAY be removed from Tour membership when they leave the active world (for example on retirement). A removed golfer SHALL no longer be a member of any Tour, SHALL be excluded from future Season Standings and promotion/relegation reviews, and their recorded movement history SHALL be preserved.

#### Scenario: Removed golfer leaves active competition

- **WHEN** a golfer is deregistered from Tour membership
- **THEN** they SHALL have no current Tour membership and SHALL not appear in any tier's standings or be moved by a season-end review

#### Scenario: History survives removal

- **WHEN** a golfer who had recorded movements is deregistered
- **THEN** their prior movement history SHALL remain retrievable

