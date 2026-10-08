## ADDED Requirements

### Requirement: Canonical airborne path playback
The canonical hole renderer SHALL render a trace's backend-authored ordered airborne path using frontend-owned timing and reduced-motion presentation only. It SHALL preserve origin, contact, roll, recovery/replay, and final-settlement semantics already required by the trace contract.

#### Scenario: Airborne path ends at first contact
- **WHEN** playback renders a trace with airborne samples
- **THEN** the moving ball's final airborne position SHALL be the trace contact marker
- **AND THEN** any roll or recovery/replay SHALL begin only after that point.

### Requirement: Authoritative post-contact roll is sequenced visibly
When `ShotTrace.roll` is present, the renderer SHALL sequence backend-authored airborne playback to authoritative
first contact, then visually interpolate only from `roll.from` to `roll.to`, then reveal the authoritative final
resting ball position. It SHALL expose a visible `Rolling` phase/status for the positive authoritative roll and make
the contact and final-settlement facts distinguishable when their positions differ. Timing and/or non-geometric
presentation emphasis SHALL make short positive releases comprehensible at normal whole-hole scale without enlarging
their physical distance, changing resolver endpoints, or manufacturing movement. It SHALL not reveal final position
while airborne or roll playback remains active. When roll is absent, playback SHALL finish at contact without
inventing rollout; an authoritative recovery/replay transition continues after contact according to the returned
trace and remains semantically distinct from roll.

#### Scenario: Roll trace plays airborne, contact, roll, final
- **WHEN** an observable trace contains airborne samples and authoritative roll
- **THEN** the presentation order SHALL be `airborne → contact → roll → final`
- **AND THEN** contact, roll endpoints, final point, score, and settlement SHALL remain backend-authored.

#### Scenario: Existing short releases remain perceptible without altered mechanics
- **WHEN** deterministic authoritative fixtures contain the existing positive-roll examples of a `0.75`-yard PITCH
  release or a `2.55`-yard CHIP release
- **THEN** the renderer SHALL expose distinct airborne, contact, visible `Rolling`, and final-settlement phases using
  only their returned contact, `roll.from`, `roll.to`, and final point
- **AND THEN** it SHALL retain distinguishable contact/final feedback and SHALL not render the final ball before the
  `Rolling` phase completes
- **AND THEN** it SHALL not enlarge the endpoint separation, change timing-dependent resolver facts, or calculate a
  replacement roll distance.

#### Scenario: Trace without roll does not manufacture ground motion
- **WHEN** an observable trace has no authoritative roll, including FULL, CONTROLLED, BUNKER, or a recovery trace
- **THEN** playback SHALL finish airborne travel at contact and SHALL not animate invented rollout.

#### Scenario: Recovery stays a rules transition rather than a roll
- **WHEN** a trace contains an authoritative recovery/replay transition and no `ShotTrace.roll`
- **THEN** the renderer SHALL present the returned transition after contact without a `Rolling` status or
  contact-to-final ground-roll animation.

### Requirement: Playback releases canonical targeting at completion
The canonical-hole renderer SHALL reject target selection only while its authoritative result playback is active.
After non-holing playback clears, it SHALL accept pointer selection as canonical `AimPoint` input from the current
rendered ball position; showing or clearing a trace SHALL not change backend ball state.

#### Scenario: Result trace no longer blocks the next target
- **WHEN** a non-holing result playback has completed
- **THEN** selecting the canonical hole SHALL emit a new `AimPoint`
- **AND THEN** the previous result's textual feedback MAY remain visible without retaining input-blocking playback.

### Requirement: Playback samples are not terrain authority
The renderer SHALL not test sampled airborne points against rendered terrain or imply that passing over a displayed feature changed the resolved outcome. It SHALL not represent samples as future collision authority.

#### Scenario: Path crosses a rendered hazard before contact
- **WHEN** a returned airborne path visually crosses a rendered hazard before its contact point
- **THEN** the renderer SHALL preserve the resolver's returned contact and settlement facts
- **AND THEN** it SHALL not invent a collision, splash, penalty, or recovery.
