## MODIFIED Requirements

### Requirement: Hybrid Spatial Representation

A generated production hole SHALL expose canonical two-dimensional terrain for gameplay and presentation. A version-supported generator MAY retain immutable route, landing-zone, green-complex, and progression-target semantics associated with the generated hole, but those semantics SHALL be planning metadata rather than a competing terrain representation. Shot sampling MAY retain a local carry/lateral frame, while production surface classification and playable settlement SHALL use canonical geometry at the sampled contact position. The frontend SHALL render gameplay landforms from that canonical geometry and SHALL NOT independently author, move, or reshape gameplay terrain.

One-dimensional zone-band data remains permitted only through the separately defined bounded legacy compatibility seam; it SHALL NOT become the authority for new generated terrain or production settlement.

#### Scenario: Semantic plan does not override a lie

- **WHEN** a sampled V3 shot contact lands inside a semantic landing-zone extent but on a canonical non-fairway surface
- **THEN** its resolved surface and settlement SHALL be determined by canonical geometry at that contact position

#### Scenario: Presentation remains canonical

- **WHEN** a client renders a V3 generated production hole
- **THEN** its gameplay landforms SHALL derive from supplied canonical geometry rather than recreating route, landing-zone, or green-complex geometry independently
