## ADDED Requirements

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
