# professional-golfer Specification

## Purpose
TBD - created by archiving change add-golfer-entities. Update Purpose after archive.
## Requirements
### Requirement: Professional Golfer Composition

Every competitive golfer SHALL be represented as a Professional Golfer composed of a Player, a Career reference, and a Control Type. The Career SHALL be referenced by identifier only in this capability; the Career entity itself is out of scope here.

#### Scenario: Professional Golfer wraps a Player and a career reference

- **WHEN** a Professional Golfer is created
- **THEN** it SHALL reference exactly one Player and hold a Career reference identifier

### Requirement: Control Type

Every Professional Golfer SHALL have a Control Type of either Human or Simulation. Control Type SHALL be assigned at creation and SHALL be immutable thereafter. Control Type determines who makes decisions; it SHALL NOT alter gameplay rules.

#### Scenario: Control Type is immutable

- **WHEN** a change to a created Professional Golfer's Control Type is attempted
- **THEN** it SHALL be rejected

#### Scenario: Control Type does not change rules

- **WHEN** a Human and a Simulation golfer have identical Players and inputs
- **THEN** the gameplay systems SHALL treat them identically; Control Type SHALL NOT be read as a gameplay-rule input

### Requirement: Shared Rules Across Control Types

Human-controlled and Simulation-controlled golfers SHALL use identical scoring, eligibility, progression, and retirement rules with no hidden advantages based on Control Type.

#### Scenario: No control-type advantage

- **WHEN** two golfers differ only in Control Type
- **THEN** neither SHALL receive any hidden bonus or penalty from that difference

### Requirement: Simulation Decision Seam

Simulation-controlled golfers SHALL make competitive decisions without human input through a defined decision-making seam. This capability defines the seam (interface) only; the decision implementation is out of scope.

#### Scenario: Simulation decisions require no human input

- **WHEN** a decision is requested for a Simulation-controlled golfer
- **THEN** it SHALL be obtainable through the decision seam without any human interaction

#### Scenario: Human golfers are not auto-decided

- **WHEN** a Human-controlled golfer is presented
- **THEN** the system SHALL NOT auto-resolve decisions that belong to the human player

### Requirement: Persistent Identity and Legacy

Every Professional Golfer SHALL maintain a persistent identity for the life of its career, and its historical achievements SHALL be preserved independent of Control Type. Careers continue to exist regardless of whether a golfer competes against the human player.

#### Scenario: Identity persists

- **WHEN** a Professional Golfer is referenced across time
- **THEN** its identity SHALL remain stable and recognisable

#### Scenario: Legacy is control-type independent

- **WHEN** achievements are preserved for a golfer
- **THEN** they SHALL be preserved the same way whether the golfer is Human- or Simulation-controlled

