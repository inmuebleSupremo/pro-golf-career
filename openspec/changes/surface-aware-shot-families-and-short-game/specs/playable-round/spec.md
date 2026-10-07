## ADDED Requirements

### Requirement: Shared human and AI family intent
Human and AI observable play SHALL submit the same club, AimPoint, and ShotFamily intent contract. AI policy SHALL choose a family before execution and that intent SHALL feed the same compatibility adapter used by human play.

#### Scenario: AI bunker extraction shares semantics
- **WHEN** AI starts a shot in a bunker
- **THEN** it SHALL emit compatible BUNKER family intent
- **AND THEN** it SHALL resolve through the same eligibility, execution, settlement, and trace path as a human bunker intent.

### Requirement: Deliberate fringe and protected green routing
Green shots SHALL retain the dedicated minimal `PuttIntent` route. Fringe shots SHALL no longer become puts solely because they are within a fixed distance; eligible deliberate `PuttIntent` or ball-strike choice SHALL control routing.

#### Scenario: Fringe chip remains a ball strike
- **WHEN** a player selects an eligible CHIP from fringe
- **THEN** the shot SHALL use ball-strike execution and any authoritative ground response
- **AND THEN** it SHALL not be silently rerouted to the green putting model.

### Requirement: Background execution parity and allocation
Bulk simulations SHALL resolve the same selected family semantics as visible play while remaining summary-only for trace allocation unless an observable consumer requests a trace.

#### Scenario: Trace mode does not change family result
- **WHEN** identical family intent and deterministic context resolve in summary and trace-materialized modes
- **THEN** contact, final ball, scoring, and settlement SHALL be equal
- **AND THEN** only trace presence may differ.
