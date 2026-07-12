## MODIFIED Requirements

### Requirement: Rounds Resolve Under Playing Conditions

Each competitor's round SHALL be resolved under the Tournament's Playing Conditions for that round AND under the Tournament's course setup, both applied through the shared round-resolution engine (the setup scales the holes' pins and effective width and the round's wind severity). Playoff holes SHALL use the final round's conditions and the same setup. A Tournament with no supplied weather SHALL default to calm conditions, and a Tournament with no supplied setup SHALL default to a neutral setup — together preserving standalone-event behaviour. The setup SHALL apply identically to every competitor of the round, and identically on the automatic and interactive playable paths.

#### Scenario: A round uses that round's conditions

- **WHEN** a competitor's round is resolved
- **THEN** the round SHALL be resolved under the Tournament's Playing Conditions and course setup for that round, applied identically to every competitor of the round

#### Scenario: Calm is the default without weather

- **WHEN** a Tournament is played without supplied weather
- **THEN** every round SHALL resolve under calm conditions, matching prior standalone behaviour

#### Scenario: Neutral setup is the default

- **WHEN** a Tournament is played without a supplied course setup
- **THEN** every round SHALL resolve under a neutral setup, matching prior baseline course behaviour
