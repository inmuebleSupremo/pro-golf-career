## MODIFIED Requirements

### Requirement: Event Course Setup

Every competitive event SHALL have a course setup that scales the difficulty of the course as it is played, expressed as at least three factors: **pin aggression** (how tucked laterally and how deep the flags are cut), **wind scale** (the severity of the event's wind/exposure), and **width scale** (the effective playing width of greens and fairways). A neutral setup (all factors at their identity value) SHALL reproduce the course's baseline difficulty and canonical geometry exactly. A more difficult setup SHALL tuck pins more aggressively, play windier, and narrow the effective greens and fairways; an easier setup SHALL do the reverse.

For any active round or playoff, the setup-specific canonical geometry used to classify shot contact and settlement SHALL be the effective geometry of that hole. Any presentation or API projection of that playable hole SHALL use the same effective geometry, not its unscaled base geometry.

#### Scenario: A harder setup raises scoring

- **WHEN** the same field plays the same course under a harder setup versus a neutral one
- **THEN** the field's scoring average SHALL be higher (more over par) under the harder setup

#### Scenario: Neutral setup is baseline

- **WHEN** a hole is resolved under a neutral course setup
- **THEN** the outcome and its canonical geometry SHALL match the course's baseline behaviour

#### Scenario: Setup geometry is shared by resolution and presentation

- **WHEN** a player event uses a non-neutral width setup
- **THEN** a position's surface in the projected hole geometry SHALL equal the surface used by gameplay resolution for that position
