## MODIFIED Requirements

### Requirement: Hybrid Spatial Representation

A hole SHALL be authored, generated, and resolved in canonical two-dimensional yard-space. Shot resolution SHALL retain the current carry/lateral probability model during this migration, but it SHALL convert the sampled result from the current playable ball position into a canonical position and determine its production surface through canonical geometry. One-dimensional zone bands SHALL remain available only as a bounded compatibility adapter for legacy fixtures and unmigrated callers; they SHALL NOT remain the permanent source of surface truth for generated production holes.

#### Scenario: Production resolution uses canonical geometry

- **WHEN** a shot is resolved on a generated production hole
- **THEN** its final contact surface SHALL be obtained from canonical `surfaceAt(position)` rather than from independently authored presentation geometry or a zone-band lookup

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

### Requirement: Zone-Band Abstraction

A **zone band** SHALL remain a valid compatibility representation for a legacy shot context while the spatial migration is in progress. Canonical geometry SHALL be the first-class contract between generated production courses and shot settlement. Any legacy profile SHALL remain contiguous and gap-free and SHALL map every sampled legacy landing to one surface, but new terrain features SHALL be expressed in canonical geometry rather than added to zone-band rules.

#### Scenario: Legacy bands remain valid

- **WHEN** a legacy shot context is resolved through the compatibility adapter
- **THEN** its bands SHALL cover the reachable range without unresolved gaps and classify every sampled legacy landing

#### Scenario: New terrain is canonical

- **WHEN** a new production terrain feature such as a one-sided bunker or water hazard is introduced
- **THEN** it SHALL be represented by canonical geometry and resolved by `surfaceAt(position)`, not by extending a symmetric zone band
