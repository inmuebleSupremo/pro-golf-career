# web-hole-visualization Specification

## Purpose
TBD - created by archiving change add-2d-hole-graphics. Update Purpose after archive.
## Requirements
### Requirement: Parametric 2D Hole Rendering

The Play Event screen SHALL render a 2D graphical representation of the hole being played from the current hole's canonical geometry: its tee, active cup, playable boundary, and ordered terrain-region polygons. It SHALL not generate gameplay landforms from coarse dimensions, hazard flags, or a local layout seed. The rendering SHALL scale to any hole in any pool course without hand-authored layouts: changing canonical geometry SHALL change the rendered gameplay shape correspondingly. A client MAY still use stable local data for non-gameplay decoration only.

#### Scenario: A hole is drawn from canonical geometry

- **WHEN** the player is on a hole with canonical fairway, green, bunker, water, and playable-boundary polygons
- **THEN** the 2D view SHALL draw those landforms from their supplied vertices, with tee and cup in the supplied play direction

#### Scenario: Different holes render differently

- **WHEN** two holes have different canonical geometry
- **THEN** their 2D renders SHALL differ in length and layout accordingly, without a local terrain-layout generator

### Requirement: Faithful Representation of Mechanical Facts

The 2D layer SHALL be a truthful representation of the simulation and SHALL NOT contradict any resolved mechanical fact. The hole's effective canonical terrain for the active event setup, active pin/cup position, and each resolved shot's contact, final playable ball state, distance remaining, and penalty SHALL be represented literally. The client SHALL NOT synthesize which flank a gameplay hazard occupies, dogleg lean, hazard longitudinal position, fairway-width variation, or any other gameplay terrain. Presentation details not represented by canonical geometry — vegetation scatter, texture, lighting, organic edge treatment, and camera treatment — MAY be synthesized only when they cannot imply or obscure a different gameplay surface.

#### Scenario: The pin is drawn at its real position

- **WHEN** the active round's cup has a canonical position
- **THEN** the flag SHALL be drawn at that position

#### Scenario: Setup-specific terrain is drawn truthfully

- **WHEN** an event applies a non-neutral width setup
- **THEN** the 2D view SHALL draw the effective terrain polygons used by shot settlement rather than base-hole terrain polygons

#### Scenario: A water recovery is shown truthfully

- **WHEN** a resolved shot contacts water and settles at a legal recovery position
- **THEN** the animation SHALL show the water contact and the subsequent authoritative recovery position, and the shown penalty and distance remaining SHALL match the resolved outcome

#### Scenario: Decoration never overrides canonical terrain

- **WHEN** a cosmetic layer would visually suggest a surface different from canonical terrain
- **THEN** canonical terrain SHALL win and the cosmetic layer SHALL be suppressed, clipped, or restyled

### Requirement: Stable, Deterministic Hole Appearance

A given hole's gameplay landforms SHALL look the same every time it is rendered because they derive from immutable canonical geometry, with only the permitted per-round cup position changing. Cosmetic decoration MAY be derived deterministically from a stable per-hole decoration seed so a course's holes read as permanent, designed venues; it SHALL not create or reshape a gameplay landform.

#### Scenario: A hole's gameplay layout is stable across rounds

- **WHEN** the same hole is played on different rounds of an event
- **THEN** its canonical fairway shape and hazards SHALL be identical, and only the permitted cup position and non-gameplay decoration variation MAY change

#### Scenario: Cosmetic layout is reproducible

- **WHEN** the same hole is rendered in two separate sessions
- **THEN** its deterministic cosmetic decoration SHALL be identical when given the same decoration seed

### Requirement: Biome-Styled Visual Themes

The 2D hole SHALL be styled by a visual theme selected from the host course's environment classification, so that each biome reads distinctly (grass shades, vegetation type, bunker and hazard styling). The mapping SHALL be: Parkland, Links, Desert, Alpine (Mountain), Heathland (Woodland), and a water-heavy Tropical theme for the Coastal classification.

#### Scenario: Theme follows the course classification

- **WHEN** a hole on a Links course and a hole on a Desert course are rendered
- **THEN** each SHALL use its biome theme — the Links hole with links styling, the Desert hole with desert styling

#### Scenario: Coastal renders as tropical

- **WHEN** a hole on a Coastal-classified course is rendered
- **THEN** it SHALL use the water-heavy Tropical theme

### Requirement: Shot Playback Animation

Each resolved shot SHALL be played back visually on the 2D hole using spatial settlement data: the ball travels from the authoritative pre-shot ball position to its sampled contact, then, when a penalty recovery or replay applies, visibly transitions to the resulting playable ball state. The final next-shot marker SHALL be on the authoritative settlement position and surface.

#### Scenario: A normal shot is animated to its result

- **WHEN** the simulation resolves a non-penalty player shot
- **THEN** the 2D view SHALL animate the ball from its authoritative origin to the contact/settlement position on the resolved surface

#### Scenario: A penalty recovery begins the next shot at settlement

- **WHEN** the simulation resolves a water drop or out-of-bounds replay
- **THEN** the 2D view SHALL retain the reported contact for playback and place the next-shot ball at the authoritative recovery or replay position

### Requirement: Flyover Introduction Per Hole

Each hole SHALL be introduced by a brief contextual card carrying the hole's identity (number, par, length) and the event's scene imagery, which SHALL recede to reveal the persistent 2D schematic as the play surface. The scene photograph SHALL serve this introductory and identity role rather than competing with the schematic during play.

#### Scenario: The intro precedes play

- **WHEN** the player reaches a new hole
- **THEN** a brief intro card showing the hole number, par, length, and scene imagery SHALL appear and then give way to the 2D hole for shot-by-shot play

### Requirement: Invertible canonical target mapping
The canonical SVG renderer SHALL provide tested project and unproject operations between canonical yard-space and SVG viewBox coordinates. For points in the interactive planning area, a project/unproject round trip SHALL remain within documented numerical tolerance.

#### Scenario: Pointer maps to selected aim point
- **WHEN** a user clicks or touches the canonical SVG
- **THEN** the viewBox coordinate is unprojected to a canonical aim point
- **AND THEN** rendering the target projects the same point back to the displayed location within tolerance.

### Requirement: Canonical trace playback
The canonical SVG renderer SHALL consume backend-authored `ShotTrace` values for visible shot playback. It MAY use frontend-owned timing/easing and a simple display interpolation from origin to contact, but that interpolation SHALL not be represented as or imply calculated ballistic flight.

#### Scenario: Playback uses authoritative endpoints
- **WHEN** canonical trace playback starts
- **THEN** its displayed start and contact positions SHALL be trace origin and trace contact
- **AND THEN** it SHALL not calculate a separate contact, lateral miss, or route from strategy.

### Requirement: Recovery/replay has distinct presentation
The canonical renderer SHALL present a trace transition as a labelled/styled rules recovery or replay, distinct from origin-to-contact display interpolation. It SHALL use trace final point as the resting ball location.

#### Scenario: Water relief is not animated as rollout
- **WHEN** a trace contains a water-drop transition
- **THEN** the renderer SHALL show water contact and legal drop as distinct semantic states
- **AND THEN** it SHALL not animate contact-to-drop as bounce, roll, or flight.

### Requirement: Synthetic flight authority retirement
Once all active playable holes use canonical geometry and trace playback covers normal shots, water, out of bounds, recovery, hole-out, and reduced motion, no live production play component SHALL depend on client-generated flight profiles, strategy/club trajectory mappings, carry-based club inference, synthetic bounce/spin/roll, or client-generated outcome placement.

#### Scenario: Canonical event has no synthetic fallback
- **WHEN** an active playable event renders a resolved shot
- **THEN** its presentation SHALL be driven by canonical geometry and its backend trace
- **AND THEN** it SHALL not fall back to synthetic flight authority that can disagree with resolver state.

### Requirement: Canonical roll playback
The canonical renderer SHALL consume optional backend-authored trace roll endpoints for a resolved shot. It MAY animate contact-to-roll-end as display timing, but SHALL not calculate roll distance, terrain response, or final placement client-side.

#### Scenario: Chip feedback uses trace roll
- **WHEN** a chip result includes an authoritative roll phase
- **THEN** playback SHALL show the returned contact and roll endpoint
- **AND THEN** the final marker SHALL use the returned final point.

### Requirement: Roll and recovery are visually distinct
Canonical playback SHALL distinguish calculated ground response from a rules-based recovery/replay transition through labels and styling, including reduced-motion presentation.

#### Scenario: Water never appears as roll
- **WHEN** a trace contains water contact and a recovery transition
- **THEN** the renderer SHALL show the rules transition distinctly
- **AND THEN** it SHALL not render contact-to-drop as a roll.

### Requirement: Contact versus final-state feedback
The renderer SHALL preserve separate semantic markers for first contact and final settled ball when they differ. It SHALL retain those facts in reduced-motion mode without relying on animation or colour alone.

#### Scenario: Release reaches a different final lie
- **WHEN** bounded resolver rollout changes the final settled point after playable contact
- **THEN** feedback SHALL identify contact and final ball as different authoritative states
- **AND THEN** it SHALL not infer either from carry/lateral or planning guidance.

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
