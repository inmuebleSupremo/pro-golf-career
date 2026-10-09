# course-generation Specification

## MODIFIED Requirements

### Requirement: Deterministic Generation

Course generation SHALL support retained V1, V2, V3, V4, and V5 implementations selected only through an explicit
generator version. V5 SHALL deterministically select an eighteen-hole architecture composition from constrained
candidate plans before compiling canonical terrain. V1/V2/V3/V4 output and fixtures SHALL remain unchanged. New
worlds SHALL select the configured current version only after adoption, while restored worlds SHALL select their
persisted pin; unsupported versions SHALL fail explicitly.

#### Scenario: Same V5 inputs reproduce selected architecture and terrain

- **WHEN** a V5 course is generated twice from the same seed, classification, design inputs and version
- **THEN** its architecture profile, selected hole plans, canonical terrain and quality signature SHALL be identical

#### Scenario: Historical generator output is isolated

- **WHEN** a V1, V2, V3 or V4 fixture is generated after V5 exists
- **THEN** its historical output SHALL remain exact and SHALL not acquire V5 architecture metadata or terrain

## ADDED Requirements

### Requirement: V5 candidates are selected for coherent variety rather than cloned templates

V5 SHALL derive a bounded deterministic candidate pool per hole brief from independent routing, width, landing,
approach, green and supported-hazard parameters. It SHALL evaluate feasibility and select an eighteen-hole
composition using course-identity, playability, difficulty and structural-repetition criteria. It SHALL NOT select
from a finite catalogue of named/showcase layouts or establish variety solely by rotating/scaling cosmetic shapes.
For a candidate described as positional or risk/reward, its interacting route, landing, approach and existing
surface facts SHALL establish a current supported consequence such as different landing forgiveness, remaining
distance, green access or bunker/rough/water exposure. A straightforward forgiving hole remains a legitimate
deliberate selection; V5 SHALL NOT make every hole artificially dramatic.

V5 SHALL select a bounded course-level routing-form mix of straight, gentle-moving, dogleg and rare double-dogleg
plans. The mix MAY vary with architecture identity and hole suitability, but double-doglegs SHALL be exceptional
rather than an incidental dominant candidate category.

#### Scenario: Similar candidates do not dominate one V5 course

- **WHEN** V5 selects a course from its candidate pool
- **THEN** its quality report SHALL include intra-course structural-distance and repetition evidence
- **AND THEN** candidates that are near-duplicates on normalized routing, fairway, landing, approach and green
  measures SHALL incur the configured deterministic repetition penalty

#### Scenario: Candidate novelty creates a supported decision

- **WHEN** a V5 candidate is selected as a positional or risk/reward hole
- **THEN** its quality facts SHALL identify a material supported landing, remaining-distance, approach-access or
  existing-surface-exposure distinction
- **AND THEN** that distinction SHALL not rely on an unimplemented tree collision, forced carry, airborne obstacle
  interception or ground-hazard traversal

### Requirement: V5 geometry uses only truthful current mechanics

V5 SHALL compile one connected canonical playable corridor, simple terrain regions and only existing endpoint/lie
hazards. It MAY express a preferred landing side or a meaningful dogleg approach through canonical endpoint terrain,
but SHALL NOT represent trees as collision volumes, water as a forced carry, airborne path interception, or
ground-surface traversal penalties until those mechanisms are authoritative.

#### Scenario: A V5 visible obstacle does not invent a rule

- **WHEN** a V5 hole contains water, trees or recovery terrain
- **THEN** its resolved consequences SHALL be limited to the existing canonical surface/settlement rules
- **AND THEN** generator metadata and presentation SHALL not claim an unsupported carry, clearance or collision

### Requirement: V5 landforms compile coherent variable bunker and tree recovery geometry

V5 SHALL derive canonical bunker and tree regions from its selected route, landing widths, approach direction,
green defence, environment and existing recovery facts. Bunkering SHALL vary purposefully between key singles,
separated fairway groups and multi-angle greenside guards rather than compiling a fixed count or only a first
inherited feature. Tree regions SHALL vary between clusters, broken corridors and openings by biome and recovery
character. Both remain current endpoint/lie terrain only.

#### Scenario: A rich green complex remains mechanically truthful

- **WHEN** V5 selects a defended green or constrained approach
- **THEN** its canonical bunker complex MAY contain several irregular simple bunker regions around supported
  approach/green positions
- **AND THEN** it SHALL not infer a forced carry, collision or unimplemented hazard-crossing rule

### Requirement: V5 environmental identity is composed from canonical course architecture

V5 SHALL derive deterministic, course-level environmental character from the eighteen selected holes, environment
and architectural tendencies. Parkland and woodland tree regions SHALL be placed inside usable course geometry where
their character calls for them; woodland SHALL be materially more enclosed than parkland, while links remain open.
Coastal and coastal-links courses SHALL assign a bounded subset of suitable holes long canonical shoreline exposure
along a route side rather than relying on isolated decorative water polygons. This SHALL NOT create islands,
bridges, disconnected playable land, collision rules or forced-carry claims.

#### Scenario: Coastal exposure is a real canonical relationship

- **WHEN** a V5 coastal-exposure hole is compiled
- **THEN** canonical water geometry SHALL extend materially alongside part of its playable routing corridor
- **AND THEN** its current consequences SHALL remain limited to canonical endpoint/lie settlement
