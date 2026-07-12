## REMOVED Requirements

### Requirement: Injury State

**Reason**: Superseded by the `injury-recovery` capability, which the `sim.health` domain (Health, Fitness & Recovery) implements end-to-end for the live World. The `sim.player` injury scaffold that satisfied this requirement was dead code — a near-exact duplicate of `sim.health.Injury` with no caller in the running simulation.

**Migration**: Injuries are modelled by `sim.health` — `PhysicalState` holds zero-or-one active `Injury` (named `InjuryType`, `InjurySeverity` with rehab weeks), `HealthSystem` rolls and rehabilitates them, and `Availability` (INJURED/RECOVERING) derives the golfer's readiness to compete. No player-entity injury API remains.

## MODIFIED Requirements

### Requirement: Career Status State Machine

Every Player SHALL have exactly one Career Status from: CREATED, ACTIVE, RETIRED, DECEASED. Only defined transitions SHALL be permitted; invalid transitions SHALL be rejected. RETIRED and DECEASED are terminal for gameplay. An injury does NOT change a Player's Career Status — a hurt golfer remains ACTIVE, and the transient inability to compete is expressed by the health domain's `Availability`, not by a career-lifecycle status.

#### Scenario: Valid transition is accepted

- **WHEN** a CREATED Player is activated
- **THEN** the status SHALL become ACTIVE

#### Scenario: Invalid transition is rejected

- **WHEN** a transition not permitted by the state machine is attempted (e.g. RETIRED back to ACTIVE)
- **THEN** it SHALL be rejected and the status SHALL remain unchanged

#### Scenario: Injury does not change career status

- **WHEN** a Player becomes injured
- **THEN** the Player's Career Status SHALL remain ACTIVE, and the injury SHALL be reflected only in the health domain's availability, not as a career-status transition
