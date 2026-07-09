## ADDED Requirements

### Requirement: Rounds Resolve Under Playing Conditions

Each competitor's round SHALL be resolved under the Tournament's Playing Conditions for that round, applied through the shared round-resolution engine's environment inputs. Playoff holes SHALL use the final round's conditions. A Tournament with no supplied weather SHALL default to calm conditions, preserving standalone-event behaviour.

#### Scenario: A round uses that round's conditions

- **WHEN** a competitor's round is resolved
- **THEN** the round SHALL be resolved under the Tournament's Playing Conditions for that round, applied identically to every competitor of the round

#### Scenario: Calm is the default without weather

- **WHEN** a Tournament is played without supplied weather
- **THEN** every round SHALL resolve under calm conditions, matching prior standalone behaviour
