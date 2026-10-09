## ADDED Requirements

### Requirement: Condition-sensitive release is rendered from authoritative trace facts
For an observable eligible ground response, the canonical renderer SHALL show the backend-authored airborne path to first contact, the authoritative roll segment, and the final ball position in that order. It SHALL not calculate firmness, release distance, boundary intersection, final surface, or final placement locally.

#### Scenario: Firm and soft playback uses distinct returned endpoints
- **WHEN** otherwise equivalent firm and soft observable results return distinct authoritative roll endpoints
- **THEN** playback SHALL display each returned contact-to-roll endpoint and final point
- **AND THEN** it SHALL not replace either endpoint with a client-derived condition response.

#### Scenario: Ordinary boundary changes final lie visibly
- **WHEN** an authoritative permitted release finishes on a different ordinary playable surface from first contact
- **THEN** the renderer SHALL preserve distinct contact and final markers on their returned canonical positions
- **AND THEN** the next-shot marker SHALL use the resolver-authored final position and lie.

### Requirement: Clamped release remains distinguishable from recovery
When first-milestone boundary policy clamps an eligible release before a second or non-playable boundary, playback SHALL present the returned ground segment and final point as ordinary resolver-authored release, not as water relief, out-of-bounds replay, bounce, or invented hazard collision.

#### Scenario: Hazard-adjacent release does not create cosmetic recovery
- **WHEN** a trace returns a clamped release before a water or out-of-bounds boundary and contains no recovery transition
- **THEN** the renderer SHALL not show a splash, penalty, drop, replay, or contact-to-final rules transition
- **AND THEN** it SHALL leave all scoring and final-state meaning to the returned outcome.
