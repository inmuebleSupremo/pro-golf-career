## ADDED Requirements

### Requirement: Canonical Playing-Geometry Read Model

The GraphQL API SHALL expose an authenticated, session-scoped read model for the current playable hole's canonical geometry and spatial state. The MVP SHALL add `PlayingHole.geometry` with `tee`, active `cup`, `playableBoundary`, and ordered `regions { surface, boundary }`; `PlayingHole.ball { position, lie }`; and `ShotOutcome.settlement { contact { position, surface }, recoveryPosition, recoveryKind, ball }`. Points SHALL be finite local-yard `{ x, y }` coordinates and all boundary/region lists SHALL use the canonical non-repeated, counter-clockwise polygon order. Existing coarse `PlayingHole` fields MAY remain as overview compatibility fields but SHALL NOT be a terrain-rendering source. It SHALL not expose simulation-engine records, SVG markup, rendering instructions, a path/corridor encoding, or a terrain-mutation API.

#### Scenario: Current geometry is projected faithfully

- **WHEN** a player queries the current playable hole during a pending event
- **THEN** the response SHALL contain DTO geometry and spatial state that match the engine's canonical terrain and current playable ball state

#### Scenario: Geometry is absent off-event

- **WHEN** a client queries canonical playing geometry without a pending playable event
- **THEN** the API SHALL return an absent result according to the existing playable-event read convention and SHALL NOT fabricate terrain

#### Scenario: Engine types remain isolated

- **WHEN** the GraphQL schema and resolver signatures are inspected
- **THEN** canonical geometry and ball state SHALL be represented only by application DTOs, not simulation-engine types
