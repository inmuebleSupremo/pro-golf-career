## ADDED Requirements

### Requirement: Ability-based field entry on equal standings

A tournament field SHALL be drawn from its tier's members ordered by season standings, and where season-standings points are equal, golfers SHALL be ordered by attribute-based ability (higher ability first) rather than by an arbitrary identity order, so that talent determines entry before results have separated the field. Ability is derived from a golfer's attributes, not from any live form rating (which starts uniform for all golfers). The ordering SHALL remain fully deterministic.

#### Scenario: A competent golfer enters at season start

- **WHEN** a field is drawn at the start of a season, when all eligible golfers have zero season points
- **THEN** the field is filled by highest attribute ability first, so a golfer of competitive ability for the tier is included rather than excluded by identity order

#### Scenario: Results still take precedence within a season

- **WHEN** some golfers have earned season points and others have not
- **THEN** golfers with more season points are ordered ahead, and ability only breaks ties among golfers with equal points

#### Scenario: Deterministic ordering

- **WHEN** the same world state draws the same field twice
- **THEN** the resulting field and its order are identical

### Requirement: Created golfers start competitive for the entry tier

A newly-created golfer SHALL start with an attribute build strong enough to make entry-tier (Development) fields on merit, so that a player who creates a golfer can begin competing rather than being cut from every field.

#### Scenario: A created golfer reaches a playable event

- **WHEN** a player creates a golfer and advances the calendar
- **THEN** the golfer makes an entry-tier field and a playable event is reached, at full world scale

