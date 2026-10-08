## ADDED Requirements

### Requirement: GraphQL exposes additive shape and airborne trace contracts
The GraphQL API SHALL accept an additive `shotShape` on `BallStrikeIntentInput`, project server-derived shape availability in shot guidance, and expose an optional airborne path on `ShotTrace`. Each airborne point SHALL contain normalized progress, canonical position, and derived height. Schema types SHALL remain application DTOs and the API SHALL not expose a client-authored trajectory or simulation-engine record.

#### Scenario: Legacy-shaped input remains compatible
- **WHEN** an existing client omits `shotShape` from a valid ball-strike input during the compatibility period
- **THEN** the API boundary SHALL submit `STRAIGHT`
- **AND THEN** the engine SHALL receive a complete intent.

#### Scenario: Trace path preserves contact identity
- **WHEN** an observable ball strike returns an airborne path
- **THEN** the final returned airborne point position SHALL equal `trace.contact.position`
- **AND THEN** its progress and height SHALL be respectively `1` and `0`.

### Requirement: GraphQL player handedness is additive and compatible
The GraphQL golfer-creation input SHALL accept nullable handedness and default an omitted value to `RIGHT`. The player-profile read model SHALL expose resolved handedness. Both values SHALL be application DTO/schema fields; handedness SHALL have no GraphQL control surface beyond player identity creation and read projection.

#### Scenario: Legacy creation caller defaults safely
- **WHEN** an existing creation caller omits handedness
- **THEN** the API SHALL create a `RIGHT`-handed player
- **AND THEN** existing caller inputs and response semantics SHALL remain compatible.

### Requirement: GraphQL exposes read-only effective wind observation
The GraphQL play read model SHALL expose the active hole's backend-authored effective canonical wind-flow vector and
its magnitude. Direction SHALL mean where the wind flows toward in canonical hole coordinates; strength SHALL use
the documented effective resolver wind units. This projection SHALL be informational only and SHALL not accept
client-authored wind, drift, carry, flight, contact, or settlement values.

#### Scenario: Wind projection is signed and non-geographic
- **WHEN** the active hole has effective wind flow
- **THEN** GraphQL SHALL return its signed canonical components and magnitude
- **AND THEN** it SHALL not label the direction as geographic north, a compass heading, or an authoritative flight prediction.
