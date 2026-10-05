## ADDED Requirements

### Requirement: Canonical Yard-Space Geometry

Each generated hole SHALL expose one immutable, canonical local two-dimensional coordinate system measured in yards. The tee SHALL be `(0, 0)`, positive `y` SHALL point from tee toward cup, and positive `x` SHALL be golfer-right while facing the cup. The geometry SHALL include a tee/start point, the active cup position, one closed simple polygon for the playable boundary, and closed simple polygons for bounded terrain sufficient to represent the existing gameplay surfaces: fairway, first cut, primary/deep rough, green, fringe, bunker, waste area, recovery area, trees, water, and out of bounds. Polygon vertex lists SHALL be finite, non-self-intersecting, counter-clockwise, and omit the duplicated closing vertex. Paths or corridors SHALL NOT be part of the persisted or GraphQL MVP contract. The simulation and any presentation consumer SHALL derive gameplay-relevant terrain from this geometry rather than independently synthesizing it.

#### Scenario: A generated hole has a canonical coordinate frame

- **WHEN** a generated hole is inspected
- **THEN** it SHALL provide finite tee and cup coordinates and immutable terrain geometry in its local yard-space

#### Scenario: Presentation does not author gameplay terrain

- **WHEN** a client renders a hole
- **THEN** its fairway, green, bunker, water, out-of-bounds, and other gameplay-relevant landforms SHALL be derived from canonical geometry rather than client-local random placement

#### Scenario: Geometry can be projected without reverse engineering

- **WHEN** a client receives a canonical terrain region
- **THEN** its ordered polygon vertices and surface SHALL be sufficient to draw the corresponding gameplay landform without a local corridor, dogleg, or hazard-placement generator

### Requirement: Deterministic Surface Lookup

Canonical geometry SHALL provide a deterministic `surfaceAt(position)` operation. Lookup SHALL classify every finite position as exactly one catalogue surface and SHALL use a documented fixed precedence for overlapping terrain; incompatible overlaps at the same precedence SHALL be rejected during generation.

#### Scenario: Lookup agrees with a canonical region

- **WHEN** a position lies inside a generated bunker, water, green, or fairway region
- **THEN** `surfaceAt(position)` SHALL return that region's catalogue surface according to the fixed precedence contract

#### Scenario: Outside terrain is out of bounds

- **WHEN** a finite position lies outside the hole's playable boundary and no higher-precedence explicit region contains it
- **THEN** `surfaceAt(position)` SHALL return `OUT_OF_BOUNDS`

### Requirement: Spatial Ball Settlement

The game SHALL retain a playable `BallState` containing the actual canonical position and lie from which the next shot begins. Each resolved shot SHALL produce a settlement that distinguishes sampled physical contact, any penalty/hazard result, any legal recovery/drop position, and the resulting playable ball state.

#### Scenario: A normal shot settles at contact

- **WHEN** a shot contacts a playable non-penalty surface
- **THEN** the resulting playable ball position SHALL equal the contact position and its lie SHALL equal `surfaceAt` at that position

#### Scenario: A water contact becomes a legal drop

- **WHEN** a shot contacts water
- **THEN** the settlement SHALL retain the water contact for reporting and SHALL set a legal playable recovery position whose lie and position determine the next shot

### Requirement: Deterministic Water Relief

Water relief SHALL retain the existing 15-yard setback semantics while making the selected point spatially explicit. From a water contact, the resolver SHALL move toward the pre-shot playable position, begin at 15 yards from contact, and inspect points at 1-yard increments toward that pre-shot position. It SHALL select the first point whose canonical surface is `PRIMARY_ROUGH` and whose cup distance does not exceed the pre-shot cup distance. The generator SHALL provide a legal rough relief corridor for every generated water hazard. If invalid or malformed geometry supplies no qualifying point, the resolver SHALL restore the pre-shot `BallState` as stroke-and-distance, retain the water contact, and identify that fallback in the settlement. The rule SHALL use no ambient randomness.

#### Scenario: Water recovery is reproducible and legal

- **WHEN** the same pre-shot state, water contact, cup, and canonical geometry are resolved twice
- **THEN** both settlements SHALL choose the same `PRIMARY_ROUGH` recovery point and that point SHALL be the next-shot origin

#### Scenario: Missing legal relief falls back safely

- **WHEN** canonical geometry contains no qualifying rough point for a water contact
- **THEN** the settlement SHALL restore the exact pre-shot ball state as stroke-and-distance and SHALL report the fallback recovery kind

#### Scenario: An out-of-bounds contact replays from the prior state

- **WHEN** a shot contacts out of bounds
- **THEN** the settlement SHALL retain the out-of-bounds contact for reporting and SHALL restore the pre-shot playable ball state for stroke-and-distance

### Requirement: Seed-Derived Canonical Geometry

Canonical geometry SHALL be generated only from the established world seed hierarchy, hole seed, and generator version. It SHALL be immutable throughout a tournament apart from the existing per-round cup/pin variation, and SHALL contain no ambient randomness.

#### Scenario: A seed reproduces terrain

- **WHEN** a course is generated twice from the same seed and generator version
- **THEN** every canonical terrain region, tee, cup frame, and surface lookup result SHALL be identical

#### Scenario: A tournament keeps its terrain

- **WHEN** a hole is played in different rounds of the same tournament
- **THEN** the canonical terrain SHALL remain unchanged and only the permitted round-specific cup/pin position may vary

### Requirement: Bounded Legacy-Adapter Retirement

`HoleZones` and `ShotZoneProfile` SHALL be treated only as a temporary compatibility adapter for fixtures and controlled comparison tests. The adapter SHALL be removable from production when (1) every generated hole exposes canonical geometry, (2) generated human and AI paths resolve sampled contacts through `surfaceAt` and retain `BallState`, (3) the current-hole API and frontend render canonical regions/boundary and spatial settlement, (4) no public consumer uses the obsolete zone-profile read model, and (5) canonical-path calibration and equivalence gates pass. Production sources SHALL have an automated guard against new adapter dependencies; remaining test fixtures SHALL use a spatial builder or test-support-only adapter.

#### Scenario: A new gameplay feature cannot extend the legacy adapter

- **WHEN** production gameplay code is added after canonical authority is enabled
- **THEN** architecture tests SHALL reject a dependency on `HoleZones` or `ShotZoneProfile` outside the explicitly permitted compatibility seam
