## MODIFIED Requirements

### Requirement: Tournament Definition

A Tournament SHALL define a name, a Course, a tier, an **event prestige**, entry requirements, a prize structure, a competition format, and a scheduled date. Once play begins, the Tournament definition SHALL be immutable.

#### Scenario: Tournament exposes its required definition

- **WHEN** a Tournament is created
- **THEN** it SHALL expose a name, Course, tier, event prestige, entry requirements, prize structure, format, and scheduled date

#### Scenario: Prestige defaults to Regular when unspecified

- **WHEN** a Tournament definition is created without an explicit prestige
- **THEN** its prestige SHALL be Regular

#### Scenario: Definition is immutable once play begins

- **WHEN** a change to the definition is attempted after Round 1 has begun
- **THEN** it SHALL be rejected
