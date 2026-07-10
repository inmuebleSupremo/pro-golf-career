## MODIFIED Requirements

### Requirement: Records

The World SHALL maintain records recognising exceptional competitive achievements (for example most tournament victories, **most major championships won**, lowest scoring performance, most consecutive cuts made, and career longevity). Each record SHALL reference the gameplay event that established it.

#### Scenario: A record references its establishing event

- **WHEN** a record is established
- **THEN** it SHALL identify the golfer and the season/event from which it emerged

#### Scenario: Most majors is a recognised record

- **WHEN** a golfer wins more majors than any previous holder
- **THEN** the most-major-championships record SHALL update to that golfer, preserving the previous holder as progression

### Requirement: Historical Archive

The World SHALL maintain a permanent, internally consistent Historical Archive preserving significant information (for example tournament results, season summaries, and championship history) that always corresponds to recorded gameplay and remains permanently accessible. **Each recorded championship SHALL preserve the event's prestige, so major championships are distinguishable in history.**

#### Scenario: The archive preserves champions permanently

- **WHEN** tournaments are won across seasons
- **THEN** the archive SHALL record each champion and keep them permanently accessible

#### Scenario: Major champions are distinguishable

- **WHEN** the championship history is inspected
- **THEN** each championship SHALL carry its event's prestige, so majors can be identified among all wins
