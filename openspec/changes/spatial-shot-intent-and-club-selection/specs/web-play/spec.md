## ADDED Requirements

### Requirement: Spatial manual targeting
The play UI SHALL let a human choose an individual club and preview/select a literal canonical aim point on the hole display. It SHALL submit that point, not a derived strategy/distance/lateral decision.

#### Scenario: Player selects a suggested or free point
- **WHEN** a player taps a guidance default or places a custom target
- **THEN** the UI displays the target marker and its relevant distance/reach feedback
- **AND THEN** submission uses the displayed coordinates.

### Requirement: Accessible multi-input targeting
The play UI SHALL support desktop pointer targeting, touch-friendly mobile targeting, and keyboard coarse/fine target adjustment. It SHALL expose target coordinates, selected club, distance, reach/legality messages, and stale-state feedback with accessible labels and non-colour-only indicators.

#### Scenario: Keyboard player adjusts target
- **WHEN** a keyboard user operates target controls
- **THEN** the target moves by documented coarse or fine increments
- **AND THEN** updated position and warnings are announced without relying solely on colour.

### Requirement: Human risk control retirement
The play UI SHALL remove conservative/balanced/aggressive as human shot-selection controls.

#### Scenario: Manual action dock has no strategy input
- **WHEN** a player prepares a manual ball strike
- **THEN** the action controls present club, target, guidance, and submit state
- **AND THEN** no human strategy selector is shown or sent.
