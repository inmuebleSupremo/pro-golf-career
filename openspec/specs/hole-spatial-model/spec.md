# hole-spatial-model Specification

## Purpose
TBD - created by archiving change add-shot-resolution-core. Update Purpose after archive.
## Requirements
### Requirement: Hybrid Spatial Representation

A hole SHALL be authored and presentable in two dimensions but SHALL be *resolved* in one dimension. Shot resolution SHALL operate on a distance-to-pin value plus a lateral offset, and SHALL NOT require full 2D geometry queries. The 2D layout is for presentation and authoring only; it SHALL NOT be an input the shot engine depends upon.

#### Scenario: Resolution needs no 2D geometry

- **WHEN** a shot is resolved
- **THEN** the engine SHALL require only the 1D inputs (distance-to-pin, applicable zone bands, and lateral dispersion parameters) and SHALL NOT query 2D coordinates

#### Scenario: 2D layout is presentation-only

- **WHEN** the 2D layout of a hole changes without changing its 1D zone-band definitions
- **THEN** resolved shot outcomes SHALL be unaffected

### Requirement: Zone-Band Abstraction

A **zone band** SHALL be the first-class contract between course generation and shot resolution. For a given shot context, the reachable landing area SHALL be described as an ordered set of bands along the shot line, each band declaring a distance interval, a lateral extent, and the surface (or hazard) that occupies it with an associated weight. Course generation produces zone bands; the shot engine consumes them and SHALL NOT infer surfaces by any other means.

#### Scenario: Bands fully partition the reachable line

- **WHEN** zone bands are defined for a shot context
- **THEN** the bands SHALL cover the reachable distance range without unresolved gaps, so that every possible sampled landing maps to exactly one surface

#### Scenario: Surface determined solely by bands

- **WHEN** a landing distance and lateral offset are sampled
- **THEN** the resulting surface SHALL be read from the matching zone band, and from no other source

#### Scenario: Generation/resolution seam is explicit

- **WHEN** a course is generated
- **THEN** it SHALL emit zone-band definitions as its output contract, and the shot engine SHALL depend only on that contract, not on the generator's internal representation

### Requirement: Surface Catalogue

Every zone band SHALL reference exactly one surface from the defined Version 1 catalogue: Tee Box, Fairway, First Cut, Primary Rough, Deep Rough, Green, Fringe, Bunker, Waste Area, Recovery Area, Trees, Water, Out of Bounds. Each surface SHALL define its influence on subsequent play (playability, expected penalty, recovery difficulty) consistently across the simulation.

#### Scenario: Unknown surface rejected

- **WHEN** a zone band references a surface not in the catalogue
- **THEN** the band SHALL be rejected as invalid

#### Scenario: Hazard surfaces carry consequences

- **WHEN** a shot lands in a hazard surface band (e.g., Water, Out of Bounds)
- **THEN** the outcome SHALL reflect that surface's defined penalty and next-shot consequences consistently

### Requirement: Lateral Dispersion Mapping

Lateral offset SHALL be resolved as a signed distance from the intended shot line and mapped onto the lateral extent of the matching distance band to determine the final surface. Directional attributes (e.g., Driving Accuracy, Irons Accuracy) SHALL reduce the spread of the lateral offset distribution; they SHALL NOT bias its center away from the intended target.

#### Scenario: Accuracy narrows spread, not aim

- **WHEN** a directional accuracy attribute is increased with all else held constant
- **THEN** the lateral offset distribution SHALL become narrower around the intended line while remaining centered on it

#### Scenario: Lateral offset selects across-line surface

- **WHEN** a landing falls at a given distance band but its lateral offset exceeds the band's central surface extent
- **THEN** the surface SHALL be taken from the appropriate across-line region of that band (e.g., rough or hazard flanking the fairway)

