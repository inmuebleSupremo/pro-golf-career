## MODIFIED Requirements

### Requirement: Advance to an event

The system SHALL let the player advance the career one week at a time, and SHALL surface a way to play their tournament when advancing lands on one. When a same-season advance creates a pending player event, the player SHALL remain on the Career Hub with a Play Event action and SHALL enter gameplay only by choosing that action. Crossing a season boundary SHALL retain the existing Season Review transition.

#### Scenario: Advancing reaches a tournament

- **WHEN** the player advances the week and their golfer has a tournament that week
- **THEN** the Career Hub SHALL remain visible and present a Play Event action for the pending event

#### Scenario: Advancing an ordinary week

- **WHEN** the player advances the week and their golfer has no tournament that week
- **THEN** the career shows the new week with no pending event

#### Scenario: Season boundary takes priority

- **WHEN** advancing crosses into the next season
- **THEN** the player SHALL reach Season Review before entering a new-season event
