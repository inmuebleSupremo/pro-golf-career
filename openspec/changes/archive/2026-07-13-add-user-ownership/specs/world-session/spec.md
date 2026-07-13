## MODIFIED Requirements

### Requirement: World Sessions

The application SHALL manage running World simulations as independent sessions addressed by id, through a service boundary, each owned by the authenticated user who created it. It SHALL create a session from a seed, hold it, advance it (by week or season), and read its status — without any controller or caller accessing the engine except through that boundary, and only for the session's owner. A caller who is not the owner SHALL be unable to retrieve or advance the session.

#### Scenario: A session is created, advanced, and read

- **WHEN** a world session is created from a seed by a user and then advanced by that same user
- **THEN** the session SHALL be retrievable by its id and its status SHALL reflect the advanced state (season, week, active population)

#### Scenario: Sessions are independent

- **WHEN** two world sessions exist
- **THEN** advancing or reading one SHALL NOT affect the other

#### Scenario: Only the owner may reach a session

- **WHEN** a user attempts to retrieve or advance a session created by a different user
- **THEN** the application SHALL deny it as not-found, as if the session did not exist
