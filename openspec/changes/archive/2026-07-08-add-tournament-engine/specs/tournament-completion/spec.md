## ADDED Requirements

### Requirement: Tournament Completion

A Tournament SHALL complete only after final scores are confirmed, a single winner is determined, prize amounts are distributed from the prize structure, and statistics are recorded. On completion the Tournament SHALL become read-only.

#### Scenario: Completion requires all finalisation steps

- **WHEN** completion is attempted before a winner is determined or prizes are distributed
- **THEN** the Tournament SHALL NOT complete

#### Scenario: Completed tournament is read-only

- **WHEN** a Tournament has completed
- **THEN** further gameplay changes to it SHALL be rejected

### Requirement: Prize Distribution

On completion, the Tournament SHALL distribute prize amounts to competitors according to finishing position and the Tournament's prize structure. This capability records payout amounts only; it does not maintain a financial ledger (the Economy domain consumes these amounts later).

#### Scenario: Prizes follow finishing position

- **WHEN** prizes are distributed
- **THEN** a better finishing position SHALL receive an amount greater than or equal to a worse position, per the prize structure

### Requirement: Withdrawal

A competitor MAY withdraw before or during a Tournament. Withdrawal SHALL be permanently recorded, SHALL NOT invalidate the Tournament, and the Tournament SHALL continue for the remaining field.

#### Scenario: Withdrawal does not invalidate the event

- **WHEN** a competitor withdraws mid-tournament
- **THEN** the withdrawal SHALL be recorded, the competitor SHALL leave the active field, and the Tournament SHALL continue to completion

### Requirement: Competitive Integrity and Reproducibility

Every competitor SHALL be evaluated under identical tournament rules with no control-type-specific advantage. A completed Tournament SHALL be reproducible from its seed and inputs.

#### Scenario: Identical rules for all

- **WHEN** the Tournament is run
- **THEN** no competitor SHALL receive a rule variation or hidden advantage based on control type

#### Scenario: Reproducible outcome

- **WHEN** the same Tournament (seed, course, field) is run twice
- **THEN** the scores, cut, winner, and prize distribution SHALL be identical

### Requirement: Permanent History

A completed Tournament SHALL become part of permanent, append-only history including the Course, competitors, scores, cut result, winner, and prize distribution. History SHALL remain available and SHALL NOT be overwritten.

#### Scenario: History is preserved

- **WHEN** a Tournament has completed
- **THEN** its Course, competitors, scores, winner, and prize distribution SHALL be retrievable and immutable
