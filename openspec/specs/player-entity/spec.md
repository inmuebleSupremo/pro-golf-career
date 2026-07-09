# player-entity Specification

## Purpose
TBD - created by archiving change add-golfer-entities. Update Purpose after archive.
## Requirements
### Requirement: Canonical Player Entity

The system SHALL represent every golfer as a single Player entity with a unique identifier. A Player SHALL belong to exactly one career and SHALL be the single authoritative owner of that golfer's permanent data; other systems SHALL reference the Player rather than duplicate its permanent attributes or identity.

#### Scenario: Player has a unique identity

- **WHEN** a Player is created
- **THEN** it SHALL have a unique identifier that other systems use to reference it

#### Scenario: Permanent data is not duplicated elsewhere

- **WHEN** another system needs a Player's permanent attributes or identity
- **THEN** it SHALL reference the Player, and SHALL NOT store its own copy of that permanent data

### Requirement: Immutable Identity

Every Player SHALL have an Identity comprising first name, last name, nationality, date of birth, and archetype. Identity SHALL be immutable after creation. First and last names SHALL be 1–50 characters and support international characters.

#### Scenario: Identity fields are required and bounded

- **WHEN** a Player is created with a first or last name outside 1–50 characters, or with any identity field missing
- **THEN** creation SHALL be rejected

#### Scenario: Identity does not change after creation

- **WHEN** a change to a created Player's identity is attempted
- **THEN** it SHALL be rejected; identity remains as created

### Requirement: Player Data Categories

Player data SHALL be divided into four categories with distinct rules: Identity (permanent), Attributes (permanent, referenced from the numerical model), Derived Statistics (calculated, never stored), and State (temporary). Derived Statistics SHALL always be recalculated rather than persisted.

#### Scenario: Attributes are referenced, not duplicated

- **WHEN** a Player exposes its permanent skill values
- **THEN** they SHALL come from the referenced `Attributes`, not a duplicated attribute store

#### Scenario: Derived statistics are recalculated

- **WHEN** a derived statistic (e.g. an expected performance value) is requested
- **THEN** it SHALL be computed on demand from current inputs, and SHALL NOT be read from stored state

### Requirement: Fatigue State

Every Player SHALL have a Fatigue value. Fatigue is temporary State that influences performance and recovers over time; it SHALL NOT permanently alter stored Attributes.

#### Scenario: Fatigue never mutates attributes

- **WHEN** a Player's Fatigue changes to any value
- **THEN** the Player's permanent Attributes SHALL remain unchanged

### Requirement: Live Skill Rating

Every Player SHALL have one Live Skill Rating representing current competitive form. It is volatile: it increases after strong performance, decreases after poor performance, and drifts back toward a baseline during inactivity. It SHALL influence performance calculations but SHALL NEVER permanently modify stored Attributes.

#### Scenario: Rating responds to performance

- **WHEN** a strong performance is recorded, then separately a poor one
- **THEN** the rating SHALL move up for the strong result and down for the poor result

#### Scenario: Rating self-corrects toward baseline on inactivity

- **WHEN** a Player is inactive over time
- **THEN** the Live Skill Rating SHALL move toward its baseline

#### Scenario: Rating does not alter attributes

- **WHEN** the Live Skill Rating changes
- **THEN** the Player's permanent Attributes SHALL remain unchanged

### Requirement: Injury State

A Player SHALL have zero or one active Injury at a time. An active Injury SHALL define a type, severity, recovery duration, and gameplay effects. Recovery SHALL progress over time and SHALL NOT permanently alter stored Attributes.

#### Scenario: At most one active injury

- **WHEN** a Player already has an active Injury and another is applied
- **THEN** the system SHALL reject the second (a Player cannot hold two simultaneous active injuries)

#### Scenario: Injury recovers over time

- **WHEN** recovery time is advanced for an injured Player
- **THEN** the remaining recovery duration SHALL decrease, and the Injury SHALL clear when it reaches zero

### Requirement: Career Status State Machine

Every Player SHALL have exactly one Career Status from: CREATED, ACTIVE, INJURED, RETIRED, DECEASED. Only defined transitions SHALL be permitted; invalid transitions SHALL be rejected. RETIRED and DECEASED are terminal for gameplay.

#### Scenario: Valid transition is accepted

- **WHEN** a CREATED Player is activated
- **THEN** the status SHALL become ACTIVE

#### Scenario: Invalid transition is rejected

- **WHEN** a transition not permitted by the state machine is attempted (e.g. RETIRED back to ACTIVE)
- **THEN** it SHALL be rejected and the status SHALL remain unchanged

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

### Requirement: Temporary Equipment Profile

A Player's temporary state SHALL be able to carry a transient equipment profile (forgiveness and power) set before play from the active Golf Bag and surfaced to the shot engine. This profile SHALL be temporary state, not a permanent Attribute, and SHALL NEVER persist into permanent attributes.

#### Scenario: The equipment profile rides temporary state

- **WHEN** the active bag's characteristics are applied to a golfer before play
- **THEN** they SHALL be held as temporary state and surfaced to shot resolution, without changing any permanent Attribute

