# course-mastery Specification

## Purpose
TBD - created by archiving change add-course-domain. Update Purpose after archive.
## Requirements
### Requirement: Player-Course Mastery Relationship

Course familiarity SHALL be tracked as a relationship between a specific player and a specific Course, owned by the relationship rather than by the Course. Mastery SHALL be tracked independently for every player.

#### Scenario: Mastery is per player and owned by the relationship

- **WHEN** two different players have each played the same Course
- **THEN** each SHALL have their own independent mastery value, and the Course itself SHALL NOT store player-specific mastery

#### Scenario: Repeated play may increase mastery

- **WHEN** a player plays a Course they have played before
- **THEN** their mastery of that Course MAY increase according to the relationship's rules

### Requirement: Mastery Isolation and Persistence

Mastery SHALL never transfer between Courses and SHALL persist across seasons.

#### Scenario: Mastery does not leak between courses

- **WHEN** a player has high mastery of Course A and first plays Course B
- **THEN** their mastery of Course B SHALL start independent of Course A

#### Scenario: Mastery survives seasonal transitions

- **WHEN** a season concludes and a new season begins
- **THEN** a player's existing course mastery SHALL remain intact

