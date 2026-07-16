# web-player-profile Specification

## Purpose
TBD - created by archiving change add-player-profile. Update Purpose after archive.
## Requirements
### Requirement: Golfer header on the career hub

The career hub SHALL show a header identifying the player's golfer — at least name, nationality, age, archetype, world ranking, and career earnings — so the career is visibly the player's own.

#### Scenario: Header shows the golfer

- **WHEN** the career hub loads for a session with a player
- **THEN** the golfer's name and key profile facts (nationality, age, archetype, world ranking, career earnings) are displayed

#### Scenario: Attribute spread

- **WHEN** the golfer header is shown
- **THEN** the golfer's attributes are presented so the player can see their build

### Requirement: Highlight the player on the leaderboard

The play leaderboard SHALL visually distinguish the player's own row from the rest of the field.

#### Scenario: Player row is highlighted

- **WHEN** the play leaderboard is shown and the player's golfer appears in it
- **THEN** the player's row is visually distinguished from the other golfers

