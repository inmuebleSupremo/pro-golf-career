## ADDED Requirements

### Requirement: World Runs the Statistics Archive

The World SHALL run a Statistics archive, feeding each tournament result and season boundary into it to accumulate per-golfer statistics, register champions, and update records. The archive SHALL NOT affect any simulation outcome, and its accumulation SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: Results and seasons feed the archive

- **WHEN** the World resolves events and completes seasons
- **THEN** it SHALL feed those outcomes into the Statistics archive, accumulating statistics, registering champions, and updating records

#### Scenario: The archive changes no outcome

- **WHEN** the Statistics archive records outcomes during World progression
- **THEN** no tournament result, ranking, career, or progression outcome SHALL be changed by it

#### Scenario: The archive is reproducible

- **WHEN** two worlds with the same master seed advance through the same seasons
- **THEN** their statistics, records, and champions archives SHALL be identical
