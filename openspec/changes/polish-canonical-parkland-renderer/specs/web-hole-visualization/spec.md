## MODIFIED Requirements

### Requirement: Faithful Representation of Mechanical Facts

The 2D layer SHALL be a truthful representation of the simulation and SHALL NOT contradict any resolved
mechanical fact. The hole's effective canonical terrain for the active event setup, active pin/cup position,
and each resolved shot's contact, final playable ball state, distance remaining, and penalty SHALL be
represented literally. The client SHALL NOT synthesize which flank a gameplay hazard occupies, dogleg lean,
hazard longitudinal position, fairway-width variation, or any other gameplay terrain. Presentation details not
represented by canonical geometry — vegetation scatter, texture, lighting, organic edge treatment, and camera
treatment — MAY be synthesized only when they cannot imply or obscure a different gameplay surface. Such
decoration SHALL be clipped to its owning canonical surface; vegetation that could read as an obstacle SHALL be
limited to authoritative tree terrain.

#### Scenario: Parkland canopies remain inside tree terrain

- **WHEN** a parkland hole has one or more canonical `TREES` regions
- **THEN** the renderer SHALL derive any deciduous canopy decoration deterministically from the hole seed
- **AND THEN** it SHALL contain the visible canopy decoration within those supplied tree polygons
- **AND THEN** it SHALL not create canopy decoration in a fairway, green, bunker, water, or other terrain region

#### Scenario: Material treatment does not change a hazard boundary

- **WHEN** the renderer applies mowing, grass, sand, or water presentation to a canonical surface
- **THEN** the corresponding visible surface boundary SHALL remain the supplied canonical polygon
- **AND THEN** the renderer SHALL not imply a new carry, obstacle, lie, or penalty

### Requirement: Stable, Deterministic Hole Appearance

A given hole's gameplay landforms SHALL look the same every time it is rendered because they derive from
immutable canonical geometry, with only the permitted per-round cup position changing. Cosmetic decoration MAY
be derived deterministically from a stable per-hole decoration seed so a course's holes read as permanent,
designed venues; it SHALL not create or reshape a gameplay landform. Deterministic decoration that uses a
canonical region SHALL also incorporate that region's supplied vertices so a changed region cannot retain a
stale visual placement.

#### Scenario: Parkland treatment is reproducible

- **WHEN** the same parkland hole is rendered in two separate sessions with the same layout seed and canonical
  geometry
- **THEN** its material treatment and permitted canopy placements SHALL be identical

#### Scenario: Different canonical tree geometry changes decoration safely

- **WHEN** two otherwise equivalent holes supply different `TREES` polygons
- **THEN** the renderer SHALL derive decoration from the supplied polygons for each hole
- **AND THEN** neither render SHALL show tree decoration outside its own tree terrain
