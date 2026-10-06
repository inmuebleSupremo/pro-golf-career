# strategic-hole-routing Specification

## ADDED Requirements

### Requirement: V4 hazards preserve route-target compatibility

V4 hazard planning SHALL validate the existing deterministic route-progression/default target cores against compiled canonical terrain. PRIMARY and SAFE recommendations SHALL remain hazard-free at their selected target points; AGGRESSIVE recommendations MAY carry bounded surrounding exposure but SHALL not direct existing AI/default-human flow into a hazard. This requirement SHALL NOT add hazard-management AI, free aim, or new player controls.

#### Scenario: Current default play does not target water

- **WHEN** a V4 dogleg or landing-zone route target is selected by the current compatibility bridge
- **THEN** the target position SHALL not resolve to water, bunker, trees, or recovery terrain
