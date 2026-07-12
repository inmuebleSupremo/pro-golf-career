## MODIFIED Requirements

### Requirement: World Runs the Health Cycle

The World SHALL give every golfer a Physical State, gate each event's field by availability, feed fatigue and any injury impairment into shot resolution, accrue fatigue and roll injuries after events, recover fatigue over time, advance injury rehabilitation only for golfers who did not compete that period, and record significant health events. The controlled golfer MAY enter an event while Recovering by choosing to play through it; all other golfers rest recovering injuries and are excluded until able. This SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: Only available golfers enter events

- **WHEN** the World forms a field for a scheduled event
- **THEN** golfers who are injured (early stage) or resting SHALL be excluded, except that the controlled golfer MAY enter while Recovering if they choose to play through

#### Scenario: Competing freezes rehabilitation

- **WHEN** an injured golfer competes in an event during a week
- **THEN** that week SHALL NOT advance their rehabilitation, while a golfer who does not compete SHALL advance one week of rehabilitation

#### Scenario: Competing accrues fatigue and recovery restores it

- **WHEN** a golfer competes in an event and later has a rest week
- **THEN** fatigue SHALL rise after the event and fall over the rest week
