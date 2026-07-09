## ADDED Requirements

### Requirement: World-Owned Weather System

The World Simulation SHALL maintain a Weather System that generates environmental conditions for competitive play. The Weather System SHALL exist independently of any individual Tournament or Professional Golfer, SHALL be available to dependent systems, and SHALL persist throughout World progression.

#### Scenario: Weather is managed by the World

- **WHEN** the World advances and resolves a scheduled event
- **THEN** the World's Weather System SHALL provide the environmental conditions for that event, independently of any golfer

#### Scenario: Weather persists across progression

- **WHEN** the World progresses across multiple weeks and seasons
- **THEN** the Weather System SHALL remain available and continue to generate conditions

### Requirement: Deterministic Weather Generation

Weather generation SHALL be a pure function of the world seed hierarchy — the same world seed, season, and tournament SHALL always produce the same conditions, and its randomness SHALL be isolated from other random streams (such as shot resolution).

#### Scenario: Same seed reproduces conditions

- **WHEN** the Weather System generates conditions for the same world seed, season, and tournament twice
- **THEN** it SHALL produce identical Playing Conditions both times

#### Scenario: Weather stream is isolated

- **WHEN** weather is generated for a tournament
- **THEN** its random draws SHALL not consume or perturb the shot-resolution stream, so play remains reproducible

### Requirement: Seasonal Climate Patterns

Different course classifications and points in the season MAY produce different patterns of conditions, and those climate patterns SHALL remain internally consistent throughout the simulation. Seasonal climate SHALL contribute to course identity and World variety.

#### Scenario: Classification shapes the climate

- **WHEN** conditions are generated for courses of different environmental classifications under the same season phase
- **THEN** their characteristic conditions SHALL differ in a consistent way (for example, an exposed links tends windier than a sheltered parkland)

#### Scenario: Season phase shifts conditions

- **WHEN** conditions are generated for the same course at different points in the season
- **THEN** the seasonal pattern SHALL shift accordingly while remaining internally consistent

### Requirement: Weather Domain Responsibility

The Weather domain SHALL be responsible only for weather generation, Playing Conditions, environmental state, climate patterns, and forecast information. It SHALL NOT perform shot calculations, tournament scheduling, course generation, ranking, or player progression; those responsibilities SHALL remain with their respective domains.

#### Scenario: Weather does not own other responsibilities

- **WHEN** the Weather domain produces conditions
- **THEN** it SHALL only describe environmental state and SHALL delegate shot calculation, scheduling, course generation, ranking, and progression to their owning domains
