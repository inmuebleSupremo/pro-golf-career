## ADDED Requirements

### Requirement: Player handedness has narrow shape semantics
Each player SHALL have immutable `Handedness` identity of `RIGHT` or `LEFT`. It SHALL influence only the sign convention used to interpret FADE and DRAW airborne curvature relative to the aim frame. It SHALL NOT change attributes, progression, equipment, strategy, ordinary shot eligibility, or any non-shape gameplay rule.

#### Scenario: Handedness inverts the FADE and DRAW bulge convention
- **WHEN** a right-handed player resolves an eligible DRAW ball strike
- **THEN** its authoritative airborne path SHALL first bulge golfer-right and then return golfer-left toward its intended endpoint relative to the origin-to-aim frame.
- **AND WHEN** that player resolves an eligible FADE ball strike
- **THEN** its authoritative airborne path SHALL first bulge golfer-left and then return golfer-right toward its intended endpoint.
- **AND WHEN** a left-handed player resolves those eligible shapes
- **THEN** the corresponding bulge and return directions SHALL be the exact inverse of the right-handed convention.
