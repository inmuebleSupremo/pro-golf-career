## ADDED Requirements

### Requirement: Intentional shot shape
Each non-putting `BallStrikeIntent` SHALL contain non-null `ShotShape` of `STRAIGHT`, `FADE`, or `DRAW`, in addition to club, `AimPoint`, and `ShotFamily`. Shape SHALL express deliberate airborne curvature; it SHALL NOT be a player-selected carry, lateral-offset, launch, height, spin, wind, or randomness control. Compatibility adapters MAY default omitted legacy/API shape to `STRAIGHT`; the engine-level intent SHALL be complete.

#### Scenario: Shape does not replace landing intent
- **WHEN** a player submits a legal shaped ball strike
- **THEN** its `AimPoint` SHALL remain the intended canonical first-contact target
- **AND THEN** the player SHALL not submit a separate launch-line or curvature-magnitude field.

#### Scenario: Ideal shaped flight returns to its intended landing target
- **WHEN** ideal zero-error execution resolves under calm wind for otherwise equivalent STRAIGHT, FADE, and DRAW intents with the same submitted `AimPoint` that is within the resolver-derived effective reach
- **THEN** each `FlightSolution.positionAt(1)` SHALL equal that submitted `AimPoint`
- **AND THEN** FADE and DRAW SHALL have distinguishable correctly handedness-directed intermediate curvature.

#### Scenario: Observable handedness determines bulge and return direction
- **WHEN** zero-error, zero-wind RIGHT-handed and LEFT-handed intents resolve for an arbitrary non-degenerate
  origin-to-`AimPoint` axis
- **THEN** a RIGHT-handed DRAW SHALL have an intermediate position golfer-right of that axis before returning
  golfer-left toward `AimPoint`, and a RIGHT-handed FADE SHALL have an intermediate position golfer-left before
  returning golfer-right
- **AND THEN** LEFT-handed DRAW and FADE sides SHALL be the exact inverse
- **AND THEN** STRAIGHT SHALL have no deliberate intermediate shape bulge
- **AND THEN** each shape's endpoint at progress `1` SHALL equal `AimPoint`.

#### Scenario: Intent is not a landing guarantee
- **WHEN** ordinary execution error or nonzero directional wind is present
- **THEN** the authoritative first-contact position MAY differ from `AimPoint`
- **AND THEN** the resolver SHALL retain that actual `FlightSolution.positionAt(1)` as contact.

### Requirement: Authoritative parametric flight solution
The resolver SHALL create one pure deterministic parametric `FlightSolution` for each canonical non-putting ball strike. The solution SHALL be the sole authority for both authoritative first-contact position and any sampled airborne playback path. It SHALL evaluate finite canonical horizontal positions and non-negative derived heights over normalized progress from origin to contact.

#### Scenario: Flight solution owns contact
- **WHEN** a canonical ball strike is resolved
- **THEN** the resolver SHALL classify `FlightSolution.positionAt(1)` as the actual first-contact position
- **AND THEN** it SHALL not calculate a second independent contact projection.

### Requirement: Derived height has no player control
Flight height SHALL be derived deterministically from resolver-owned family, club, resolved travel, and bounded flight profile inputs. A player SHALL not select launch height, apex, spin, or trajectory parameters. Putt resolution SHALL remain outside airborne-flight behavior.

#### Scenario: Putt remains non-airborne
- **WHEN** the dedicated putting path resolves a stroke
- **THEN** wind and airborne-flight parameters SHALL not change its result
- **AND THEN** it SHALL not create an airborne path.

### Requirement: Shape eligibility and bounded execution tradeoff
FULL and CONTROLLED ball strikes SHALL permit STRAIGHT, FADE, and DRAW when otherwise legal. PITCH, CHIP, and BUNKER SHALL permit STRAIGHT only in this feature. A shaped eligible strike SHALL have a calibrated, bounded execution-control tradeoff using existing club control, player attributes, dispersion, and equipment workability inputs; it SHALL not introduce a new progression statistic, a fixed universal carry penalty, or a universally dominant shape.

#### Scenario: Ineligible short-game shape is rejected
- **WHEN** a player submits FADE or DRAW for a PITCH, CHIP, or BUNKER family
- **THEN** authoritative eligibility validation SHALL reject the stroke without changing ball, stroke count, or round state.

#### Scenario: Shape is not cosmetic or free
- **WHEN** equivalent legal STRAIGHT and shaped strikes resolve under fixed calm contexts
- **THEN** the shaped strike SHALL have a distinct authoritative airborne curve
- **AND THEN** calibrated execution-control behavior SHALL remain bounded and explainable through existing execution systems.

### Requirement: Separate wind and execution channels
The resolver SHALL keep ordinary execution dispersion, deterministic signed wind displacement, and wind-related uncertainty as separate named/calibrated channels. Signed wind displacement SHALL affect actual flight/contact through the aim-relative wind components. Wind-related uncertainty MAY widen outcome uncertainty but SHALL not duplicate deterministic displacement or legacy unsigned-crosswind logic. A calm wind vector and STRAIGHT shape SHALL preserve established baseline behavior.

#### Scenario: Crosswind has direction and uncertainty without double counting
- **WHEN** a legal ball strike resolves in nonzero crosswind
- **THEN** the resolver SHALL calculate signed cross-axis displacement and separately apply only its calibrated wind uncertainty
- **AND THEN** it SHALL not apply an obsolete second unsigned-crosswind penalty to the same physical effect.

#### Scenario: Calm straight compatibility
- **WHEN** a pre-feature-compatible canonical context resolves with calm wind and STRAIGHT shape
- **THEN** fixed-seed carry, lateral displacement, authoritative first contact, settlement/final ball state, score, and deterministic random consumption SHALL match the approved baseline result.
- **AND THEN** corpus-level calm STRAIGHT carry, distance-dispersion, lateral-dispersion, and scoring distributions SHALL remain within the approved baseline calibration bounds.

### Requirement: Contact and settlement remain authoritative boundaries
After deriving contact from `FlightSolution`, the resolver SHALL retain existing canonical surface classification, bounded post-contact roll, and `ShotSettlement` authority. Water/out-of-bounds recovery/replay remains a rules transition after contact. Airborne terrain along the path SHALL not alter a result in this feature.

#### Scenario: Path crossing has no premature collision effect
- **WHEN** an authoritative airborne curve geometrically passes over or through terrain before its computed first contact
- **THEN** only the computed first-contact endpoint SHALL be classified for this feature
- **AND THEN** no flight collision, forced carry, or intermediate recovery SHALL be invented.

### Requirement: Existing family rollout boundaries are preserved
This change SHALL preserve the existing bounded ground-response configuration: FULL, CONTROLLED, and BUNKER
families SHALL have no configured post-contact roll, while PITCH and CHIP MAY retain only their existing bounded
authoritative roll. It SHALL not introduce new rollout physics or extend roll across arbitrary terrain.

#### Scenario: Family rollout remains authoritative and bounded
- **WHEN** an observable FULL, CONTROLLED, or BUNKER strike settles without a recovery transition
- **THEN** its trace SHALL not contain authoritative roll and its final point SHALL equal first contact.
- **AND WHEN** an observable PITCH or CHIP strike has existing calculated roll
- **THEN** its trace SHALL expose only the bounded authoritative roll endpoints returned by the resolver.

## MODIFIED Requirements

### Requirement: Canonical ball-strike intent
The simulation SHALL accept immutable `ShotIntent` values. Its supported non-putting variant, `BallStrikeIntent`, SHALL contain a stable individual `ClubId`, an `AimPoint`, one non-null `ShotFamily`, and one non-null `ShotShape`; its putting variant SHALL be minimal `PuttIntent` and SHALL preserve existing non-spatial putting behavior. Human intent SHALL contain no strategy, target-distance, target-lateral, carry, rollout, launch, height, wind, or random-execution control.

#### Scenario: Intent states technique and shape but not execution knobs
- **WHEN** a human submits a ball-strike intent
- **THEN** the authoritative input contains selected club, literal intended landing point, intended technique, and shape
- **AND THEN** the resolver derives reach, dispersion, wind response, airborne path, and any permitted ground response.

### Requirement: AimPoint means intended first contact
`AimPoint(x,y)` SHALL be finite absolute canonical-hole yard coordinates for intended carry/first contact from the current ball state for STRAIGHT, FADE, and DRAW. It SHALL NOT mean initial launch line, final resting position, or a player-selected curvature endpoint.

#### Scenario: Compatibility resolution derives direction
- **WHEN** a valid intent is resolved
- **THEN** direction and requested travel are derived from current ball position to its aim point
- **AND THEN** existing reach caps, execution dispersion, directional wind, shape profile, and surface settlement determine actual contact.

### Requirement: Simplified resolver remains authoritative
This change SHALL preserve one deterministic simplified resolver beneath intent adaptation. It MAY add directional wind, eligible shot shape, resolver-derived height, and one bounded authoritative parametric airborne path whose exact endpoint becomes first contact. It MAY retain the explicitly bounded authoritative post-contact ground response needed for supported shot families. It SHALL NOT add flight-terrain or obstacle intersection, forced carries, bounce, spin, launch-velocity or full aerodynamic physics, player-controlled trajectory height, slope, firmness, arbitrary terrain traversal, or richer ground response.

#### Scenario: Flight remains bounded rather than full physics
- **WHEN** the resolver processes a supported family and shape intent
- **THEN** it produces calculated airborne contact/path facts, bounded permitted ground response, and settlement data
- **AND THEN** it does not calculate terrain collision, forced carry, bounce, spin, slope, terrain traversal, or a player-controlled flight parameter.

### Requirement: Truthful semantic shot trace
For a trace-materialized canonical shot, the simulation SHALL produce an immutable `ShotTrace` containing executing `ClubId`, pre-shot origin, exact intended `AimPoint`, optional authoritative airborne path, actual canonical `ShotContact`, optional authoritative `ShotTraceRoll`, optional recovery/replay transition, and final legal point. An airborne path SHALL contain only samples evaluated by the resolver's `FlightSolution`; the trace SHALL contain only values the resolver and settlement actually calculate.

#### Scenario: Normal contact has a truthful flight phase
- **WHEN** a canonical non-putting shot contacts a playable ordinary surface and has no calculated ground response
- **THEN** trace origin SHALL equal pre-shot `BallState.position`
- **AND THEN** any returned airborne path SHALL start at origin and end at resolved contact
- **AND THEN** roll and recovery transition SHALL be absent and final point SHALL equal settlement ball position.

### Requirement: Trace materialization is observational
The resolver SHALL support trace materialization without changing sampling, geometry lookup, settlement, score, deterministic random consumption, or `FlightSolution` contact. Summary-only resolution SHALL remain available for non-observable consumers and SHALL not require airborne sample allocation.

#### Scenario: Trace mode preserves resolved result
- **WHEN** identical canonical context and seed are resolved once with trace materialization and once summary-only
- **THEN** carry, lateral, contact, settlement, penalties, score, final ball state, and random consumption SHALL be equal
- **AND THEN** only trace presence and its resolver-derived samples may differ.

### Requirement: Bounded authoritative airborne representation
The resolver and trace MAY represent deterministic shape curvature, derived height, and aim-relative directional-wind response only through an authoritative `FlightSolution` and its derived samples. They SHALL NOT claim or calculate launch velocity, spin, bounce, slope, firmness, terrain crossing effects, time-of-flight, forced carry, tree collision, obstacle collision, player-controlled trajectory, or full aerodynamic physics.

#### Scenario: Airborne path does not become collision physics
- **WHEN** a resolver emits an authoritative airborne path
- **THEN** it SHALL expose only resolver-derived position/progress/height facts needed for contact and playback
- **AND THEN** it SHALL not use sample points as terrain-intersection authority or expose uncomputed physical events.
