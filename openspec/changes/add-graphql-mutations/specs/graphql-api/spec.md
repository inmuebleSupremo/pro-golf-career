## ADDED Requirements

### Requirement: Player-Control Mutations

The API SHALL expose the player's standing management decisions as mutations, each delegating to the world engine: designate or create the player golfer, set the development focus, set resting, skip or re-enter a scheduled event, set career goals, and accept a pending sponsorship, hire or release staff, and buy a pending equipment upgrade. Creating a player SHALL return the new golfer's id; the other decisions SHALL confirm success.

#### Scenario: A custom player is created

- **WHEN** a client runs the create-player mutation with an identity and archetype for a session
- **THEN** a human-controlled golfer SHALL be created and admitted, and its new id SHALL be returned

#### Scenario: Career goals are set and then read back

- **WHEN** a client sets the player's career goals via mutation and then queries career goals
- **THEN** the chosen goals SHALL be reflected with their live progress

#### Scenario: A pending decision is accepted

- **WHEN** a client accepts a pending sponsorship, hires a pending staff candidate, or buys a pending equipment upgrade by index
- **THEN** the engine SHALL apply the decision when affordable/valid and the corresponding pending list SHALL no longer contain it

### Requirement: Playable-Event Mutations

The API SHALL let the player play or sim their paused event: play the current shot with a decision, sim the current shot, sim the rest of the current hole, round, or event, and complete the finished event. Playing or simming a shot SHALL return the resolved shot outcome; completing the event SHALL resume the paused week and return the new world status.

#### Scenario: A shot is played with a decision

- **WHEN** a client plays the current shot with a club, target, and strategy for a session paused at a player event
- **THEN** the engine SHALL resolve the shot and the API SHALL return its outcome (final surface, carry, remaining distance, strokes)

#### Scenario: The event is simmed and completed

- **WHEN** a client sims the remainder of the event and then completes it
- **THEN** the event's result SHALL count, the paused week SHALL resume, and the returned status SHALL reflect the advanced world

#### Scenario: Acting off-event is rejected

- **WHEN** a client plays a shot or completes an event for a session with no pending player event
- **THEN** the API SHALL return a client-error-classified GraphQL error, not an opaque internal error

### Requirement: Persistence-Write Mutations

The API SHALL expose durable-save writes as mutations: save a session under a save id, load a save into a new session, and delete a save. Loading SHALL return the restored session's status (including its new session id) so the client can continue; saving and deleting SHALL confirm success.

#### Scenario: A session is saved and loaded

- **WHEN** a client saves a session under an id and later loads that id
- **THEN** a new session SHALL be created from the save and its returned status SHALL reflect the saved season and week

#### Scenario: An unknown save is loaded

- **WHEN** a client loads or deletes a save id that does not exist
- **THEN** the API SHALL return a not-found-classified GraphQL error

### Requirement: Mutation Input Types

The API SHALL accept structured inputs for the mutations that need them: a shot decision (club, target distance, optional lateral aim, strategy) and a career goal (goal type with an optional target). Enum-valued arguments SHALL be supplied as their enum names and resolved at the API edge; an unrecognized name SHALL yield a client-error-classified GraphQL error rather than a server error.

#### Scenario: A shot decision input is honored

- **WHEN** a client supplies a shot-decision input with a club, target distance, and strategy
- **THEN** the engine SHALL resolve the shot using those values, defaulting the lateral aim to straight when omitted

#### Scenario: An unrecognized enum name is rejected

- **WHEN** a client supplies an enum-valued argument whose name is not a valid engine value
- **THEN** the API SHALL return a client-error-classified GraphQL error naming the bad value

### Requirement: Client-Error Classification

The API SHALL translate client-fault engine failures into a client-error (bad-request) classified GraphQL error rather than an opaque internal error. These include acting with no player assigned, acting off-event, an out-of-range pending-offer index, and an invalid enum argument. The not-found classification for unknown sessions and saves SHALL be retained.

#### Scenario: An out-of-range index is rejected

- **WHEN** a client accepts a pending offer at an index outside the pending list
- **THEN** the API SHALL return a bad-request-classified GraphQL error, not an opaque internal error
