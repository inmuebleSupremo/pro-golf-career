## ADDED Requirements

### Requirement: World Runs the Health Cycle

The World SHALL give every golfer a Physical State, gate each event's field by availability, feed fatigue into shot resolution, accrue fatigue and roll injuries after events, recover fatigue and advance rehabilitation over time, and record significant health events. This SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: Only available golfers enter events

- **WHEN** the World forms a field for a scheduled event
- **THEN** golfers who are injured, recovering, or resting SHALL be excluded from the field

#### Scenario: Competing accrues fatigue and recovery restores it

- **WHEN** the World resolves events and advances weeks
- **THEN** competitors SHALL accrue fatigue from participation, and golfers SHALL recover fatigue and advance any rehabilitation over time

#### Scenario: Health keeps the world reproducible

- **WHEN** two worlds with the same master seed advance through the same seasons
- **THEN** their golfers' physical state SHALL be identical
