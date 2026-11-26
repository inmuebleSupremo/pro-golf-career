## ADDED Requirements

### Requirement: Player Rest Decision Read-Back

The GraphQL API SHALL expose an authenticated, owner-scoped read of the player's standing blanket rest choice from the current world session. The read SHALL return null when no player is assigned and SHALL not derive the choice from physical availability or per-event entry.

#### Scenario: Read after changing rest

- **WHEN** the owner sets or clears blanket rest and reads the decision
- **THEN** the read SHALL return the current choice without changing it

#### Scenario: Read a reopened career

- **WHEN** the owner opens a save containing a blanket rest choice
- **THEN** the read SHALL return the restored choice
