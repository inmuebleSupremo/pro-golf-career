# web-play Specification

## Purpose
TBD - created by archiving change add-play-event. Update Purpose after archive.
## Requirements
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

### Requirement: Spatial manual targeting
The play UI SHALL let a human choose an individual club and preview/select a literal canonical aim point on the hole display. It SHALL submit that point, not a derived strategy/distance/lateral decision.

#### Scenario: Player selects a suggested or free point
- **WHEN** a player taps a guidance default or places a custom target
- **THEN** the UI displays the target marker and its relevant distance/reach feedback
- **AND THEN** submission uses the displayed coordinates.

### Requirement: Accessible multi-input targeting
The play UI SHALL support desktop pointer targeting, touch-friendly mobile targeting, and keyboard coarse/fine target adjustment. It SHALL expose target coordinates, selected club, distance, reach/legality messages, and stale-state feedback with accessible labels and non-colour-only indicators.

#### Scenario: Keyboard player adjusts target
- **WHEN** a keyboard user operates target controls
- **THEN** the target moves by documented coarse or fine increments
- **AND THEN** updated position and warnings are announced without relying solely on colour.

### Requirement: Human risk control retirement
The play UI SHALL remove conservative/balanced/aggressive as human shot-selection controls.

#### Scenario: Manual action dock has no strategy input
- **WHEN** a player prepares a manual ball strike
- **THEN** the action controls present club, target, guidance, and submit state
- **AND THEN** no human strategy selector is shown or sent.

### Requirement: Trace-owned result feedback
During visible-shot playback and feedback, the play UI SHALL use the returned trace's intended aim, contact, transition, and final point. It SHALL not derive result feedback from current guidance, selected next aim, strategy, carry/lateral, or client-generated terrain.

#### Scenario: Refetch cannot replace the completed shot target
- **WHEN** the server advances to the next pending shot while the prior visible shot is still being presented
- **THEN** result feedback SHALL display the completed trace's intended aim point
- **AND THEN** it SHALL not substitute the new shot's guidance/default target.

### Requirement: Accessible semantic result feedback
The play UI SHALL distinguish intended target, actual contact, final legal ball position, and any recovery/replay transition with labels and non-colour-only cues. It MAY show a miss vector from aim to contact when helpful.

#### Scenario: Penalty result is understandable without inferred motion
- **WHEN** a trace contains a water-drop or replay transition
- **THEN** the UI SHALL identify the contact and legal recovery/replay destination
- **AND THEN** it SHALL not imply the ball physically travelled between them.

### Requirement: Reduced-motion trace comprehension
When reduced motion is enabled, the play UI SHALL still expose all trace semantic markers and recovery/replay distinction without requiring animation to understand the result.

#### Scenario: Reduced-motion player receives complete result
- **WHEN** a visible shot resolves with reduced motion enabled
- **THEN** the UI SHALL render target, contact, final point, and applicable transition in a stable state
- **AND THEN** it SHALL retain accessible textual result feedback.

### Requirement: Deliberate technique selection
The play UI SHALL support the minimal planning sequence Club → Technique → Landing target → Play shot. It SHALL submit the chosen family in `BallStrikeIntent` and SHALL not infer or silently replace technique from club, distance, lie, or human strategy.

#### Scenario: Unavailable family is understandable
- **WHEN** current guidance marks a family unavailable for the selected club and lie
- **THEN** the UI SHALL present it disabled with its concise reason
- **AND THEN** it SHALL not silently hide or substitute the selection.

### Requirement: Landing-target semantics
For PITCH and CHIP, the UI SHALL identify AimPoint as landing target / intended first contact. It SHALL not predict or present an uncomputed final resting position during planning.

#### Scenario: Planned chip target is not ball rest
- **WHEN** a player selects CHIP and adjusts the marker
- **THEN** the UI SHALL describe that marker as the landing target
- **AND THEN** final position remains resolver-owned until the shot resolves.

### Requirement: Putting boundary remains explicit
The UI SHALL preserve the existing green putting interaction through dedicated `playPutt` while allowing deliberate eligible fringe ball-strike choices. It SHALL not add spatial putt line, break, speed, or slope controls.

#### Scenario: Green does not show ball-strike family selector
- **WHEN** current lie is GREEN
- **THEN** the UI SHALL use the putting path
- **AND THEN** it SHALL not present normal ball-strike family choices.

