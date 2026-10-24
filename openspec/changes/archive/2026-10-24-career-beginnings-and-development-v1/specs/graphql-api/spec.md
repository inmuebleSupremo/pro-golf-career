## ADDED Requirements

### Requirement: Starting Background Creation Input

The authenticated `createPlayer` mutation SHALL accept a Starting Background in addition to existing creation choices. Omission SHALL select Elite Amateur for older clients. Invalid background values SHALL be rejected. The API SHALL pass the choice to deterministic golfer creation without rewriting any existing golfer.

#### Scenario: Older creation client remains compatible

- **WHEN** a client invokes `createPlayer` without Starting Background
- **THEN** creation SHALL succeed with the Elite Amateur initial build and all existing argument meanings preserved

#### Scenario: Explicit choice reaches creation

- **WHEN** a client submits one of the three supported Starting Backgrounds
- **THEN** the resulting new golfer SHALL receive that background's starting ability for its age and Playing Style

### Requirement: Authoritative Development Purchase Read

The authenticated player-development read SHALL expose per-attribute current purchase availability, including next-point cost, remaining potential, remaining current-season purchased allowance, and whether a next point is purchasable or why it is blocked. These values SHALL be projected from the same backend rules used by spending validation and Development Inbox qualification. The GraphQL layer SHALL expose application DTOs, not engine entities.

#### Scenario: Training reads current availability

- **WHEN** a client reads development state after a purchase or season change
- **THEN** it SHALL receive the current bank and per-attribute availability without changing simulation state

#### Scenario: Read and mutation agree

- **WHEN** the read reports a next-point purchase unavailable under unchanged state
- **THEN** a spending mutation attempting that same raise SHALL not succeed
