# course-persistence Specification

## Purpose
TBD - created by archiving change add-course-domain. Update Purpose after archive.
## Requirements
### Requirement: Courses Are Permanent Historical Assets

A Course generated during a career SHALL become a permanent historical asset. Historical tournament results SHALL always reference the exact Course used, and that Course SHALL remain reproducible.

#### Scenario: Historical result references the exact course

- **WHEN** a completed tournament's history is examined
- **THEN** it SHALL reference the exact Course used, and that Course SHALL be retrievable in the form it was played

#### Scenario: Course remains reproducible

- **WHEN** a historical Course is reconstructed from its seed and generator version
- **THEN** it SHALL match the Course as originally generated

### Requirement: Course Revisions Do Not Invalidate History

Introducing new Courses or generator changes SHALL NOT alter or invalidate Courses already recorded in history.

#### Scenario: New generation does not rewrite old courses

- **WHEN** the course generator is extended or new Courses are created
- **THEN** previously recorded historical Courses SHALL remain unchanged and their tournaments SHALL stay reproducible

### Requirement: Persistence Carries No Tournament State

The persisted Course SHALL describe only the environment. It SHALL NOT carry tournament-specific data.

#### Scenario: Persisted course excludes competitive data

- **WHEN** a Course is persisted and later restored
- **THEN** it SHALL contain no leaderboards, prize money, rankings, competitors, or round scores

