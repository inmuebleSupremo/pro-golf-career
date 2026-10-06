## MODIFIED Requirements

### Requirement: Deterministic Generation

A Course SHALL be generated deterministically from a seed obtained through the world seed hierarchy and an explicit supported generator version. Regenerating a Course from the same seed, environment classification, and generator version SHALL produce an identical Course, including its design profile/plan when that version supports them. Generation SHALL NOT use any ambient randomness. A current-version convenience path MAY be used only for new-world creation; restoration SHALL select the recorded version explicitly.

#### Scenario: Same seed and version reproduce the same course

- **WHEN** a Course is generated twice from the same seed, environment classification, and supported generator version
- **THEN** the two Courses, their canonical geometry, and their design profile/plan where applicable SHALL be identical

#### Scenario: Historical version remains selectable

- **WHEN** a supported historical generator version is requested
- **THEN** generation SHALL use that retained implementation rather than the current generator implementation

#### Scenario: Historical seed fixture remains exact

- **WHEN** a retained V1 implementation is run for a checked-in historical seed/classification fixture
- **THEN** its complete course record and canonical geometry SHALL exactly equal that fixture, independently of the current V2 implementation

#### Scenario: Unknown version is rejected

- **WHEN** generation or restoration requests an unsupported generator version
- **THEN** it SHALL fail explicitly rather than silently substituting the latest implementation

### Requirement: Course Composition

A design-aware generator version SHALL create its eighteen-hole composition from its deterministic course plan before individual hole geometry is generated. The plan's brief-derived bounded inputs SHALL guide existing scalar/terrain compilation, while `CourseGeometry` remains the authoritative surface representation for play.

#### Scenario: Planned composition guides generated holes

- **WHEN** a design-aware course is generated
- **THEN** every generated hole SHALL correspond in order and par to its course-plan brief and retain valid canonical geometry

#### Scenario: Design does not replace spatial authority

- **WHEN** a generated hole is resolved during play
- **THEN** surface classification and settlement SHALL continue to use canonical geometry rather than a design profile, plan, or brief
