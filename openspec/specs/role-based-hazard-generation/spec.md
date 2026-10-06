# role-based-hazard-generation Specification

## Purpose
TBD - created by archiving change role-based-hazard-generation. Update Purpose after archive.
## Requirements
### Requirement: V4 hazard features have semantic provenance

Every V4 gameplay hazard region with surface `BUNKER`, `WATER`, `TREES`, or `RECOVERY_AREA` SHALL be derived from one deterministic immutable semantic hazard feature. A feature SHALL identify an existing surface, one bounded role, a meaningful strategic anchor, an applicable route/approach-relative side, bounded severity, and sufficient non-polygon envelope data for deterministic compilation. Hazard features SHALL remain generated metadata and SHALL NOT replace canonical terrain authority, become a player control, or be serialized as polygon copies.

#### Scenario: A V4 bunker can explain its purpose

- **WHEN** a V4 bunker region is inspected through generator diagnostics or tests
- **THEN** it SHALL trace to exactly one feature with a role and strategic anchor

#### Scenario: Semantics do not resolve a shot

- **WHEN** a V4 shot settles on a hazard
- **THEN** its surface, penalty, and playable state SHALL be resolved from canonical `CourseGeometry`, not the feature label

### Requirement: V4 uses a compact bounded role set

V4 SHALL support only `LANDING_GUARD`, `TURN_GUARD`, `GREEN_GUARD`, `BAILOUT_BOUNDARY`, and `RECOVERY_BOUNDARY` as initial hazard roles. Features SHALL anchor to a landing zone, route distance, actual dogleg corner, or approach-relative green side as appropriate. Cosmetic-only roles and unanchored random gameplay hazards SHALL NOT be introduced.

#### Scenario: Turn guard requires a turn

- **WHEN** a feature is assigned `TURN_GUARD`
- **THEN** the hole SHALL have a real generated dogleg corner that supplies its local anchor/frame

### Requirement: Role, frequency, and severity remain independent

V4 planning SHALL treat role as strategic purpose, frequency as bounded selection/budget, and severity as bounded intrusion or recovery consequence. Profile recovery severity MAY affect severity and placement bounds but SHALL NOT be implemented solely as an unbounded increase in hazard count. Biome MAY constrain plausible surface mix but SHALL NOT replace role-based planning.

#### Scenario: Penal profile remains intentional

- **WHEN** a V4 course has `PENAL` recovery intent
- **THEN** its hazards SHALL still have valid roles and anchors rather than being arbitrary additional shapes

### Requirement: Landing-zone hazards preserve viable decisions

V4 fairway hazards SHALL challenge only bounded edges/envelopes of their intended landing or turn decision. PRIMARY and SAFE zones SHALL retain usable hazard-free cores; AGGRESSIVE zones MAY have controlled additional exposure. Route-progression/default target points SHALL not lie directly within generated bunker, water, trees, or recovery-area terrain, and hazards SHALL not fully block intended progression.

#### Scenario: Risk/reward preserves the safe option

- **WHEN** a V4 risk/reward hole guards its aggressive landing area
- **THEN** its safe landing core and compatible route progression SHALL remain playable

### Requirement: Green guards honor green-complex intent

V4 green hazards SHALL use `GreenComplexPlan` approach direction and surround meanings. A protected-side guard SHALL align with the declared protected side; declared open entry and run-up space SHALL remain usable; the bailout side SHALL remain free of serious bunker/water obstruction in V4; and at least one plausible recovery sector SHALL remain. Bunker or water SHALL not cover the green core or completely seal normal approach access.

#### Scenario: Open green entry is retained

- **WHEN** a green complex declares an open entry or run-up opening
- **THEN** V4 hazard compilation SHALL leave its corresponding approach corridor free of serious hazard terrain

### Requirement: Water is endpoint-truthful

V4 water MAY be lateral to a landing edge, mark a risk/reward route boundary, make a route-side miss worse, or guard a protected green side. V4 SHALL NOT label or imply water as a mechanically enforced forced carry, creek crossing, or airborne interception while resolution remains endpoint/contact based.

#### Scenario: Water beyond contact point does not create a fictional penalty

- **WHEN** a shot's resolved contact clears a V4 water polygon
- **THEN** it SHALL not be penalized merely because an imagined flight path would have crossed that polygon

### Requirement: Trees and recovery areas use existing lie mechanics only

V4 trees and recovery-area features SHALL act only through their existing canonical surface/recovery consequences. V4 SHALL NOT claim tree collision, canopy interaction, blocked line of sight, mandatory punch-outs, or shot-shape requirements.

#### Scenario: Recovery boundary remains playable

- **WHEN** a ball settles in a V4 recovery-boundary tree or recovery-area region
- **THEN** its next shot SHALL use the existing playable lie and recovery-difficulty mechanics
