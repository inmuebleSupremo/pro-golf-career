## ADDED Requirements

### Requirement: Truthful semantic shot trace
For a trace-materialized canonical shot, the simulation SHALL produce an immutable `ShotTrace` containing the executing `ClubId`, pre-shot origin, exact intended `AimPoint`, actual canonical `ShotContact`, optional settlement transition, and final legal point. The trace SHALL contain only values the current resolver and settlement actually calculate.

#### Scenario: Normal contact has no recovery transition
- **WHEN** a canonical shot contacts a playable ordinary surface
- **THEN** trace origin SHALL equal the pre-shot `BallState.position`
- **AND THEN** trace contact SHALL equal the resolved contact
- **AND THEN** trace transition SHALL be absent and final point SHALL equal the settlement ball position.

### Requirement: Trace derives legal state from settlement
`ShotSettlement` SHALL remain the authority for recovery and final playable `BallState`. A trace SHALL derive its transition and final point from settlement and SHALL NOT reimplement penalty, drop, or replay logic. `ShotOutcome.finalSurface` SHALL NOT be treated as final playable location authority.

#### Scenario: Water contact preserves both facts
- **WHEN** a shot contacts water and settlement grants a legal drop
- **THEN** trace contact SHALL retain the water position and WATER surface
- **AND THEN** trace SHALL contain a `WATER_DROP` non-flight transition to the settlement recovery position
- **AND THEN** trace final point SHALL equal the settlement ball position.

#### Scenario: Replay recovery remains non-flight
- **WHEN** water relief falls back to stroke-and-distance or contact is out of bounds
- **THEN** trace SHALL preserve the actual contact position
- **AND THEN** it SHALL contain the applicable fallback or out-of-bounds replay transition to the legal replay point
- **AND THEN** it SHALL not represent recovery as ball flight.

### Requirement: Trace materialization is observational
The resolver SHALL support trace materialization without changing sampling, geometry lookup, settlement, score, or deterministic random consumption. Summary-only resolution SHALL remain available for non-observable consumers.

#### Scenario: Trace mode preserves resolved result
- **WHEN** identical canonical context and seed are resolved once with trace materialization and once summary-only
- **THEN** carry, lateral, contact, settlement, penalties, score, and final ball state SHALL be equal
- **AND THEN** only the trace presence may differ.

### Requirement: No speculative trajectory physics
The first trace SHALL NOT model or claim apex, curve, launch velocity, spin, bounce, rollout, terrain crossing, time-of-flight, directional wind response, forced carry, tree collision, shot shape, or trajectory control.

#### Scenario: Trace does not turn a contact chord into physics
- **WHEN** a resolver emits a trace
- **THEN** it SHALL expose semantic endpoints and any rules transition only
- **AND THEN** it SHALL not expose uncomputed flight samples or physical events.
