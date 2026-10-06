## MODIFIED Requirements

### Requirement: Deterministic Generation

A Course SHALL be generated deterministically from a seed obtained through the world seed hierarchy and an explicit supported generator version. Regenerating a Course from the same seed, environment classification, and generator version SHALL produce an identical Course, canonical geometry, and all version-supported design/spatial semantics. Generation SHALL NOT use ambient randomness. A current-version convenience path MAY be used only for new-world creation; restoration SHALL select the recorded version explicitly.

#### Scenario: V3 course regenerates exactly

- **WHEN** a V3 Course is generated twice from the same seed, environment classification, and explicit supported version
- **THEN** its design plan, route/zone/green semantics, progression targets, and canonical geometry SHALL be identical

#### Scenario: Historical generators are isolated

- **WHEN** the V3 generator is added or changed
- **THEN** checked-in V1 and V2 seed fixtures SHALL still produce their exact historical course records and canonical geometry

#### Scenario: Unknown generator version is rejected

- **WHEN** generation or restoration requests an unsupported generator version
- **THEN** it SHALL fail explicitly rather than silently substituting the latest implementation

### Requirement: Course Composition

A design-aware generator version SHALL create its eighteen-hole composition from its deterministic course plan before individual hole geometry is generated. V2 SHALL compile its brief-derived bounded scalar inputs through the existing canonical terrain path. V3 SHALL additionally derive constrained route, landing-zone, and green-complex semantics from each brief before compiling canonical terrain. `CourseGeometry` remains the authoritative surface representation for play in every version.

#### Scenario: V3 planned composition guides spatial holes

- **WHEN** a V3 design-aware course is generated
- **THEN** every generated hole SHALL correspond in order and par to its plan brief, have valid version-three spatial semantics, and retain valid canonical geometry

#### Scenario: Semantics do not replace spatial authority

- **WHEN** any generated hole is resolved during play
- **THEN** surface classification and settlement SHALL use canonical geometry rather than a profile, brief, route, zone, or green-complex plan
