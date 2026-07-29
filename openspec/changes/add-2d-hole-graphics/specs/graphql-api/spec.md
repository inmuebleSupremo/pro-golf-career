## ADDED Requirements

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

