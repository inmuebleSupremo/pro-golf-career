# tournament-play Specification

## Purpose
TBD - created by archiving change add-tournament-engine. Update Purpose after archive.
## Requirements
### Requirement: Tournament Lifecycle

A Tournament SHALL progress through its lifecycle in order: Scheduled → Registration Open → Field Confirmed → Round 1 → Round 2 → Cut Evaluation → Round 3 → Round 4 → Playoff (if required) → Completed. States SHALL NOT be skipped, except that the Playoff stage is entered only when required.

#### Scenario: States occur in order

- **WHEN** a Tournament advances
- **THEN** it SHALL move to the next lifecycle state and SHALL NOT skip an intermediate state

#### Scenario: Playoff only when required

- **WHEN** the final round completes with a single outright leader
- **THEN** the Tournament SHALL proceed to Completed without a Playoff

### Requirement: Four-Round Play Through the Shared Engine

Every competitor's round SHALL be resolved through the shared round-resolution engine — the same path for all control types, with no separate human or simulation scoring. A Tournament consists of four rounds; scores carry forward between rounds; completed rounds are immutable.

#### Scenario: All competitors use the shared engine

- **WHEN** a round is played
- **THEN** every active competitor's hole scores SHALL be produced by the shared round resolver, with no control-type-specific scoring

#### Scenario: Scores carry forward and completed rounds are immutable

- **WHEN** a round completes
- **THEN** its scores SHALL be added to the running totals and SHALL NOT change afterward

### Requirement: Scoring

Each completed hole SHALL contribute to a competitor's cumulative Tournament score, measured as strokes relative to Course par. Lower scores represent better performance.

#### Scenario: Hole scores accumulate relative to par

- **WHEN** a competitor completes holes
- **THEN** their Tournament score SHALL equal total strokes minus the par of the holes played, updated as holes complete

#### Scenario: Lower is better

- **WHEN** two competitors are compared
- **THEN** the one with the lower cumulative score SHALL rank ahead

### Requirement: Live Leaderboard

A Tournament SHALL maintain a live leaderboard ranking active competitors by cumulative score. It SHALL be recomputed whenever a score changes, and ties SHALL be handled consistently (tied competitors share a position).

#### Scenario: Leaderboard recomputes on score change

- **WHEN** a competitor's score changes
- **THEN** the leaderboard positions SHALL be recomputed

#### Scenario: Ties share a position

- **WHEN** two competitors have identical cumulative scores
- **THEN** they SHALL share the same leaderboard position, applied consistently

