## ADDED Requirements

### Requirement: Guidance points are literal spatial suggestions
Strategic routing SHALL expose SAFE, PRIMARY, and AGGRESSIVE guidance as literal canonical `AimPoint` suggestions where applicable. Guidance SHALL remain advisory and SHALL not limit free target selection within the aim envelope.

#### Scenario: Default becomes an ordinary aim point
- **WHEN** a player chooses the SAFE default
- **THEN** the client submits its resolved canonical coordinates as `BallStrikeIntent.aimPoint`
- **AND THEN** the resolver grants it no special execution treatment.

### Requirement: ShotAim is policy and guidance only
`ShotAim` MAY support AI route selection and player guidance, but SHALL NOT be hidden human aiming authority.

#### Scenario: Submitted human point differs from route suggestion
- **WHEN** a human submits a point distinct from the suggested route point
- **THEN** resolution derives direction from the submitted point
- **AND THEN** it does not substitute the route suggestion.
