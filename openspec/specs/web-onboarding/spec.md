# web-onboarding Specification

## Purpose
TBD - created by archiving change add-onboarding-create-golfer. Update Purpose after archive.
## Requirements
### Requirement: Onboarding entry point

The system SHALL let an authenticated player start the create-your-golfer flow from the saves screen, and SHALL make this action prominent when the player has no saved careers.

#### Scenario: Start from the saves screen

- **WHEN** an authenticated player selects "Start a new career" on the saves screen
- **THEN** they are taken to the onboarding flow

#### Scenario: Empty state invites creation

- **WHEN** an authenticated player has no saved careers
- **THEN** the empty state presents the create-a-career action as the primary next step

### Requirement: Golfer composition

The system SHALL let the player compose a custom golfer by choosing an identity (first and last name), a nationality, a playing-style archetype, and a starting age, and SHALL constrain each input to values the backend accepts.

#### Scenario: Nationality options

- **WHEN** the player chooses a nationality
- **THEN** the options are exactly the engine's supported nationalities and no other value can be submitted

#### Scenario: Only playing-style archetypes are offered

- **WHEN** the player chooses an archetype
- **THEN** only the five playing-style archetypes are offered, each shown with its play-style strengths and weaknesses, and the AI-only background archetypes are not selectable

#### Scenario: Starting age within range

- **WHEN** the player sets a starting age
- **THEN** the value is constrained to the backend-enforced range of 16 to 22 inclusive, and an out-of-range or missing value is rejected before submission

#### Scenario: Required identity

- **WHEN** the player submits without a first or last name
- **THEN** the form is not submitted and the missing field is identified

### Requirement: Career creation

The system SHALL create and persist the new career on submission by orchestrating, in order, the creation of a world, the creation of the player's golfer within it, and a save, so that the career becomes a durable saved game.

#### Scenario: Successful creation

- **WHEN** the player submits a valid golfer
- **THEN** the system creates a new world, creates the player's golfer in it, and saves it, resulting in a stored saved game owned by the player

#### Scenario: Nothing persists on partial failure

- **WHEN** world or golfer creation fails before the save completes
- **THEN** no saved game is created and the player can retry without a leftover career

#### Scenario: Creation rejected by the backend

- **WHEN** the backend rejects creation (for example, a player has already been assigned to the world, or a value is invalid)
- **THEN** a clear, non-technical error is shown and the player remains in the flow able to correct and retry

### Requirement: Creation confirmation and return

The system SHALL confirm the created golfer to the player and return them to their saves, where the new career appears.

#### Scenario: Confirmation of the created golfer

- **WHEN** creation succeeds
- **THEN** the player sees a confirmation identifying the golfer they created (name, archetype, nationality, and starting age)

#### Scenario: New career appears in saves

- **WHEN** the player continues from the confirmation to the saves screen
- **THEN** the newly created career is listed as a saved game in progress

