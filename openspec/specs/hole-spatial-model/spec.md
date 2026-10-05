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

A **zone band** SHALL remain a valid compatibility representation for a legacy shot context while the spatial migration is in progress. Canonical geometry SHALL be the first-class contract between generated production courses and shot settlement. Any legacy profile SHALL remain contiguous and gap-free and SHALL map every sampled legacy landing to one surface, but new terrain features SHALL be expressed in canonical geometry rather than added to zone-band rules.

#### Scenario: Legacy bands remain valid

- **WHEN** a legacy shot context is resolved through the compatibility adapter
- **THEN** its bands SHALL cover the reachable range without unresolved gaps and classify every sampled legacy landing

#### Scenario: New terrain is canonical

- **WHEN** a new production terrain feature such as a one-sided bunker or water hazard is introduced
- **THEN** it SHALL be represented by canonical geometry and resolved by `surfaceAt(position)`, not by extending a symmetric zone band

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

### Requirement: Canonical projection preserves the legacy local shot frame

For a canonical full shot, the resolver SHALL retain the legacy sampler's carry/lateral semantics in an explicit local frame. The frame origin SHALL be the current playable `BallState`; its forward axis SHALL point to the green-centre reference at the active cup's front/back depth; and positive lateral SHALL be golfer-right. The active cup's lateral coordinate SHALL be derived in that frame before strategy targeting is applied, so a pin offset neither rotates the frame nor gets applied twice. The existing sampled carry distribution SHALL remain measured against its legacy remaining-distance input. A canonical putt, whose distance is already measured directly to the physical cup, SHALL instead use a cup-facing local frame.

#### Scenario: Tucked pin does not double-count its lateral offset

- **WHEN** a full shot starts on the green-centre reference and its active cup is laterally offset
- **THEN** zero sampled lateral displacement lands on the centre reference, and a local lateral displacement equal to the cup's frame-relative offset lands at the cup

#### Scenario: Persistent off-centre ball establishes a new local frame

- **WHEN** a playable ball settles left or right of the initial corridor and takes another full shot
- **THEN** the next projection SHALL begin at that stored ball position and use its own ball-to-reference direction, without reapplying the previous miss as a global lateral offset

#### Scenario: Legacy fixtures retain a migration seam

- **WHEN** an existing test or explicitly legacy caller supplies only a one-dimensional hole model
- **THEN** it MAY resolve through the zone-band adapter while preserving deterministic legacy behaviour

#### Scenario: Two-dimensional layout is gameplay authority

- **WHEN** canonical terrain changes in a generated production hole
- **THEN** surface lookup and resulting shot settlement SHALL reflect that terrain change

