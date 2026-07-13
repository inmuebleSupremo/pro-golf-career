# resource-ownership Specification

## Purpose
TBD - created by archiving change add-user-ownership. Update Purpose after archive.
## Requirements
### Requirement: Sessions Are Owned

Every world session SHALL belong to the authenticated user who created it. The application SHALL scope each session operation to the caller's user id, admitting only the owner. A caller who is not the owner SHALL be unable to read, advance, or otherwise act on the session, and the attempt SHALL be reported as not-found so the session's existence is not revealed.

#### Scenario: The owner reaches their session

- **WHEN** the user who created a session reads or advances it
- **THEN** the operation SHALL succeed

#### Scenario: A non-owner is denied a session

- **WHEN** a user reads, advances, or acts on a session created by a different user
- **THEN** the operation SHALL be rejected as not-found, revealing nothing about the session

### Requirement: Saves Are Owned

Every saved game SHALL belong to the user who saved it. Listing saves SHALL return only the caller's saves; loading or deleting a save the caller does not own SHALL be reported as not-found. Autosave SHALL write to the caller's own reserved slot, independent of other users' autosaves.

#### Scenario: A user lists only their own saves

- **WHEN** two users have each saved games and one of them lists saves
- **THEN** only that user's saves SHALL be returned

#### Scenario: A non-owner cannot load or delete a save

- **WHEN** a user loads or deletes a save id that belongs to a different user
- **THEN** the operation SHALL be rejected as not-found

#### Scenario: Autosave is per user

- **WHEN** two users each advance a season
- **THEN** each user's reserved autosave slot SHALL hold their own world, neither overwriting the other

### Requirement: Ownership Is Enforced In The Application Layer

Ownership SHALL be enforced in the application layer, with the authenticated user id passed into the engine seam as a plain value. The engine seam SHALL NOT depend on the security framework, and the simulation core SHALL be unchanged. The GraphQL resolvers SHALL derive the owner from the authenticated principal and never accept it as client input.

#### Scenario: The owner comes from the token, not the request body

- **WHEN** a client calls any owning operation
- **THEN** the owner SHALL be taken from the authenticated principal, and no client-supplied owner field SHALL be honored

#### Scenario: The engine seam stays framework-free

- **WHEN** the engine seam is inspected
- **THEN** it SHALL receive the owner as a plain identifier and SHALL contain no security-framework dependency

