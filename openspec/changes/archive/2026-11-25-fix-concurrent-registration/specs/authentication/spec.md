## MODIFIED Requirements

### Requirement: User Registration

The system SHALL let a new user register with a username and password, creating a persistent account whose password is stored only as a BCrypt hash. Usernames SHALL be unique, including when registration requests for the same username run concurrently; registering an already-taken username SHALL be rejected. The plaintext password SHALL never be stored or returned.

#### Scenario: A new account is created

- **WHEN** a client posts a registration with an unused username and a password
- **THEN** an account SHALL be created with the password stored as a BCrypt hash, and the response SHALL NOT contain the plaintext password

#### Scenario: A duplicate username is rejected

- **WHEN** a client registers a username that already exists
- **THEN** the request SHALL be rejected with a conflict error and no second account SHALL be created

#### Scenario: Simultaneous registration of one username

- **WHEN** two clients attempt to register the same unused username at the same time on one backend instance
- **THEN** exactly one request SHALL create an account, the other SHALL receive a conflict error, and only one account with that username SHALL be persisted
