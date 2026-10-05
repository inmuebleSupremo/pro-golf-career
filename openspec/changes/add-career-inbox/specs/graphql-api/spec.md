## ADDED Requirements

### Requirement: Career Inbox Query

The GraphQL API SHALL expose an additive, read-only `careerInbox(id)` query scoped to the authenticated owner of the session. It SHALL return an application `CareerInbox` DTO containing zero or more `CareerInboxItem` DTOs. Each item SHALL expose a typed `kind` and an integer `count`; the supported MVP kinds SHALL be `SCHEDULE`, `DEVELOPMENT`, `SPONSORSHIP`, `EQUIPMENT`, and `STAFF`. The query SHALL NOT mutate state or expose a simulation-engine type.

#### Scenario: A player reads their current Inbox

- **WHEN** an authenticated player queries `careerInbox` for their loaded session
- **THEN** the API SHALL return the current qualifying Inbox items as typed DTOs with their source counts

#### Scenario: An Inbox read does not change the career

- **WHEN** a client queries `careerInbox`
- **THEN** no offer, decision, calendar state, player state, or Inbox-specific state SHALL change

#### Scenario: Inbox respects session ownership

- **WHEN** a player queries `careerInbox` for a session they do not own
- **THEN** the query SHALL enforce the same authenticated ownership boundary as other world-session reads

#### Scenario: No engine type leaks

- **WHEN** the GraphQL schema is inspected for the Inbox query
- **THEN** its return types SHALL be application DTOs rather than simulation-engine records

### Requirement: Narrow Review Acknowledgements

The GraphQL API SHALL expose authenticated `acknowledgeScheduleReview(id)` and `acknowledgeStaffReview(id)` mutations solely for their respective career screens to record completion of the current season's review. Each SHALL be owner-scoped, SHALL NOT accept or return an Inbox item identifier, and SHALL NOT change simulation state or any other Inbox source. Each MAY record only the current season number in application save/session state.

#### Scenario: Leaving Schedule records only its seasonal review

- **WHEN** the Schedule client leaves the Schedule section during an eligible week-1 review
- **THEN** the API SHALL record the current season as Schedule-reviewed
- **AND** a subsequent `careerInbox` read SHALL omit only the Schedule item when its remaining conditions are unchanged

#### Scenario: Landing on Staff records only its seasonal review

- **WHEN** the Staff client successfully loads the Staff landing during an eligible review
- **THEN** the API SHALL record the current season as Staff-reviewed
- **AND** a subsequent `careerInbox` read SHALL omit only the Staff item when its remaining conditions are unchanged

#### Scenario: Acknowledgement does not become a general Inbox mutation

- **WHEN** a client uses either review acknowledgement
- **THEN** it SHALL NOT dismiss, archive, read, or otherwise change an Inbox item from a different source
