## MODIFIED Requirements

### Requirement: Hybrid Spatial Representation

A generated production hole SHALL expose canonical two-dimensional terrain for gameplay and presentation. Shot sampling MAY retain a local carry/lateral frame, but production surface classification and playable settlement SHALL use canonical geometry at the sampled contact position. The frontend SHALL render gameplay landforms from that canonical geometry and SHALL NOT independently author, move, or reshape gameplay terrain.

One-dimensional zone-band data remains permitted only through the separately defined bounded legacy compatibility seam; it SHALL NOT become the authority for new generated terrain or production settlement.

#### Scenario: Production resolution uses canonical terrain

- **WHEN** a sampled shot contact is resolved on a generated production hole
- **THEN** its surface and playable settlement SHALL be determined by canonical geometry at that contact position

#### Scenario: Presentation renders rather than authors terrain

- **WHEN** a client renders a generated production hole
- **THEN** its gameplay-relevant landforms SHALL derive from supplied canonical geometry and changes to that geometry SHALL be reflected in gameplay and presentation

#### Scenario: Legacy zones remain bounded

- **WHEN** an explicitly legacy fixture or compatibility caller supplies only one-dimensional zone bands
- **THEN** it MAY resolve through the zone-band adapter without granting that adapter authority over new production terrain
