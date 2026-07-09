## ADDED Requirements

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
