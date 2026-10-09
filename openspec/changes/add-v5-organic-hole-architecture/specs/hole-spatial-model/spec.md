# hole-spatial-model Specification

## ADDED Requirements

### Requirement: V5 architecture metadata cannot replace canonical terrain

V5 route, width-station, landing, approach, green-footprint and candidate-quality records SHALL be immutable
generated metadata. They MAY guide deterministic generation, diagnostics and current target selection, but surface
lookup, penalties, recovery, settlement and rendering SHALL remain exclusively governed by effective canonical
`CourseGeometry`.

#### Scenario: V5 semantic preference loses to canonical surface

- **WHEN** a V5 landing or approach metadata point overlaps a canonical non-fairway surface under an effective
  event setup
- **THEN** target validation and shot settlement SHALL use that canonical surface rather than the metadata label

### Requirement: V5 quality review renders authoritative geometry

The V5 quality gallery/contact sheet SHALL derive every gameplay landform from the same canonical geometry used by
the generator and renderer. It MAY annotate plan/metric facts for review, but SHALL NOT synthesize, move or reshape
terrain from those facts.

The gallery and quality report SHALL expose the complete-course routing-form mix and actual compiled bunker/tree
coverage so sparse feature planning cannot be mistaken for richer canonical landform output.

It SHALL additionally distinguish in-bound from outside tree regions, meaningful fairway-adjacent/wooded-corridor
coverage, bunker role/area/grouping, and actual shoreline-adjacent water from aggregate region counts.

#### Scenario: Contact sheet exposes compiled terrain

- **WHEN** a V5 course contact sheet is generated
- **THEN** each mini-map's tee, green, playable boundary and terrain regions SHALL correspond to the selected
  hole's canonical geometry
