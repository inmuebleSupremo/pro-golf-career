## ADDED Requirements

### Requirement: User login

The system SHALL allow a user to authenticate by submitting a username and password to the backend REST `/auth/login` endpoint, and SHALL establish an authenticated session on success.

#### Scenario: Successful login

- **WHEN** a user submits valid credentials
- **THEN** the backend issues an access token and a refresh token, an authenticated session is established, and the user is taken to an authenticated route

#### Scenario: Invalid credentials

- **WHEN** a user submits credentials the backend rejects
- **THEN** the user remains unauthenticated and a clear, non-leaking error message is shown

#### Scenario: Client-side validation

- **WHEN** the login form is submitted with an empty or malformed field
- **THEN** the request is not sent and a validation message identifies the offending field

### Requirement: User registration

The system SHALL allow a new user to create an account by submitting a username and password to the backend REST `/auth/register` endpoint.

#### Scenario: Successful registration

- **WHEN** a user submits a valid new username and password
- **THEN** the account is created and the user can proceed to log in

#### Scenario: Username already taken

- **WHEN** a user registers with a username that already exists
- **THEN** the conflict is surfaced as a clear field-level error and no session is established

### Requirement: Token transport and session persistence

The system SHALL keep the access and refresh tokens out of client-accessible JavaScript by transporting them through a Backend-For-Frontend layer that stores them in httpOnly cookies, and SHALL persist the authenticated session across page reloads.

#### Scenario: Tokens not exposed to client JavaScript

- **WHEN** the user is authenticated
- **THEN** the access and refresh tokens are held in httpOnly cookies and are not readable from client-side JavaScript or web storage

#### Scenario: Session survives reload

- **WHEN** an authenticated user reloads the page
- **THEN** the session is recognised as authenticated without re-entering credentials

### Requirement: Access token refresh

The system SHALL transparently obtain a new access token via the backend REST `/auth/refresh` endpoint when the current access token has expired, using the stored refresh token, without interrupting the user.

#### Scenario: Silent refresh on expiry

- **WHEN** an authenticated request is made after the access token has expired but the refresh token is still valid
- **THEN** a new access token is obtained using the refresh token and the original request completes

#### Scenario: Refresh token invalid or expired

- **WHEN** the refresh token is rejected by the backend
- **THEN** the session is ended and the user is returned to the login screen

### Requirement: Route protection and logout

The system SHALL restrict authenticated routes to logged-in users, redirect unauthenticated users to the login screen, and allow a user to log out and end their session.

#### Scenario: Unauthenticated access is redirected

- **WHEN** an unauthenticated user requests an authenticated route
- **THEN** they are redirected to the login screen

#### Scenario: Logout ends the session

- **WHEN** an authenticated user logs out
- **THEN** the session and stored tokens are cleared and authenticated routes are no longer accessible
