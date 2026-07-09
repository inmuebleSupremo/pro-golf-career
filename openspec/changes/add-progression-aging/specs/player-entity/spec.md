## ADDED Requirements

### Requirement: Evolvable Attributes

A Player's permanent Attributes SHALL be evolvable through defined progression: they MAY increase through development and decrease through aging or regression. Evolved values SHALL always remain within 0–100. Random tournament performance SHALL NEVER permanently alter Attributes — only development and aging SHALL. Every permanent Attribute change SHALL be recorded.

#### Scenario: Attributes change only through progression

- **WHEN** a Player's Attributes change
- **THEN** the change SHALL originate from development or aging, never from a tournament result or other randomness

#### Scenario: Evolved attributes stay in range

- **WHEN** development or aging would move an attribute outside 0–100
- **THEN** the stored value SHALL be clamped to the bound

#### Scenario: Permanent changes are recorded

- **WHEN** a permanent Attribute change is applied
- **THEN** it SHALL be recorded as part of the golfer's history
