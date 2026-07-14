## ADDED Requirements

### Requirement: Advance to an event

The system SHALL let the player advance the career one week at a time, and SHALL surface a way to play their tournament when advancing lands on one.

#### Scenario: Advancing reaches a tournament

- **WHEN** the player advances the week and their golfer has a tournament that week
- **THEN** the career presents a way to play that event

#### Scenario: Advancing an ordinary week

- **WHEN** the player advances the week and their golfer has no tournament that week
- **THEN** the career shows the new week with no pending event

### Requirement: Shot-by-shot play

The system SHALL let the player play a pending event shot by shot, choosing a club, a target distance within the shot's reach range, and a strategy, with the simulation resolving each shot's outcome.

#### Scenario: Play a shot

- **WHEN** the player submits a club, target distance, and strategy for the current shot
- **THEN** the outcome is resolved and the next shot situation (or event completion) is presented

#### Scenario: Situation and standing are shown

- **WHEN** the player is on the play screen for a pending event
- **THEN** the current shot situation and the player's position on the leaderboard are displayed

#### Scenario: Target is constrained to the shot

- **WHEN** the player sets a target distance
- **THEN** it is constrained to the shot's minimum and maximum reach

### Requirement: Sim shortcuts

The system SHALL let the player skip play at any point by simulating the current shot, the rest of the round, or the rest of the event.

#### Scenario: Sim the rest of the event

- **WHEN** the player chooses to sim the rest of the event
- **THEN** the event is resolved without further shot decisions and the player is offered event completion

### Requirement: Complete the event

The system SHALL let the player complete a finished event so its result counts and the week resumes, returning them to the career hub.

#### Scenario: Finish the event

- **WHEN** the event's play is finished and the player completes it
- **THEN** the result is committed, the paused week resumes, and the player is returned to the career hub

### Requirement: Play-state handling

The play surface SHALL handle the absence of a pending event and the usual loading and error states.

#### Scenario: No event to play

- **WHEN** the play screen is opened for a session with no pending event
- **THEN** the player is directed back to the career hub

#### Scenario: Loading and error

- **WHEN** the play data is loading or fails to load
- **THEN** the corresponding loading or error state is shown
