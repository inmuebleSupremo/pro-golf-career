## ADDED Requirements

### Requirement: Setup-Specific Pin Authority

Course setup SHALL affect pin derivation and surface settlement through one identical effective canonical geometry. For a V5 pin, the width-scaled/otherwise setup-specific GREEN polygon used to validate the cup SHALL be the exact geometry returned by the active hole model for contact classification and presentation. Pin aggression may increase the frequency and degree of valid tucked locations, but SHALL never place a cup outside GREEN or inside the 2.0-yard excluded edge region.

#### Scenario: Hard setup remains valid and challenging

- **WHEN** an Elite Major or another harder supported setup derives V5 pins
- **THEN** every cup SHALL satisfy the effective-GREEN invariants
- **AND THEN** the setup SHALL retain more challenging valid pin placement than a regular/easier setup

#### Scenario: Presentation and settlement share cup authority

- **WHEN** a current playable hole is projected under a non-neutral setup
- **THEN** its displayed cup and effective GREEN geometry SHALL be the same values used by authoritative shot settlement
