## ADDED Requirements

### Requirement: Tournament Pin-Policy Immutability

Each tournament SHALL carry one explicit pin-placement version from scheduling through completion. Automatic field resolution, interactive rounds, and playoff holes SHALL derive pins using that same pinned version. Changing a world's future-event default SHALL not alter a tournament that has started or completed.

#### Scenario: Human and AI use the same V5 pin

- **WHEN** the human player and an AI competitor resolve the same hole and round of a V5 tournament
- **THEN** each SHALL use the identical active cup and effective canonical geometry for that round

#### Scenario: Started event ignores later adoption

- **WHEN** a V5 adoption is requested after a tournament has started
- **THEN** the active tournament SHALL retain its existing pinned policy and cup sequence
