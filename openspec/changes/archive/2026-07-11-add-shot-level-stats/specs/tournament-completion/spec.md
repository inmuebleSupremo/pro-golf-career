## MODIFIED Requirements

### Requirement: Tournament Completion

A Tournament SHALL complete only after final scores are confirmed, a single winner is determined, prize amounts are distributed from the prize structure, and statistics are recorded. On completion the Tournament SHALL become read-only. **Each competitor's finishing record SHALL additionally carry their shot-level statistics for the event (fairways hit and possible, greens in regulation, holes played, and putts), so those statistics can be recorded.**

#### Scenario: Completion requires all finalisation steps

- **WHEN** completion is attempted before a winner is determined or prizes are distributed
- **THEN** the Tournament SHALL NOT complete

#### Scenario: Finishing records carry shot statistics

- **WHEN** a Tournament completes
- **THEN** each competitor's finishing record SHALL include their shot-level statistics for the event

#### Scenario: Completed tournament is read-only

- **WHEN** a Tournament has completed
- **THEN** further gameplay changes to it SHALL be rejected
