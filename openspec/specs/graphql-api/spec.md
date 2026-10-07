# graphql-api Specification

## Purpose
TBD - created by archiving change add-graphql-read-api. Update Purpose after archive.
## Requirements
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
The API SHALL let the player play or sim their paused event: play the current shot with a ball-strike intent, sim the current shot, sim the rest of the current hole, round, or event, and complete the finished event. Playing or visibly simming one canonical shot SHALL return its resolved outcome and materialized trace; completing the event SHALL resume the paused week and return world status. Playing SHALL authorize the session and reject stale intents before a stroke is resolved.

#### Scenario: Stale intent contains no trace
- **WHEN** a client submits a stale ball-strike revision
- **THEN** the typed stale result SHALL have no outcome and no trace
- **AND THEN** no spatial or scoring state changes.

### Requirement: Persistence-Write Mutations

The API SHALL expose durable-save writes as mutations: save a session under a save id, load a save into a new session, and delete a save. Loading SHALL return the restored session's status (including its new session id) so the client can continue; saving and deleting SHALL confirm success.

#### Scenario: A session is saved and loaded

- **WHEN** a client saves a session under an id and later loads that id
- **THEN** a new session SHALL be created from the save and its returned status SHALL reflect the saved season and week

#### Scenario: An unknown save is loaded

- **WHEN** a client loads or deletes a save id that does not exist
- **THEN** the API SHALL return a not-found-classified GraphQL error

### Requirement: Mutation Input Types
The API SHALL accept structured inputs for mutations that need them: ball-strike intent (catalogue club, absolute aim point, expected shot revision) and career goal (goal type with optional target). Enum-valued arguments SHALL be supplied as enum names and resolved at the API edge; an unrecognized name SHALL yield a client-error-classified GraphQL error.

#### Scenario: A ball-strike intent input is honored
- **WHEN** a client supplies a valid current ball-strike intent
- **THEN** the engine SHALL resolve the shot using that club and literal aim point.

#### Scenario: An unrecognized enum name is rejected
- **WHEN** a client supplies an enum-valued argument whose name is not valid
- **THEN** the API SHALL return a client-error-classified GraphQL error naming the bad value.

### Requirement: Client-Error Classification

The API SHALL translate client-fault engine failures into a client-error (bad-request) classified GraphQL error rather than an opaque internal error. These include acting with no player assigned, acting off-event, an out-of-range pending-offer index, and an invalid enum argument. The not-found classification for unknown sessions and saves SHALL be retained.

#### Scenario: An out-of-range index is rejected

- **WHEN** a client accepts a pending offer at an index outside the pending list
- **THEN** the API SHALL return a bad-request-classified GraphQL error, not an opaque internal error

### Requirement: Owned-Equipment Read Queries

The API SHALL expose, keyed by session id, the player's owned equipment and current tournament loadout: `playerEquipment` returns every item the player owns across all categories, and `playerLoadout` returns the item currently selected in each category. Both SHALL project to the equipment DTO and SHALL return an empty result when no player is assigned rather than an error.

#### Scenario: Owned equipment is listed

- **WHEN** a client queries the player's equipment for a session with an assigned player
- **THEN** the API SHALL return each owned item as a DTO with its name, category, quality, cost, and characteristics

#### Scenario: The current loadout is read

- **WHEN** a client queries the player's loadout for a session with an assigned player
- **THEN** the API SHALL return the item currently selected in each equipment category

#### Scenario: Reads are empty without a player

- **WHEN** a client queries the player's equipment or loadout for a session with no assigned player
- **THEN** the API SHALL return an empty result and SHALL NOT raise an error

### Requirement: Loadout Selection Mutation

The API SHALL let the player switch the tournament loadout to an already-owned item, addressed by its category and name. The mutation SHALL resolve the named owned item and equip it in that category, confirming success. Naming an item the player does not own SHALL yield a client-error-classified GraphQL error.

#### Scenario: An owned item is equipped

- **WHEN** a client runs the select-loadout mutation with the category and name of an item the player owns
- **THEN** the engine SHALL set that category's loadout slot to the item, and a subsequent loadout query SHALL reflect it

#### Scenario: An unowned item is rejected

- **WHEN** a client runs the select-loadout mutation with a category and name the player does not own
- **THEN** the API SHALL return a bad-request-classified GraphQL error, not an opaque internal error

### Requirement: Playing Hole Geometry Query

The API SHALL expose a read-only query returning the geometry of a hole being played in the player's pending event, as an application DTO. The DTO SHALL carry the load-bearing facts needed to render the hole faithfully: the hole number and par, its length, its fairway and green dimensions, its elevation change, whether it carries a greenside bunker, water, and trees, the active round's pin lateral (and depth) offset, the host course's environment classification, and a stable layout seed for deterministic cosmetic placement. The query SHALL NOT mutate state, and no simulation-engine record SHALL appear as a GraphQL type.

#### Scenario: A playing hole's geometry is returned

- **WHEN** a client queries the geometry of a hole in the pending event
- **THEN** the API SHALL return a DTO with the hole's par, length, fairway and green dimensions, elevation, hazard flags, active-round pin offset, course type, and layout seed

#### Scenario: The pin reflects the active round

- **WHEN** the geometry for the same hole is queried for two different rounds of the event
- **THEN** the returned pin lateral (and depth) SHALL reflect each round's pin, while the hole's dimensions and hazard flags SHALL be identical across the rounds

#### Scenario: No engine type leaks

- **WHEN** the GraphQL schema is inspected for the playing-hole query
- **THEN** its return type SHALL be an application DTO, not a simulation-engine record such as `GeneratedHole` or `PinPosition`

#### Scenario: The layout seed is stable

- **WHEN** the geometry for the same hole is queried in two separate sessions of the same world
- **THEN** the returned layout seed SHALL be identical, so the client's synthesized layout is reproducible

### Requirement: Reachable Surfaces On The Shot Situation

The shot-situation projection SHALL additionally expose the current shot's reachable surface profile — the ordered distance bands the shot could find, each partitioned into lateral surface regions (a surface kind and its cumulative lateral extent from the centre outward) — as application DTO data, so the client can render a truthful shot-preview overlay rather than inventing reachable hazards. This SHALL be derived from the same reachable profile the simulation uses to resolve the shot, and SHALL NOT expose any engine record.

#### Scenario: The shot situation carries its reachable surfaces

- **WHEN** a client queries the current shot situation for a pending event
- **THEN** the situation SHALL include an ordered list of reachable distance bands, each with its lateral surface regions, spanning the shot's reach range contiguously

#### Scenario: Reachable surfaces match the resolver's profile

- **WHEN** the reachable surfaces reported for a shot are compared to the profile the simulation resolves that shot against
- **THEN** they SHALL describe the same surfaces, so a surface shown as reachable is one the shot could actually find

### Requirement: Canonical Playing-Geometry Read Model

The GraphQL API SHALL expose an authenticated, session-scoped read model for the current playable hole's **effective** canonical geometry and spatial state. The MVP SHALL add `PlayingHole.geometry` with `tee`, active `cup`, `playableBoundary`, and ordered `regions { surface, boundary }`; `PlayingHole.ball { position, lie }`; and `ShotOutcome.settlement { contact { position, surface }, recoveryPosition, recoveryKind, ball }`. Points SHALL be finite local-yard `{ x, y }` coordinates and all boundary/region lists SHALL use the canonical non-repeated, counter-clockwise polygon order. For an event with a non-neutral course setup, `PlayingHole.geometry` SHALL be the exact setup-specific geometry used by the active hole model for shot settlement. Existing coarse `PlayingHole` fields MAY remain as overview compatibility fields but SHALL NOT be a terrain-rendering source. It SHALL not expose simulation-engine records, SVG markup, rendering instructions, a path/corridor encoding, or a terrain-mutation API.

#### Scenario: Current geometry is projected faithfully

- **WHEN** a player queries the current playable hole during a pending event
- **THEN** the response SHALL contain DTO geometry and spatial state that match the engine's effective canonical terrain and current playable ball state

#### Scenario: Non-neutral setup geometry is projected faithfully

- **WHEN** the pending event applies a non-neutral width setup
- **THEN** the returned terrain regions and playable boundary SHALL match the setup-specific geometry used to resolve that hole

#### Scenario: Geometry is absent off-event

- **WHEN** a client queries canonical playing geometry without a pending playable event
- **THEN** the API SHALL return an absent result according to the existing playable-event read convention and SHALL NOT fabricate terrain

#### Scenario: Engine types remain isolated

- **WHEN** the GraphQL schema and resolver signatures are inspected
- **THEN** canonical geometry and ball state SHALL be represented only by application DTOs, not simulation-engine types

### Requirement: Ball-strike GraphQL contract
The GraphQL API SHALL expose a dedicated ball-strike intent input with a catalogue club ID, absolute aim-point coordinates, and expected opaque shot revision. It SHALL not expose engine-only decision objects as the public contract.

#### Scenario: Intent mutation is validated
- **WHEN** a client submits malformed coordinates, an unknown club, or an out-of-envelope point
- **THEN** the API returns a validation error without resolving a stroke.

### Requirement: Planning guidance projection
The GraphQL API SHALL expose a coherent current-shot planning projection containing canonical ball state, revision, aim envelope, club/reach guidance, and SAFE/PRIMARY/AGGRESSIVE defaults.

#### Scenario: Reach warning is not a server rejection
- **WHEN** guidance reports a selected point beyond approximate club reach
- **THEN** the client can show a warning
- **AND THEN** a point inside the aim envelope remains submittable.

### Requirement: Stale-shot conflict contract
The API SHALL return a distinct retryable stale-shot result when expected revision does not match current round state, without leaking internal mutation details.

#### Scenario: Client refreshes after stale intent
- **WHEN** a stale-shot result is returned
- **THEN** the client can request the latest planning projection before retrying.

### Requirement: Shot trace projection
The GraphQL API SHALL expose an optional `ShotOutcome.trace` for observable canonical shots. The projection SHALL include factual club identity, origin, intended aim point, contact with surface, optional non-flight transition, and final legal point. It SHALL reuse established point/contact representations where practical and SHALL retain `ShotSettlement`.

#### Scenario: Observable ball strike returns trace and settlement
- **WHEN** an authorized client resolves a current canonical ball-strike intent
- **THEN** its outcome SHALL expose the trace that arose from that exact intent and resolution
- **AND THEN** trace final point SHALL agree with `settlement.ball.position`.

#### Scenario: Historical or summary result may have no trace
- **WHEN** an outcome was resolved without trace materialization or predates trace support
- **THEN** its trace field MAY be null
- **AND THEN** existing outcome and settlement consumers SHALL remain valid.

### Requirement: Transition projection is explicitly non-flight
The API SHALL project recovery/replay as an optional trace transition with kind, source contact point, and legal destination. It SHALL not encode this transition as a flight path.

#### Scenario: Out-of-bounds is distinct from endpoint travel
- **WHEN** a shot contacts out of bounds and settlement replays from a legal point
- **THEN** the API SHALL expose the out-of-bounds contact and replay transition
- **AND THEN** the client can distinguish it from ordinary travel to final point.

