## ADDED Requirements

### Requirement: Canonical ball-strike intent
The simulation SHALL accept immutable `ShotIntent` values. Its first supported variant, `BallStrikeIntent`, SHALL contain a stable individual `ClubId` and an `AimPoint`.

#### Scenario: Intent contains no human risk policy
- **WHEN** a human submits a ball-strike intent
- **THEN** the authoritative input contains the selected club and literal aim point
- **AND THEN** it contains no strategy, target-distance, or target-lateral field.

### Requirement: AimPoint means intended first contact
`AimPoint(x,y)` SHALL be finite absolute canonical-hole yard coordinates for intended carry/first contact from the current ball state. It SHALL NOT mean final resting position.

#### Scenario: Compatibility resolution derives direction
- **WHEN** a valid intent is resolved
- **THEN** direction and requested travel are derived from current ball position to its aim point
- **AND THEN** existing reach caps, dispersion, conditions, and surface settlement determine the outcome.

### Requirement: Individual club catalogue compatibility
The simulation SHALL provide a static stable catalogue of individual clubs, including driver, fairway woods, hybrids, numbered irons, wedges, and putter. Every club SHALL map to an existing equipment family so current aggregate bag/loadout effects remain applicable.

#### Scenario: Club ID survives compatibility use
- **WHEN** a valid catalogue club is selected
- **THEN** the resolver uses its calibrated characteristics plus applicable family equipment effects
- **AND THEN** no new equipment ownership or progression system is required.

### Requirement: Bounded but expressive aim validation
The simulation SHALL reject non-finite points and points outside a server-derived finite aim envelope based on hole boundary plus documented margin. It SHALL permit points within that envelope even when they lie in hazards, trees, out of bounds, or beyond selected-club reach.

#### Scenario: Unreachable hazardous target remains a golf decision
- **WHEN** a player aims a short club at a reachable-coordinate point in water or beyond its reach
- **THEN** the intent is valid
- **AND THEN** existing execution and settlement rules determine the result.

### Requirement: Simplified resolver remains authoritative
This change SHALL preserve the existing simplified resolver beneath intent adaptation. It SHALL NOT add shot types, bounce/roll, flight-terrain intersection, forced carries, directional wind, shape, trajectory control, or a flight trace.

#### Scenario: Intent does not create unimplemented physics
- **WHEN** the resolver processes a ball-strike intent
- **THEN** it produces the same class of existing settlement/outcome data
- **AND THEN** it does not claim an unimplemented path or final-resting target guarantee.
