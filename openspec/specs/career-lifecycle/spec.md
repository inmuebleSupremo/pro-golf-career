# career-lifecycle Specification

## Purpose
TBD - created by archiving change add-career-lifecycle. Update Purpose after archive.
## Requirements
### Requirement: Career Definition and Start

A Career SHALL represent the complete playable history of a single golfer and SHALL belong to exactly one Player. A Career begins when the golfer is activated and SHALL NOT be transferable to another Player.

#### Scenario: Career begins on activation

- **WHEN** a Career is started for an activated Player at a starting age
- **THEN** the Career SHALL be ACTIVE, own that Player, and record the starting age

#### Scenario: Career is not transferable

- **WHEN** an attempt is made to reassign a Career to a different Player
- **THEN** it SHALL be rejected

### Requirement: Career Age and Timeline

A Career SHALL track Age as whole years. Careers SHALL begin between ages 16 and 22. Age SHALL advance once per completed season and SHALL never decrease.

#### Scenario: Starting age is within range

- **WHEN** a Career is started with an age outside 16–22
- **THEN** it SHALL be rejected

#### Scenario: Age advances by one per season and never decreases

- **WHEN** a season is completed
- **THEN** Age SHALL increase by exactly one, and no operation SHALL ever decrease Age

### Requirement: Mandatory Retirement

A Career SHALL conclude with mandatory retirement at age 65; this SHALL NOT be bypassed. Reaching 65 through seasonal advance SHALL retire the golfer automatically.

#### Scenario: Reaching 65 retires automatically

- **WHEN** seasonal advance brings the Career's Age to 65
- **THEN** the Career SHALL become RETIRED

#### Scenario: Retirement cannot be bypassed

- **WHEN** an attempt is made to continue competing after reaching the mandatory retirement age
- **THEN** it SHALL be rejected

### Requirement: Seasonal Advance

A Career SHALL provide a seasonal-advance operation that advances Age and archives the completed season. Calendar generation is not part of this operation; the Career advances, the World drives cadence.

#### Scenario: Seasonal advance archives and ages

- **WHEN** the seasonal-advance operation is invoked on an active Career below retirement age
- **THEN** the completed season SHALL be archived and Age SHALL advance by one

#### Scenario: Archived seasons remain accessible

- **WHEN** multiple seasons have been advanced
- **THEN** each archived season SHALL remain accessible in order

### Requirement: Runtime States

A Career MAY exist in runtime states (Active, Saved, Loaded, Paused) that affect application execution only. These states SHALL NOT advance or alter gameplay progression.

#### Scenario: Pausing does not progress the simulation

- **WHEN** a Career is paused
- **THEN** no age advance, statistics change, or history entry SHALL occur as a result

### Requirement: Career Completion and Integrity

On retirement a Career SHALL become read-only: no further competitive progression is permitted. Historical records SHALL be append-only, and a completed Career SHALL be reconstructible from its historical records.

#### Scenario: Retired career is read-only

- **WHEN** a competitive update (e.g. recording a tournament) is attempted on a RETIRED Career
- **THEN** it SHALL be rejected and no record SHALL change

#### Scenario: History is append-only

- **WHEN** any historical record exists
- **THEN** it SHALL never be modified or removed; new records are only appended

