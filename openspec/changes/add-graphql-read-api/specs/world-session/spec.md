## MODIFIED Requirements

### Requirement: Status Surface

The application SHALL expose a read-only status of a world session, reflecting the engine's state, without exposing gameplay mutation over that read surface. This status SHALL be served through the GraphQL API (capability `graphql-api`); the provisional REST status endpoint is removed.

#### Scenario: World status is served

- **WHEN** a world session's status is requested through the GraphQL API
- **THEN** the application SHALL return its current season, week, and active population from the engine

#### Scenario: The provisional REST status endpoint is gone

- **WHEN** the former REST status path is requested
- **THEN** it SHALL no longer be served, status being available only through the GraphQL API
