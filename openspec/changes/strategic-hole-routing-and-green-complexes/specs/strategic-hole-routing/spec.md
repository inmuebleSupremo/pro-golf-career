# strategic-hole-routing Specification

## Purpose

Define the V3 internal spatial-design layer that turns course-design briefs into deterministic routes, landing zones, green complexes, and progression targets before canonical terrain is compiled.

## ADDED Requirements

### Requirement: Constrained deterministic route

A V3 generated hole SHALL own a deterministic semantic `HoleRoute` generated from its brief and seed before canonical geometry compilation. The route SHALL contain a tee origin, a green/approach anchor, and zero or one intermediate anchor. Zero anchors SHALL represent a straight route and one anchor MAY represent a single dogleg. V3 SHALL NOT generate a double dogleg or a branching route.

#### Scenario: Same V3 input reproduces its route

- **WHEN** a V3 hole is generated twice from the same supported version, seed, classification, and design inputs
- **THEN** its route and all anchor coordinates SHALL be identical

#### Scenario: V3 rejects unsupported route complexity

- **WHEN** a V3 route candidate contains two intermediate anchors or a branching route
- **THEN** it SHALL be rejected before canonical terrain is compiled

### Requirement: Route feasibility precedes terrain compilation

Before V3 terrain is emitted, the generator SHALL validate route connectivity, increasing progression, bounded segment displacement and turn angle, non-self-intersection, route length appropriate to the brief, landing-zone reachability, and a valid final approach corridor. Candidate retries, if needed, SHALL be deterministic and bounded; exhaustion SHALL fail explicitly.

#### Scenario: Invalid route cannot produce a hole

- **WHEN** a route candidate crosses itself or cannot progress from tee to approach anchor
- **THEN** it SHALL not produce `CourseGeometry`

#### Scenario: Landing zone supports final approach

- **WHEN** a V3 landing zone is accepted for a hole
- **THEN** it SHALL have a route-ordered reachable position and a valid preferred approach corridor to the green entry

### Requirement: Landing zones express internal strategic intent

Every V3 `LandingZone` SHALL define a route-relative role, route distance, signed centre offset, depth, half width, preferred approach side, and abstract `ReferenceCarryBand`. V3 SHALL support `PRIMARY`, `SAFE`, and `AGGRESSIVE`; `LAYUP` MAY be represented only as an optional/future role. Zones SHALL not identify clubs, player attributes, player-controlled aim points, or new shot physics.

#### Scenario: Risk/reward offers a real spatial trade-off

- **WHEN** an eligible risk/reward brief receives safe and aggressive zones
- **THEN** the aggressive zone SHALL be shorter to the approach and/or have a more favourable approach relationship than the safe zone, rather than differing solely by hazard probability

#### Scenario: Zone does not create a player control

- **WHEN** a landing zone is available on a V3 hole
- **THEN** it SHALL remain generator metadata and SHALL not by itself expose a club-specific or free-aim player control

### Requirement: Archetypes map to bounded spatial intent

V3 SHALL spatially express the existing `POSITIONAL`, `BALANCED`, and `RISK_REWARD` hole archetypes. Positional holes SHALL prefer controlled primary geometry plus a safer spatially inferior alternative where applicable; balanced holes SHALL retain a neutral primary/approach relationship; eligible risk/reward holes SHALL use safe/aggressive spatial alternatives. These mappings SHALL compose with, not replace, profile and biome constraints.

#### Scenario: Positional preferred side is meaningful

- **WHEN** a positional V3 hole has a preferred approach side
- **THEN** its primary and safer alternatives SHALL encode different approach relationships in the route/zone plan

#### Scenario: Balanced does not fabricate a trade-off

- **WHEN** a balanced V3 hole is generated
- **THEN** it SHALL not be required to introduce an aggressive alternative or artificial forced narrowness

### Requirement: Green complex describes orientation without replacing terrain authority

A V3 `GreenComplexPlan` SHALL define green-centre offset from the approach anchor, major and minor dimensions, rotation, approach direction, open/protected/bailout sides, and optional run-up opening. Its surround meanings SHALL use only `OPEN_ENTRY`, `PROTECTED_SIDE`, `BAILOUT_SIDE`, `SHORT_MISS`, `LONG_MISS`, and `RECOVERY_SIDE`. The plan SHALL be compiled to a rotated ellipse or another simple convex canonical polygon with a valid approach relationship; it SHALL not add new terrain surface types, contours, elevation, or spatial putting.

#### Scenario: Green orientation matches approach

- **WHEN** a V3 green complex is compiled
- **THEN** its orientation, entry/open side, and final approach direction SHALL be geometrically consistent

#### Scenario: Surround semantics stay internal

- **WHEN** a surround role is generated
- **THEN** it MAY guide placement of existing canonical surfaces but SHALL not become a new `Surface` value or a putting mechanic

### Requirement: Canonical compilation retains one terrain authority

V3 SHALL compile route, zones, green complex, and minimal compatible hazard placement into one simple canonical `CourseGeometry`. The fairway corridor SHALL follow the route and may vary width for zones, pinch/widen, bailout, and approach behavior, but SHALL NOT create a multi-route network. Gameplay settlement and SVG rendering SHALL continue to use canonical geometry rather than the semantic records.

#### Scenario: Route semantics compile into canonical fairway

- **WHEN** a V3 one-dogleg route is generated
- **THEN** the playable fairway corridor SHALL follow the valid route while canonical geometry remains the source of surface lookup

#### Scenario: Semantics are not a second terrain model

- **WHEN** a shot settles on a V3 hole
- **THEN** its surface and playability SHALL be resolved from `CourseGeometry`, not from landing-zone or surround labels

### Requirement: Existing decision flows receive compatible progression targets

V3 SHALL derive deterministic internal progression targets from its route and zones. Existing AI targeting and the human/default decision path SHALL use the appropriate generated progress target when available; existing strategy labels MAY choose a safe or aggressive recommendation. V1/V2 and absent-target callers SHALL use the deterministic green-centre fallback. This requirement SHALL NOT add player free aiming, new shot types/shapes, or a redesign of current control flows.

#### Scenario: Dogleg default avoids an inside cut

- **WHEN** an existing default shot flow plays a V3 dogleg before the final approach
- **THEN** it SHALL receive the generator-provided progression target rather than being forced to aim directly at green centre

#### Scenario: Legacy hole remains compatible

- **WHEN** a V1 or V2 hole is played through the same target-selection seam
- **THEN** it SHALL retain the deterministic legacy green-centre behavior

### Requirement: V3 quality contract is separate from historical equality

The V3 implementation SHALL use a committed deterministic corpus and locked pre-tuning calibration contract covering route feasibility, canonical validity, repeatability, target behavior, playable resolution, and scoring envelopes. Existing V1/V2 fixtures SHALL remain exact historical compatibility assertions; V3 SHALL not be required to have byte-identical V2 geometry or scores.

#### Scenario: V3 calibration distinguishes intent from regression

- **WHEN** a V3 corpus or scoring diagnostic changes
- **THEN** V1/V2 fixture drift SHALL fail as a historical regression, while valid V3 output SHALL be assessed against the committed V3 geometry and scoring envelope
