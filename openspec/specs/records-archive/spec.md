# records-archive Specification

## Purpose
TBD - created by archiving change add-statistics. Update Purpose after archive.
## Requirements
### Requirement: Records

The World SHALL maintain records recognising exceptional competitive achievements (for example most tournament victories, lowest scoring performance, most consecutive cuts made, and career longevity). Each record SHALL reference the gameplay event that established it.

#### Scenario: A record references its establishing event

- **WHEN** a record is established
- **THEN** it SHALL identify the golfer and the season/event from which it emerged

### Requirement: Record Integrity

Records SHALL emerge solely from gameplay outcomes and SHALL NEVER be fabricated or manually assigned. When a record is surpassed, both the new record and the previous historical record holder SHALL remain preserved in the World history, and record changes SHALL be logged.

#### Scenario: A surpassed record preserves the previous holder

- **WHEN** a record is surpassed by a new performance
- **THEN** the new holder SHALL become current and the previous holder SHALL remain preserved as record progression

#### Scenario: Records are never fabricated

- **WHEN** the record book is inspected
- **THEN** every record SHALL correspond to an observed gameplay outcome, with none manually assigned

### Requirement: Historical Archive

The World SHALL maintain a permanent, internally consistent Historical Archive preserving significant information (for example tournament results, season summaries, and championship history) that always corresponds to recorded gameplay and remains permanently accessible.

#### Scenario: The archive preserves champions permanently

- **WHEN** tournaments are won across seasons
- **THEN** the archive SHALL record each champion and keep them permanently accessible

### Requirement: Permanent History

The World SHALL preserve historical information throughout the lifetime of the World; it SHALL NOT remove information solely because a golfer retires, a season concludes, rankings change, or a record is broken.

#### Scenario: History survives retirements and broken records

- **WHEN** a golfer retires, a season ends, or a record is broken
- **THEN** the previously recorded history SHALL remain preserved

