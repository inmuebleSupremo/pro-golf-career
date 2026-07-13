## ADDED Requirements

### Requirement: GraphQL API Surface

The application SHALL expose a schema-first GraphQL API over the world engine, served through Spring GraphQL, as the primary API surface. It SHALL make an interactive GraphiQL explorer available for manual use. All GraphQL types, resolvers, and mapping SHALL live in the application layer; the simulation core SHALL remain free of any API or framework dependency.

#### Scenario: The GraphQL endpoint is served

- **WHEN** the application is running
- **THEN** a GraphQL endpoint SHALL accept queries and mutations defined by the schema, and the GraphiQL explorer SHALL be reachable

#### Scenario: The engine stays framework-free

- **WHEN** the API is built
- **THEN** every GraphQL resolver, DTO, and schema file SHALL reside in the application layer and the simulation core SHALL contain no API/framework import

### Requirement: Engine Type Isolation

The GraphQL schema SHALL expose only application-layer DTO types. No raw simulation-engine record or entity (for example a golfer, a leaderboard entry, a schedule entry, a sponsorship offer, or a shot situation) SHALL appear as a GraphQL type or as a resolver return type; the application SHALL map engine read types to DTOs at the API edge.

#### Scenario: A leaderboard is projected to a DTO

- **WHEN** a query returns the field leaderboard for a session with a pending player event
- **THEN** each row SHALL be an application DTO carrying the golfer's identity (id and name) and standing, not the engine's golfer object

#### Scenario: No engine type leaks into the schema

- **WHEN** the GraphQL schema is inspected
- **THEN** it SHALL contain no type whose shape is a simulation-engine record; only application DTOs SHALL be present

### Requirement: World Lifecycle Mutations

The API SHALL let a client create and advance world sessions. It SHALL create a session from a seed, optionally with an explicit world configuration, and SHALL advance an existing session by a single week or a full season, returning the resulting status.

#### Scenario: A world is created from a seed

- **WHEN** a client runs the create-world mutation with a seed and no configuration
- **THEN** a new session SHALL be created with the engine's default configuration and its id and initial status SHALL be returned

#### Scenario: A world is created with an explicit configuration

- **WHEN** a client runs the create-world mutation with a seed and a configuration input
- **THEN** a new session SHALL be created honoring the supplied configuration fields, with unset fields taking engine defaults

#### Scenario: A world is advanced

- **WHEN** a client runs the advance-season or advance-week mutation for a known session id
- **THEN** the session SHALL advance accordingly and the returned status SHALL reflect the new season and week

### Requirement: World Status Query

The API SHALL serve a read-only status of a world session by id, reflecting the engine's current state: the season, the week, the active population size, and whether the session is paused awaiting a player event.

#### Scenario: Status is read for a known session

- **WHEN** a client queries the world status for a known session id
- **THEN** the API SHALL return the current season, week, active population, and pending-player-event flag from the engine

### Requirement: Read Model Queries

The API SHALL expose the engine's player-facing read model as queries keyed by session id: the player's reviewable schedule, career-goal progress, the world's Hall-of-Fame inductions, and the player's pending sponsorship, staff, and equipment decisions. Each SHALL project engine read types into DTOs. When no player is assigned or no data exists, a query SHALL return an empty result rather than an error.

#### Scenario: The player schedule is read

- **WHEN** a client queries the player schedule for a session with an assigned player
- **THEN** the API SHALL return each eligible upcoming event as a DTO with its week, tour tier, prestige, and entered status

#### Scenario: Career goals report live progress

- **WHEN** a client queries career goals for a session whose player has chosen goals
- **THEN** the API SHALL return each goal as a DTO with its current value, target, and achieved status evaluated from live state

#### Scenario: Pending decisions are listed

- **WHEN** a client queries pending sponsorship, staff, or equipment offers for a session
- **THEN** the API SHALL return the current pending offers as DTOs, or an empty list when there are none

#### Scenario: Hall-of-Fame inductions are read

- **WHEN** a client queries the Hall of Fame for a session
- **THEN** the API SHALL return each induction as a DTO with the inducted golfer, the season, and the induction score

### Requirement: Playable Event Read Queries

The API SHALL expose the read side of a paused player event by session id: the current shot situation, the live event leaderboard, and whether the player made the cut. When the session is not paused at a player event, these queries SHALL return an empty/absent result rather than an error.

#### Scenario: The current situation is read during a pending event

- **WHEN** a client queries the current shot situation for a session paused at a player event
- **THEN** the API SHALL return the situation as a DTO describing the hole, distance, lie, and shot context

#### Scenario: Reads are safe off-event

- **WHEN** a client queries the current situation or leaderboard for a session that is not paused at a player event
- **THEN** the API SHALL return an absent/empty result and SHALL NOT raise an error

### Requirement: Saved Games Query

The API SHALL list the stored saved games as DTOs, newest first, each carrying the save's metadata.

#### Scenario: Saves are listed

- **WHEN** a client queries the list of saves
- **THEN** the API SHALL return each stored save's metadata as a DTO

### Requirement: Error Classification

The API SHALL translate known engine-boundary failures into typed GraphQL errors rather than opaque server errors. A request naming an unknown world session SHALL yield a not-found-classified error, and a read for an unknown save SHALL likewise be not-found-classified.

#### Scenario: An unknown session is queried

- **WHEN** a client queries or mutates using a session id that does not exist
- **THEN** the API SHALL return a GraphQL error classified as not-found, not an opaque internal error
