# authentication Specification

## Purpose
TBD - created by archiving change add-auth-accounts. Update Purpose after archive.
## Requirements
### Requirement: User Registration

The system SHALL let a new user register with a username and password, creating a persistent account whose password is stored only as a BCrypt hash. Usernames SHALL be unique; registering an already-taken username SHALL be rejected. The plaintext password SHALL never be stored or returned.

#### Scenario: A new account is created

- **WHEN** a client posts a registration with an unused username and a password
- **THEN** an account SHALL be created with the password stored as a BCrypt hash, and the response SHALL NOT contain the plaintext password

#### Scenario: A duplicate username is rejected

- **WHEN** a client registers a username that already exists
- **THEN** the request SHALL be rejected with a conflict error and no second account SHALL be created

### Requirement: Credential Login

The system SHALL authenticate a user by username and password and, on success, issue a JWT token pair: a short-lived access token and a longer-lived refresh token. Invalid credentials SHALL be rejected without revealing whether the username or the password was wrong.

#### Scenario: Valid credentials yield tokens

- **WHEN** a client logs in with a correct username and password
- **THEN** the system SHALL return an access token and a refresh token, the access token identifying the user

#### Scenario: Invalid credentials are rejected

- **WHEN** a client logs in with an unknown username or a wrong password
- **THEN** the system SHALL reject the request as unauthorized and issue no tokens

### Requirement: Token Refresh

The system SHALL exchange a valid refresh token for a new access token without requiring the user to log in again. A token that is not a refresh token, or is invalid or expired, SHALL be rejected.

#### Scenario: A refresh token yields a new access token

- **WHEN** a client posts a valid, unexpired refresh token to the refresh endpoint
- **THEN** the system SHALL return a new access token for the same user

#### Scenario: A non-refresh or invalid token is rejected

- **WHEN** a client posts an access token, or an invalid/expired token, to the refresh endpoint
- **THEN** the system SHALL reject the request as unauthorized and issue no token

### Requirement: API Authentication

The system SHALL require a valid access token to reach the GraphQL API, while leaving the authentication endpoints and the health check publicly reachable. A request to the GraphQL endpoint without a valid access token SHALL be rejected as unauthorized; a request bearing a valid access token SHALL be admitted with the authenticated user available as the security principal.

#### Scenario: The GraphQL endpoint requires a token

- **WHEN** a client calls the GraphQL endpoint with no token or an invalid token
- **THEN** the request SHALL be rejected as unauthorized (401) and no resolver SHALL run

#### Scenario: A valid access token admits the request

- **WHEN** a client calls the GraphQL endpoint with a valid access token
- **THEN** the request SHALL be admitted and the authenticated user SHALL be the security principal

#### Scenario: Auth and health stay public

- **WHEN** a client calls a registration/login/refresh endpoint or the health check without a token
- **THEN** the request SHALL be admitted without authentication

#### Scenario: A refresh token cannot access the API

- **WHEN** a client calls the GraphQL endpoint bearing a refresh token instead of an access token
- **THEN** the request SHALL be rejected as unauthorized

### Requirement: Account Storage

The system SHALL persist user accounts through a storage port so the backing medium can change (a filesystem adapter now, a database later) without affecting the rest of the application. Accounts SHALL survive an application restart, and the store SHALL look accounts up by username and by id.

#### Scenario: Accounts persist across restart

- **WHEN** an account is registered and the application is restarted
- **THEN** the account SHALL still exist and its owner SHALL be able to log in

#### Scenario: Accounts are addressable

- **WHEN** the application needs an account during login or token handling
- **THEN** the store SHALL return it by username or by id, or report its absence

