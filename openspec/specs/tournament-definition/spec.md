# tournament-definition Specification

## Purpose
TBD - created by archiving change add-tournament-engine. Update Purpose after archive.
## Requirements
### Requirement: Tournament Definition

A Tournament SHALL define a name, a Course, a tier, entry requirements, a prize structure, a competition format, and a scheduled date. Once play begins, the Tournament definition SHALL be immutable.

#### Scenario: Tournament exposes its required definition

- **WHEN** a Tournament is created
- **THEN** it SHALL expose a name, Course, tier, entry requirements, prize structure, format, and scheduled date

#### Scenario: Definition is immutable once play begins

- **WHEN** a change to the definition is attempted after Round 1 has begun
- **THEN** it SHALL be rejected

### Requirement: Tournament Entry

A Tournament Entry SHALL represent one golfer competing in one Tournament. A Player SHALL have at most one Entry per Tournament; duplicate entries SHALL be rejected.

#### Scenario: One entry per player

- **WHEN** a Player who already has an Entry attempts to enter the same Tournament again
- **THEN** the second entry SHALL be rejected

#### Scenario: Entry references a player

- **WHEN** a Tournament Entry is created
- **THEN** it SHALL reference exactly one Player and belong to exactly one Tournament

### Requirement: Registration and Eligibility

Registration SHALL evaluate eligibility (including the Tournament's tier/entry requirements) and SHALL create a Tournament Entry only when all requirements are met. A failed registration SHALL provide a clear reason.

#### Scenario: Eligible registration succeeds

- **WHEN** an eligible Player registers for an open Tournament
- **THEN** a Tournament Entry SHALL be created

#### Scenario: Ineligible registration fails with a reason

- **WHEN** an ineligible Player attempts to register
- **THEN** registration SHALL fail and SHALL provide a clear reason for the failure

### Requirement: Confirmed Field

A Tournament SHALL maintain a confirmed Field consisting of all accepted Entries. Once Round 1 begins, the Field SHALL be fixed — no new Entries SHALL be accepted.

#### Scenario: Field fixed at Round 1

- **WHEN** Round 1 has begun and a new registration is attempted
- **THEN** it SHALL be rejected and the Field SHALL remain unchanged

#### Scenario: Field is historically reproducible

- **WHEN** the same seed and registrations are used
- **THEN** the confirmed Field SHALL be identical

