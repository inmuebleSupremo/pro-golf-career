## ADDED Requirements

### Requirement: Ground firmness reaches non-putting resolution
For a round's existing deterministic `PlayingConditions`, the resolver-facing condition contract SHALL include its derived normalized ground firmness in addition to the existing wind and lie-quality facts. Existing weather generation, forecasts, persistence, and wind/lie semantics SHALL remain unchanged.

#### Scenario: Firm and soft conditions retain their weather identity
- **WHEN** otherwise equivalent holes receive deterministic firm and soft `PlayingConditions`
- **THEN** their resolver contexts SHALL retain the corresponding distinct ground-firmness values
- **AND THEN** their wind and lie-quality values SHALL continue to follow the existing weather mapping.

#### Scenario: Compatibility context has documented neutral firmness
- **WHEN** a legacy constructor or focused fixture creates resolver-facing conditions without an explicit firmness value
- **THEN** it SHALL receive the documented neutral compatibility firmness
- **AND THEN** it SHALL not receive non-deterministic or ambient environmental state.

### Requirement: Green speed remains outside non-putting release
Derived green speed SHALL remain available as a playing-condition fact, but it SHALL NOT independently alter non-putting ground release in this milestone. Dedicated `PuttIntent` resolution SHALL retain its existing behaviour.

#### Scenario: Green-speed-only variation does not alter a non-putting release
- **WHEN** otherwise identical non-putting resolver contexts differ only in green speed
- **THEN** their calculated ground response SHALL be equal
- **AND THEN** a putt SHALL continue through its dedicated putting route.
