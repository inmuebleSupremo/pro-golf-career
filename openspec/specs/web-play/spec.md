# web-play Specification

## Purpose
TBD - created by archiving change add-play-event. Update Purpose after archive.
## Requirements
### Requirement: Advance to an event

The system SHALL let the player advance the career one week at a time, and SHALL surface a way to play their tournament when advancing lands on one.

#### Scenario: Advancing reaches a tournament

- **WHEN** the player advances the week and their golfer has a tournament that week
- **THEN** the career presents a way to play that event

#### Scenario: Advancing an ordinary week

- **WHEN** the player advances the week and their golfer has no tournament that week
- **THEN** the career shows the new week with no pending event

### Requirement: Shot-by-shot play

The system SHALL let the player play a pending event shot by shot. For an ordinary non-putting shot, the player SHALL
choose a legal club, literal canonical target, shot family, and shape; the simulation SHALL resolve the outcome.

#### Scenario: Play a shot

- **WHEN** the player submits the current shot's club, target, family, and shape
- **THEN** the outcome is resolved and the next shot situation (or event completion) is presented

#### Scenario: Situation and standing are shown

- **WHEN** the player is on the play screen for a pending event
- **THEN** the current shot situation and the player's position on the leaderboard are displayed

#### Scenario: Target is constrained to the shot

- **WHEN** the player selects a target on the hole
- **THEN** its coordinates SHALL remain inside the server's inclusive aim envelope
- **AND THEN** approximate club reach SHALL be information, not a target-selection limit

### Requirement: Sim shortcuts

The system SHALL let the player skip play at any point by simulating the current shot, the rest of the hole, the rest of the round, or the rest of the event.

#### Scenario: Sim the rest of the event

- **WHEN** the player chooses to sim the rest of the event
- **THEN** the event is resolved without further shot decisions and the player is offered event completion

### Requirement: Complete the event

The system SHALL let the player complete a finished event so its result counts and the week resumes, returning them to the career hub.

#### Scenario: Finish the event

- **WHEN** the event's play is finished and the player completes it
- **THEN** the result is committed, the paused week resumes, and the player is returned to the career hub

### Requirement: Play-state handling

The play surface SHALL handle the absence of a pending event and the usual loading and error states.

#### Scenario: No event to play

- **WHEN** the play screen is opened for a session with no pending event
- **THEN** the player is directed back to the career hub

#### Scenario: Loading and error

- **WHEN** the play data is loading or fails to load
- **THEN** the corresponding loading or error state is shown

### Requirement: Spatial manual targeting
The play UI SHALL let a human choose an individual club and preview/select a literal canonical aim point on the hole display. It SHALL submit that point, not a derived strategy/distance/lateral decision.

#### Scenario: Player selects a course point
- **WHEN** a player clicks or taps a legal point on the displayed hole
- **THEN** the UI displays its target marker and intended direction, with updated target distance
- **AND THEN** submission uses the displayed canonical coordinates without guaranteeing that landing position.

### Requirement: Accessible multi-input targeting
The play UI SHALL support desktop pointer targeting and touch-friendly mobile targeting. It SHALL retain labelled
numerical X/Y target controls in Shot details for precise keyboard adjustment, and SHALL expose selected club,
distance, normal reach, and stale-state feedback without relying solely on colour.

#### Scenario: Keyboard player adjusts target
- **WHEN** a keyboard user operates the numerical target controls
- **THEN** a legal coordinate change updates the same visible target and submitted point
- **AND THEN** the current distance remains readable without relying solely on colour.

### Requirement: Human risk control retirement
The play UI SHALL remove conservative/balanced/aggressive as human shot-selection controls.

#### Scenario: Manual action dock has no strategy input
- **WHEN** a player prepares a manual ball strike
- **THEN** the action controls present legal club, technique, shape, target information, and submit state
- **AND THEN** no human strategy selector is shown or sent.

### Requirement: Trace-owned result feedback
During visible-shot playback, the play UI SHALL use the returned trace's intended aim, contact, transition, and final
point. Previous-shot text MAY use authoritative outcome carry, final lie, remaining distance, and penalty facts. The UI
SHALL not derive a completed shot's visual path or position from current guidance, selected next aim, or client terrain.

#### Scenario: Refetch cannot replace the completed shot target
- **WHEN** the server advances to the next pending shot while the prior visible shot is still being presented
- **THEN** result feedback SHALL display the completed trace's intended aim point
- **AND THEN** it SHALL not substitute the new shot's guidance/default target.

### Requirement: Accessible semantic result feedback
The play UI SHALL distinguish intended target, actual contact, final legal ball position, and any recovery/replay transition with labels and non-colour-only cues. It MAY show a miss vector from aim to contact when helpful.

#### Scenario: Penalty result is understandable without inferred motion
- **WHEN** a trace contains a water-drop or replay transition
- **THEN** the UI SHALL identify the contact and legal recovery/replay destination
- **AND THEN** it SHALL not imply the ball physically travelled between them.

### Requirement: Reduced-motion trace comprehension
When reduced motion is enabled, the play UI SHALL still expose all trace semantic markers and recovery/replay distinction without requiring animation to understand the result.

#### Scenario: Reduced-motion player receives complete result
- **WHEN** a visible shot resolves with reduced motion enabled
- **THEN** the UI SHALL render target, contact, final point, and applicable transition in a stable state
- **AND THEN** it SHALL retain accessible textual result feedback.

### Requirement: Deliberate technique selection
The play UI SHALL support deliberate target, club, technique, and shape choices before Play shot. It SHALL submit the
chosen legal family in `BallStrikeIntent` and SHALL not infer a technique from distance or a human strategy label.

#### Scenario: Unavailable family is not submitted
- **WHEN** current guidance marks a family unavailable for the selected club and lie
- **THEN** the controls SHALL offer only available families for that selection
- **AND THEN** a changed shot situation SHALL rederive a legal selection before submission.

### Requirement: Landing-target semantics
For PITCH and CHIP, the UI SHALL identify AimPoint as landing target / intended first contact. It SHALL not predict or present an uncomputed final resting position during planning.

#### Scenario: Planned chip target is not ball rest
- **WHEN** a player selects CHIP and adjusts the marker
- **THEN** the UI SHALL describe that marker as the landing target
- **AND THEN** final position remains resolver-owned until the shot resolves.

### Requirement: Putting boundary remains explicit
The UI SHALL select the green putting interaction from the backend-authored event putting policy while allowing deliberate eligible fringe ball-strike choices. For `LEGACY`, it SHALL preserve dedicated `playPutt` with no spatial controls. For an eligible `PHYSICAL_V1` green lie, it SHALL present the dedicated overhead putting view with independently adjustable intended starting direction and pace, optional backend-derived green-reading overlays, and authoritative trajectory playback. It SHALL not calculate a physical outcome, cup capture, or final resting position in the browser.

#### Scenario: Green does not show ball-strike family selector
- **WHEN** current lie is GREEN
- **THEN** the UI SHALL use the putting path
- **AND THEN** it SHALL not present normal ball-strike family choices.

#### Scenario: Legacy putting remains unchanged

- **WHEN** the backend reports `LEGACY` putting policy
- **THEN** the UI SHALL use the established `playPutt` interaction
- **AND THEN** it SHALL not expose physical line, pace, or green-reading controls.

#### Scenario: Physical playback does not reveal future state

- **WHEN** a `PHYSICAL_V1` putt is animating
- **THEN** the UI SHALL render only the already traversed authoritative trajectory
- **AND THEN** it SHALL defer final resting state and hole-completion presentation until the relevant terminal sample is reached.

### Requirement: Human play selects only legal shot shape
For a non-putting shot, the play surface SHALL offer server-derived legal shapes alongside existing club, technique, and literal landing-target selection. It SHALL not expose curvature magnitude, wind compensation, launch, height, spin, endpoint prediction, or execution randomness controls.

#### Scenario: Short-game shape restriction is visible
- **WHEN** the selected club/family/lie permits only STRAIGHT
- **THEN** the play surface SHALL not submit FADE or DRAW for that shot.

### Requirement: Browser has no flight authority
The browser SHALL send only the selected club, family, shape, literal `AimPoint`, and revision. It SHALL render result feedback from backend `ShotTrace` values and SHALL not derive curve, wind drift, apex, first contact, rollout, recovery, or final ball position.

#### Scenario: Result feedback follows server path
- **WHEN** an observable result includes an airborne path
- **THEN** playback SHALL use the returned ordered points rather than a client-generated ballistic or curved route.

### Requirement: Wind is a minimal read-only gameplay observation
The play surface SHALL render the backend-authored effective canonical wind flow as a small direction arrow and
strength indicator in the existing gameplay UI. The arrow SHALL use an unmistakable symmetric arrowhead whose tip
points where wind is flowing toward after the same canonical-to-screen transformation used for the hole. At normal
gameplay scale, adjacent visible text SHALL say `Wind toward` and display the backend-provided effective-wind
strength with its stated unit. It SHALL clearly use canonical/effective reference semantics, not geographic north or
compass labels, and SHALL not provide wind compensation or flight-prediction controls.

#### Scenario: Wind presentation remains informational
- **WHEN** the active play read contains nonzero effective canonical wind flow
- **THEN** the UI SHALL show its direction and magnitude
- **AND THEN** changing `AimPoint` SHALL remain a player decision and SHALL not cause the browser to compute drift,
  carry, contact, settlement, or a replacement trajectory.

#### Scenario: Projected cardinal canonical flow reaches the matching arrow tip
- **WHEN** deterministic effective-wind fixtures use canonical flow vectors `(+X)`, `(-X)`, `(+Y)`, and `(-Y)`
- **THEN** the projected symmetric arrowhead tip SHALL respectively point screen-right, screen-left, screen-up, and
  screen-down
- **AND THEN** each nonzero fixture SHALL visibly identify the direction as `Wind toward` and show its effective
  strength/unit without implying geographic orientation.

#### Scenario: Development preview is presentation-only
- **WHEN** a developer opens the development-only wind-display preview with fixed, server-shaped headwind, tailwind,
  or crosswind fixtures
- **THEN** it SHALL render the same wind-display component used by play
- **AND THEN** it SHALL not mutate live weather/conditions, player state, ball state, shot intent, resolver inputs,
  or gameplay authority.

### Requirement: Player-directed shot strategy

The ordinary human shot dock SHALL allow the player to choose an intended target directly on the course and SHALL
not present SAFE/PRIMARY/AGGRESSIVE route buttons, prescribed route explanations, or suggested club-and-route
combinations. Backend route geometry, guidance data, and AI policy SHALL remain available to their existing consumers.

#### Scenario: Strategic options remain internal to ordinary human play

- **WHEN** a V4 shot exposes distinct SAFE and AGGRESSIVE strategic options
- **THEN** the ordinary human dock SHALL still accept any legal manual target within the aim envelope
- **AND THEN** it SHALL not turn those options into prescribed route controls or recommendations.

#### Scenario: Manual aim survives shot setup changes

- **WHEN** the player selects a legal course target and then changes club, technique, or shape
- **THEN** the selected target SHALL remain visible and SHALL be submitted with the current legal shot settings
- **AND THEN** the next shot revision SHALL start from its own default target.

### Requirement: Compact factual shot dock

For ordinary shots, the collapsed responsive dock SHALL show distance to pin, distance to the selected target, and
the selected club's server-provided normal reach together. Shot details SHALL disclose factual context and precise
target coordinates without permanently enlarging the dock. Previous-shot feedback SHALL have a concise summary and
expandable authoritative carry, lie, remaining-distance, and exceptional-result facts.

#### Scenario: Selected club or target changes the comparison

- **WHEN** the player selects another club or legal target
- **THEN** normal reach or target distance SHALL update from the current server club data or selected world point
- **AND THEN** the pin distance SHALL remain the current situation's authoritative value.

#### Scenario: A completed shot does not displace the controls

- **WHEN** shot playback completes and result feedback appears
- **THEN** the dock SHALL show the previous result separately from the next shot's target measurements
- **AND THEN** primary controls SHALL remain in their compact responsive arrangement.

#### Scenario: Introductory aim hint is dismissed

- **WHEN** a valid new point is selected directly on the course for the first time in a play session
- **THEN** the introductory aiming hint SHALL stay dismissed through later shots and holes
- **AND THEN** an invalid or unchanged target interaction SHALL not dismiss it.

### Requirement: Availability-backed shot controls and click-first aiming

The play UI SHALL initialize and maintain its non-putting club, family, and shape selection from the current server-authored availability projection. It SHALL preserve a still-legal player selection and SHALL not submit a default combination that the projection marks unavailable. The server remains the final authority for submitted intents. The hole click/tap target is the normal aiming interaction; the UI SHALL retain accessible numerical X/Y precision controls under a compact disclosure and SHALL not render directional aim-nudge buttons.

#### Scenario: A recovery lie changes the legal default without removing legal choice

- **WHEN** the current lie makes a long club or ordinary technique unavailable and a legal recovery combination is projected
- **THEN** the controls select a legal club, family, and shape
- **AND THEN** a player may still choose any other combination that the current projection marks legal.

### Requirement: Expected shot-validation feedback is actionable

When the server rejects a submitted shot with an expected `BAD_REQUEST` validation error, the play UI SHALL display the safe validation message. It SHALL show generic retry feedback for network or unexpected failures and SHALL not expose implementation details from those failures.

#### Scenario: An invalid manual aim is explained

- **WHEN** a course interaction would select a point outside the server's planning envelope
- **THEN** the dock SHALL retain the last legal visible target
- **AND THEN** any out-of-envelope submission that reaches the API SHALL be rejected without resolving a stroke.

