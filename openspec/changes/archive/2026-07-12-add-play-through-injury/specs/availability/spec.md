## MODIFIED Requirements

### Requirement: Availability

Every Professional Golfer SHALL possess a current Availability status, derived from their Physical State, that determines whether they may enter competitive events. Availability SHALL distinguish at least Available, Resting, Recovering, and Injured. The Recovering status (an injury in its final rehabilitation stage) MAY permit a controlled golfer to enter an event by playing through the injury at an impairment; the Injured status (the early stage of a significant injury) and Resting SHALL never permit competition.

#### Scenario: Availability is derived from physical state

- **WHEN** a golfer's Physical State indicates an active injury or excessive fatigue
- **THEN** their Availability SHALL reflect it (Injured, Recovering, or Resting) rather than Available

#### Scenario: Availability gates event entry

- **WHEN** a field is formed for an event
- **THEN** golfers who are Injured (early stage) or Resting SHALL be excluded, and only golfers who are Available — or a controlled golfer who is Recovering and chooses to play through — SHALL be entered
