## MODIFIED Requirements

### Requirement: Automatic Event Resolution

When the World advances a week, it SHALL automatically resolve each tournament scheduled that week: draw the field from the event's tour's eligible members, run the Tournament to completion via the shared engine, and record the result. Resolution SHALL use the same rules for all competitors. **When a world has a designated player and reaches an event that player is entered in, it SHALL NOT resolve that event automatically; instead it SHALL build a pending interactive event for the player and pause the week's advance — leaving health recovery, the seasonal transition, and the calendar un-advanced — until the player's event is completed. Completing the player's event SHALL feed its result to every consumer exactly as an automatic resolution would and SHALL resume the paused week (its remaining events, then recovery and any seasonal transition, then the calendar advance). A bulk season advance SHALL sim any pending player event to completion so the world still advances unattended.**

#### Scenario: Scheduled events resolve automatically

- **WHEN** a week containing scheduled tournaments is advanced and no designated player is entered in them
- **THEN** each such tournament SHALL be played to completion and produce a winner and full results

#### Scenario: The player's event yields and pauses the week

- **WHEN** a world with a designated player advances a week containing an event the player is entered in
- **THEN** the World SHALL build a pending interactive event for the player and SHALL NOT advance recovery, the seasonal transition, or the calendar until that event is completed

#### Scenario: Completing the player's event resumes the week

- **WHEN** the player's pending event is completed
- **THEN** its result SHALL feed every consumer as an automatic resolution would and the paused week SHALL resume to completion

#### Scenario: Field is drawn from tour membership

- **WHEN** an event is resolved
- **THEN** its field SHALL consist of eligible members of the event's tour
