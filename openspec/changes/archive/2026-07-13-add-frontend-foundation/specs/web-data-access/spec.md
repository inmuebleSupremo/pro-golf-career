## ADDED Requirements

### Requirement: Server-state management

The system SHALL manage all server-derived state through TanStack Query, providing caching, loading, and error states for API reads.

#### Scenario: Query exposes lifecycle states

- **WHEN** a component reads server data through the query layer
- **THEN** it can render distinct loading, error, and success states from that query

#### Scenario: Server state is not duplicated in ad-hoc state

- **WHEN** server-derived data is needed by the UI
- **THEN** it is sourced from the TanStack Query cache rather than being copied into unmanaged local/global state

### Requirement: Typed GraphQL client

The system SHALL access the backend GraphQL API through a client whose operation and result types are generated at build time from `backend/src/main/resources/graphql/schema.graphqls`, so that queries are type-checked against the real schema.

#### Scenario: Types generated from the schema

- **WHEN** code generation runs against the backend GraphQL schema
- **THEN** typed operations and result types are produced and a query that does not match the schema fails type-checking

#### Scenario: Schema drift is caught

- **WHEN** a GraphQL operation references a field that does not exist in the schema
- **THEN** the build or type-check fails rather than the error surfacing only at runtime

### Requirement: Authenticated request path to the API

The system SHALL route browser GraphQL requests through a same-origin Backend-For-Frontend proxy that attaches the user's access token server-side when calling the backend `/graphql` endpoint, so the browser never calls the cross-origin backend directly.

#### Scenario: Browser stays same-origin

- **WHEN** the browser issues a GraphQL request
- **THEN** it targets a same-origin route which forwards the request to the backend `/graphql` endpoint with the access token attached server-side

#### Scenario: Unauthenticated request is rejected

- **WHEN** a GraphQL request is made without a valid session
- **THEN** the proxy does not attach a token and the request is treated as unauthenticated

### Requirement: Saves list read

The system SHALL let an authenticated user view their list of saved games, sourced from the GraphQL `listSaves` query, as the proof that the foundation stack works end to end.

#### Scenario: Saves are listed

- **WHEN** an authenticated user opens the saves view
- **THEN** the `listSaves` query runs through the typed client and the returned saves are displayed with their metadata

#### Scenario: Empty state

- **WHEN** an authenticated user has no saved games
- **THEN** an intentional empty state is shown rather than a blank or broken view

#### Scenario: Load or error feedback

- **WHEN** the saves query is loading or fails
- **THEN** the view shows the corresponding loading or error state
