## ADDED Requirements

### Requirement: Versioned Development Tour catalogue

New standard worlds SHALL schedule the 20 approved Development Tour events with permanent logical identities, names, geographic locations, weeks, prestige tiers, and course classifications. Their occurrence IDs and selected host course identities SHALL remain separate from those logical identities. The approved calendar SHALL be:

| Week | Event | Location | Classification | Policy |
|---|---|---|---|---|
| 1 | Carolina Gateway Open | North Carolina, USA | Parkland | Rotating |
| 2 | Sandhills Classic | Pinehurst region, North Carolina, USA | Heathland | Fixed |
| 3 | Lowcountry Coastal Open | Kiawah Island, South Carolina, USA | Links | Rotating |
| 4 | Piedmont Challenge | North Carolina / Georgia, USA | Woodland | Rotating |
| 5 | Southeastern Invitational | Georgia, USA | Parkland | Fixed |
| 8 | Ohio Valley Open | Ohio, USA | Parkland | Rotating |
| 9 | Lake Michigan Dunes Classic | Michigan, USA | Links | Rotating |
| 10 | Pine Barrens Open | New Jersey, USA | Heathland | Rotating |
| 12 | Hudson Valley Open | New York, USA | Woodland | Rotating |
| 13 | Westchester Championship | New York, USA | Parkland | Rotating |
| 15 | Ardennes Woodland Open | Belgium | Woodland | Rotating |
| 17 | Surrey Heath Invitational | Surrey, England | Heathland | Rotating |
| 18 | Northumbria Links Classic | Northumberland, England | Links | Rotating |
| 19 | Irish Midlands Open | County Kildare, Ireland | Heathland | Rotating |
| 20 | Fife Coast Championship | Fife, Scotland | Links | Rotating |
| 23 | Pacific Coast Classic | Monterey, California, USA | Links | Rotating |
| 24 | Coachella Valley Classic | California, USA | Desert | Rotating |
| 25 | Sonoran Invitational | Arizona, USA | Desert | Rotating |
| 26 | Rio Grande Valley Open | Albuquerque, New Mexico, USA | Parkland | Rotating |
| 29 | Development Tour Championship | Carolinas, USA | Parkland | Fixed |

Weeks 5, 17, and 25 SHALL remain Signature events; week 29 SHALL remain the Tour Championship; all other entries SHALL remain Regular.

#### Scenario: A permanent event returns

- **WHEN** the same Development event is scheduled in another season of a catalogue-enabled world
- **THEN** its logical identity, name, geographic location, week, prestige, and course classification SHALL remain unchanged
- **AND** its occurrence ID SHALL be new.

### Requirement: Regional procedural Development hosts

A catalogue-enabled world SHALL have one stable procedural host recipe for each fixed Development event and three geographically eligible recipes for each rotating event. Each season's schedule SHALL select one host per event from its eligible recipes and SHALL use 20 distinct Development host identities. Fixed events SHALL retain their host across seasons; rotating events SHALL visit their three eligible hosts deterministically and MAY return to a previous host. Full generated courses SHALL be resolved lazily from stable seed, recipe, and generator provenance.

#### Scenario: Fixed and rotating hosts

- **WHEN** a catalogue-enabled world schedules successive seasons
- **THEN** Sandhills Classic, Southeastern Invitational, and the Development Tour Championship SHALL keep their host course identities
- **AND** every other Development event SHALL rotate among three eligible procedural hosts
- **AND** no two Development events in one season SHALL select the same host identity.

### Requirement: Historical Development schedule compatibility

The world snapshot SHALL pin Development catalogue provenance independently of course generator version and course-pool policy. A snapshot without catalogue provenance SHALL preserve the existing Development naming and host assignment for its current, archived, and future seasons. It SHALL not infer catalogue identity from event week, displayed text, or host classification.

#### Scenario: An older career continues

- **WHEN** a world snapshot without catalogue provenance is restored
- **THEN** its scheduled and historical event names, hosts, results, and career records SHALL remain unchanged
- **AND** its future Development schedules SHALL continue using the historical rotation rule.

### Requirement: Versioned Development host names

New catalogue worlds SHALL use version 2 host naming: Links venues MAY retain Links suffixes, while Parkland,
Woodland, Heathland, and Desert venue names SHALL NOT end in Links or Golf Links. Catalogue version 1 SHALL
retain its original generated venue names after restore. The naming policy SHALL NOT change the catalogue,
venue recipe keys, host selection, course seeds, classifications, or geometry.

#### Scenario: An existing catalogue career is restored

- **WHEN** a catalogue version 1 world is restored
- **THEN** each selected venue SHALL keep its original generated name, including any Links suffix
- **AND** a new version 2 world with the same seed SHALL keep the same venue identities and geometry.
