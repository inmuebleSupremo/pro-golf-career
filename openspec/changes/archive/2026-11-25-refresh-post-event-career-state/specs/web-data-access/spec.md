## MODIFIED Requirements

### Requirement: Server-state management

The system SHALL manage all server-derived state through TanStack Query, providing caching, loading, and error states for API reads. After successful event completion or week advancement, it SHALL invalidate affected cached player-summary reads for that session so subsequent views refresh from the backend.

#### Scenario: Query exposes lifecycle states

- **WHEN** a component reads server data through the query layer
- **THEN** it can render distinct loading, error, and success states from that query

#### Scenario: Server state is not duplicated in ad-hoc state

- **WHEN** server-derived data is needed by the UI
- **THEN** it is sourced from the TanStack Query cache rather than being copied into unmanaged local/global state

#### Scenario: Completed event refreshes dependent reads

- **WHEN** a player event is completed successfully
- **THEN** the current session's profile and career-record reads SHALL be invalidated so bankroll, rank, and last result can refresh from the committed career state

#### Scenario: Week advancement refreshes the profile

- **WHEN** a career week advances successfully
- **THEN** the current session's profile read SHALL be invalidated without invalidating another session's profile
