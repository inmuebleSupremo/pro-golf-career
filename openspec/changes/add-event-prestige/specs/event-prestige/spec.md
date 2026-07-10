## ADDED Requirements

### Requirement: Event Prestige Levels

Every tournament SHALL carry an event prestige — one of Regular, Signature, or Major — distinct from and orthogonal to its tour tier. Prestige SHALL weight the event's rewards (ranking points and prize money) and its career significance, with Major weighted above Signature above Regular for every reward it affects. Prestige SHALL NOT affect how shots or scores are resolved.

#### Scenario: Prestige is independent of tour tier

- **WHEN** an event is defined
- **THEN** it SHALL have both a tour tier and an event prestige, and either may vary without the other

#### Scenario: Higher prestige means greater rewards, monotonically

- **WHEN** two events of the same tour tier and field strength are compared
- **THEN** the higher-prestige event SHALL award more ranking points and a larger purse for the same finishing position

#### Scenario: Prestige does not change play

- **WHEN** the same field, course, conditions, and seed are resolved at different prestige levels
- **THEN** every competitor's scores SHALL be identical; only the rewards derived from the result SHALL differ

### Requirement: Majors Are Cross-Tour Marquee Events

A Major SHALL draw its field from the strongest active golfers across all tour tiers, rather than from a single tour's members, and SHALL carry the greatest ranking-point weight, the largest purse, and the greatest career significance of any event. The selection of the strongest field SHALL be deterministic.

#### Scenario: A major's field spans the tours

- **WHEN** a major is contested
- **THEN** its field SHALL be drawn from the strongest eligible active golfers across all tiers, not restricted to one tour's membership

#### Scenario: A major is the biggest news

- **WHEN** a golfer wins a major
- **THEN** the victory SHALL be reported as a distinct, high-prominence event in the world narrative

#### Scenario: A golfer plays at most one event per week

- **WHEN** a major and a tour event fall in the same week and a golfer qualifies for the major
- **THEN** that golfer SHALL be entered only in the major and SHALL NOT also be drawn into the concurrent tour event
