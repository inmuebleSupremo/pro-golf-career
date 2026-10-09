# course-landscape Specification

## ADDED Requirements

### Requirement: V6 generates one deterministic shared course landscape before local holes

V6 SHALL generate one immutable `CourseLandscapePlan` in shared course-yard coordinates before final local-hole
architecture selection. It SHALL contain bounded continuous landscape features and exactly eighteen ordered
`HolePlacement`s with course-space tee origin, heading, invertible local-to-course transform, design envelope and
adjacency facts. Selection SHALL use a fixed seed-derived candidate budget, deterministic tie-breaking and explicit
rejection for overlap, unsafe spacing, invalid transforms, excessive transitions and incompatible relationships.

#### Scenario: Same V6 inputs reproduce course-scale geography

- **WHEN** V6 is generated twice from identical version, seed hierarchy, classification and inputs
- **THEN** its landscape features, placements, transforms and rejection/selection outcome SHALL be identical

### Requirement: V6 uses continuous multi-hole environmental systems

V6 SHALL model coast/bay, woodland mass/clearing, parkland field/copse, links heath/dune, open ground and lake
features as deterministic continuous course-space systems, not cosmetic per-hole water/tree shapes or named maps.
Coastal courses SHALL relate multiple suitable holes to one shared shore/bay; woodland SHALL establish shared
mass/clearing corridors; parkland SHALL establish shared fields/copses; links SHALL remain distinctly open/exposed.

#### Scenario: A coast is shared rather than repeated

- **WHEN** an eligible V6 coastal course is generated
- **THEN** its evidence SHALL identify multiple hole placements related to the same continuous shore/bay feature

### Requirement: V6 has a coherent deterministic placement fallback

If its primary bounded placement selection is exhausted, V6 SHALL select a documented deterministic course-scale
fallback with valid transforms, non-overlapping envelopes and bounded transitions. It SHALL still generate one shared
landscape/placement plan and SHALL NOT fall back to independently positioned holes.

#### Scenario: Candidate exhaustion remains deterministic

- **WHEN** the primary placement candidate budget is exhausted
- **THEN** V6 SHALL emit its stable fallback identity and a valid coherent placement plan
