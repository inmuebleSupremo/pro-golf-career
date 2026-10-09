## ADDED Requirements

### Requirement: Deterministic condition-sensitive ground response
After an eligible canonical non-putting shot first contacts a playable surface, the resolver SHALL calculate a finite deterministic desired release from the authoritative flight/contact facts, shot family, club/category, contact surface, lie, and ground firmness. FULL, CONTROLLED, PITCH, and CHIP SHALL be eligible; BUNKER and putts SHALL remain outside this response.

The response SHALL preserve meaningful ordering: equivalent eligible firm conditions SHALL not release less than soft conditions, and the response model SHALL retain distinct calibrated behaviour for low-lofted long shots, higher-lofted approaches, PITCH, and CHIP. The requirement does not prescribe uncalibrated yard constants.

#### Scenario: Firm fairway driver releases more than soft fairway driver
- **WHEN** identical deterministic driver strikes have equal airborne first contact on a playable fairway and differ only between firm and soft ground
- **THEN** the firm result SHALL finish no nearer its contact point than the soft result
- **AND THEN** both final positions and surfaces SHALL be resolver-authored and reproducible.

#### Scenario: Higher-lofted approach remains more restrained than long shot
- **WHEN** equivalent eligible firm-condition long-shot and higher-lofted approach fixtures contact comparable playable surfaces
- **THEN** the calibrated higher-lofted approach SHALL not receive the long shot's release entitlement
- **AND THEN** both outcomes SHALL retain their authoritative first-contact facts.

#### Scenario: Pitch and chip remain distinct
- **WHEN** eligible PITCH and CHIP fixtures resolve on the same playable surface and firmness
- **THEN** each SHALL use its own bounded response profile
- **AND THEN** neither SHALL silently become a FULL, CONTROLLED, BUNKER, or putt response.

### Requirement: Authoritative first contact precedes ground response
The resolver SHALL derive non-putting first contact exclusively from `FlightSolution.positionAt(1)` before applying ground response. Display samples, client geometry, and a separate contact calculation SHALL NOT influence release, settlement, scoring, or final ball state.

#### Scenario: Trace materialization does not alter ground response
- **WHEN** an identical canonical context and seed resolve summary-only and trace-materialized
- **THEN** their first contact, ground response, settlement, score, and final ball state SHALL be equal
- **AND THEN** only observable trace allocation may differ.

### Requirement: Bounded ordinary-surface boundary response
For the approved first-milestone policy, release SHALL follow one deterministic straight ground segment from first contact. It MAY cross at most one boundary between ordinary playable canonical surfaces. If the desired segment would cross a second boundary, WATER, or OUT_OF_BOUNDS, the resolver SHALL clamp the final point to the last deterministic valid ordinary playable point before that boundary. It SHALL NOT create a penalty, drop, replay, or general multi-surface traversal from release.

#### Scenario: One ordinary boundary transition is retained
- **WHEN** an eligible desired release crosses exactly one boundary from a playable ordinary surface to another playable ordinary surface and reaches no further boundary
- **THEN** the final ball SHALL settle at the deterministic released endpoint on its canonically classified final surface
- **AND THEN** the outcome SHALL retain distinct contact and final facts.

#### Scenario: Non-playable boundary is not treated as rollout hazard traversal
- **WHEN** an eligible desired release would reach WATER or OUT_OF_BOUNDS after first playable contact
- **THEN** the resolver SHALL clamp before that non-playable boundary
- **AND THEN** it SHALL not apply a release-created penalty, recovery, or replay.

#### Scenario: Second boundary caps first-milestone response
- **WHEN** an eligible desired release would cross more than one canonical surface boundary
- **THEN** the resolver SHALL clamp before the second boundary
- **AND THEN** it SHALL not continue with arbitrary surface traversal.

### Requirement: Ground response preserves deterministic compatibility
Ground response SHALL consume no uncontrolled randomness and SHALL preserve deterministic execution for identical context, seed, conditions, policy, and intended shot. Existing completed historical results SHALL not be re-resolved; loaded careers SHALL follow the approved future-stroke compatibility policy.

#### Scenario: Identical inputs repeat exactly
- **WHEN** an eligible canonical shot resolves twice with identical deterministic inputs
- **THEN** its contact, release endpoint, final ball, scoring, and settlement SHALL be equal
- **AND THEN** the response SHALL not consume an additional random draw.
