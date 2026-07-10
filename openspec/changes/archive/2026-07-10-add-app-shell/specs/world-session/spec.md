## ADDED Requirements

### Requirement: Application Runtime

The system SHALL provide a runnable Spring Boot application that boots cleanly and hosts the simulation engine, with the simulation engine remaining framework-free and the application merely wrapping it.

#### Scenario: The application boots

- **WHEN** the application is started
- **THEN** the Spring context SHALL load successfully and expose a health status

### Requirement: World Sessions

The application SHALL manage running World simulations as independent sessions addressed by id, through a service boundary. It SHALL create a session from a seed, hold it, advance it (by week or season), and read its status — without any controller or caller accessing the engine except through that boundary.

#### Scenario: A session is created, advanced, and read

- **WHEN** a world session is created from a seed and then advanced
- **THEN** the session SHALL be retrievable by its id and its status SHALL reflect the advanced state (season, week, active population)

#### Scenario: Sessions are independent

- **WHEN** two world sessions exist
- **THEN** advancing or reading one SHALL NOT affect the other

### Requirement: Status Surface

The application SHALL expose a read-only status of a world session over HTTP, reflecting the engine's state, without exposing gameplay mutation over that surface.

#### Scenario: World status is served

- **WHEN** a world session's status is requested over HTTP
- **THEN** the application SHALL return its current season, week, and active population from the engine
