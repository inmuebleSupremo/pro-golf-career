## MODIFIED Requirements

### Requirement: Per-Round Pin Positions

Every Hole SHALL have exactly one active pin position, which SHALL remain fixed for the duration of a completed round. Pin positions MAY differ between rounds of the same tournament, derived deterministically per round. The pin-placement version selected for that tournament SHALL be an explicit input to derivation and SHALL remain fixed for the tournament, including playoffs.

`LEGACY_V1` SHALL preserve the historical pin-generation behaviour exactly. `V5_EFFECTIVE_GREEN` SHALL derive its intended signed depth and lateral placement from the existing deterministic pin draws, interpret that intent in the effective green's local approach/right frame, and resolve the cup against the same setup-specific canonical geometry used by shot settlement. A V5 cup SHALL classify as `GREEN` through `surfaceAt` and SHALL have a shortest Euclidean distance of at least 2.0 yards to every edge of the effective GREEN polygon. V5 SHALL use bounded, deterministic geometry-aware projection into that eligible inset when intent is invalid; it SHALL NOT use ambient randomness, unbounded rejection sampling, radial/rectangular proxies, nominal unscaled green geometry, or a clearance-reducing fallback.

The generator or setup validation boundary SHALL fail deterministically if its effective GREEN has no 2.0-yard eligible pin region. A V5 front pin SHALL retain a front signed intent and a back pin a back signed intent, so the existing asymmetric green-complex semantics remain meaningful; left/right tuck semantics shall likewise remain meaningful. A centre pin SHALL leave the green symmetric about it.

#### Scenario: V5 cup is valid in effective geometry

- **WHEN** a V5 tournament derives a hole's pin under a supported non-neutral setup
- **THEN** its active cup SHALL be GREEN in that hole's setup-specific canonical geometry
- **AND THEN** its true shortest polygon-edge clearance SHALL be at least 2.0 yards

#### Scenario: Pin is fixed within a round

- **WHEN** a round is in progress on a hole
- **THEN** that hole SHALL expose exactly one active pin position that does not change until the round completes

#### Scenario: Pins may vary across rounds deterministically

- **WHEN** pin positions are derived for two different rounds of the same tournament
- **THEN** they MAY differ, and each SHALL be reproducible from the course seed, course-generator version, pin-placement version, effective setup, round identity, and established canonical inputs

#### Scenario: A back pin brings the over-green trouble closer

- **WHEN** the green complex is emitted for a back pin versus a front pin
- **THEN** the back pin SHALL have less green behind it (over-green trouble nearer), and the front pin SHALL have less green in front of it (a shorter safe run-up)

#### Scenario: Unsupported effective inset fails explicitly

- **WHEN** a V5 setup-specific GREEN cannot contain any point with 2.0 yards polygon-edge clearance
- **THEN** pin placement SHALL fail deterministically rather than placing a closer, legacy, or non-GREEN cup
