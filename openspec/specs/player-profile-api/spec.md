# player-profile-api Specification

## Purpose
TBD - created by archiving change add-player-profile. Update Purpose after archive.
## Requirements
### Requirement: Player profile query

The system SHALL expose a query returning the authenticated user's player golfer profile — identity (name, nationality, age, archetype), per-attribute values, current world ranking, career earnings and available funds, current tour, and career statistics (events, wins, top-10s) — or null when the session has no player.

#### Scenario: Profile for a session with a player

- **WHEN** the query is made for a session whose player is assigned
- **THEN** the golfer's id, name, nationality, age, archetype, attributes, world ranking, career earnings, available funds, tour, and career stats are returned

#### Scenario: No player assigned

- **WHEN** the query is made for a session with no player
- **THEN** null is returned rather than an error

#### Scenario: Owner-scoped

- **WHEN** the query is made for a session the user does not own
- **THEN** it is treated as not found (the profile is not disclosed)

#### Scenario: Unranked golfer

- **WHEN** the player has no world ranking yet
- **THEN** the world ranking is absent (null) rather than a fabricated value

