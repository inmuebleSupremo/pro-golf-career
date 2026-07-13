## ADDED Requirements

### Requirement: Owned-Equipment Read Queries

The API SHALL expose, keyed by session id, the player's owned equipment and current tournament loadout: `playerEquipment` returns every item the player owns across all categories, and `playerLoadout` returns the item currently selected in each category. Both SHALL project to the equipment DTO and SHALL return an empty result when no player is assigned rather than an error.

#### Scenario: Owned equipment is listed

- **WHEN** a client queries the player's equipment for a session with an assigned player
- **THEN** the API SHALL return each owned item as a DTO with its name, category, quality, cost, and characteristics

#### Scenario: The current loadout is read

- **WHEN** a client queries the player's loadout for a session with an assigned player
- **THEN** the API SHALL return the item currently selected in each equipment category

#### Scenario: Reads are empty without a player

- **WHEN** a client queries the player's equipment or loadout for a session with no assigned player
- **THEN** the API SHALL return an empty result and SHALL NOT raise an error

### Requirement: Loadout Selection Mutation

The API SHALL let the player switch the tournament loadout to an already-owned item, addressed by its category and name. The mutation SHALL resolve the named owned item and equip it in that category, confirming success. Naming an item the player does not own SHALL yield a client-error-classified GraphQL error.

#### Scenario: An owned item is equipped

- **WHEN** a client runs the select-loadout mutation with the category and name of an item the player owns
- **THEN** the engine SHALL set that category's loadout slot to the item, and a subsequent loadout query SHALL reflect it

#### Scenario: An unowned item is rejected

- **WHEN** a client runs the select-loadout mutation with a category and name the player does not own
- **THEN** the API SHALL return a bad-request-classified GraphQL error, not an opaque internal error
