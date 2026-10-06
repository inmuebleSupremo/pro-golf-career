## MODIFIED Requirements

### Requirement: Faithful Representation of Mechanical Facts

The 2D layer SHALL be a truthful representation of the simulation and SHALL NOT contradict any resolved mechanical fact. The hole's effective canonical terrain for the active event setup, active pin/cup position, and each resolved shot's contact, final playable ball state, distance remaining, and penalty SHALL be represented literally. The client SHALL NOT synthesize which flank a gameplay hazard occupies, dogleg lean, hazard longitudinal position, fairway-width variation, or any other gameplay terrain. Presentation details not represented by canonical geometry — vegetation scatter, texture, lighting, organic edge treatment, and camera treatment — MAY be synthesized only when they cannot imply or obscure a different gameplay surface.

#### Scenario: The pin is drawn at its real position

- **WHEN** the active round's cup has a canonical position
- **THEN** the flag SHALL be drawn at that position

#### Scenario: Setup-specific terrain is drawn truthfully

- **WHEN** an event applies a non-neutral width setup
- **THEN** the 2D view SHALL draw the effective terrain polygons used by shot settlement rather than base-hole terrain polygons

#### Scenario: A water recovery is shown truthfully

- **WHEN** a resolved shot contacts water and settles at a legal recovery position
- **THEN** the animation SHALL show the water contact and the subsequent authoritative recovery position, and the shown penalty and distance remaining SHALL match the resolved outcome

#### Scenario: Decoration never overrides canonical terrain

- **WHEN** a cosmetic layer would visually suggest a surface different from canonical terrain
- **THEN** canonical terrain SHALL win and the cosmetic layer SHALL be suppressed, clipped, or restyled
