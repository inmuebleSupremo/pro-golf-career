## ADDED Requirements

### Requirement: Parametric 2D Hole Rendering

The Play Event screen SHALL render a 2D graphical representation of the hole being played, generated parametrically from that hole's simulation geometry — its par, length, fairway and green dimensions, elevation, and hazard presence — rather than from a fixed per-hole image. The rendering SHALL scale to any hole in any pool course without hand-authored layouts: changing a hole's geometry SHALL change the rendered shape correspondingly.

#### Scenario: A hole is drawn from its geometry

- **WHEN** the player is on a hole with a given length, fairway width, and green size
- **THEN** the 2D view SHALL show a corridor and green whose proportions reflect those values, with the tee and green in the play direction

#### Scenario: Different holes render differently

- **WHEN** two holes of different par and length are played
- **THEN** their 2D renders SHALL differ in length and layout accordingly, from the same generator

### Requirement: Faithful Representation of Mechanical Facts

The 2D layer SHALL be a truthful representation of the simulation and SHALL NOT contradict any resolved mechanical fact. The hole's dimensions, the active pin's lateral side, and each resolved shot's final surface, distance remaining, and penalty SHALL be represented literally. Presentation details the simulation does not model — which flank a hazard sits on, dogleg lean, hazard longitudinal position, and vegetation scatter — MAY be synthesized, provided they never contradict a load-bearing fact.

#### Scenario: The pin is drawn on its real side

- **WHEN** the active round's pin has a lateral offset to one side of the green
- **THEN** the flag SHALL be drawn on that side

#### Scenario: A hazard outcome is shown in a hazard

- **WHEN** a resolved shot finishes on a penalty surface such as water or a bunker
- **THEN** the ball SHALL be shown resting in a hazard of that type, and the shown penalty and distance remaining SHALL match the resolved outcome

#### Scenario: Cosmetic placement never overrides an outcome

- **WHEN** a shot's synthesized (flavor) surroundings would place the ball on a different surface than the one the simulation resolved
- **THEN** the resolved surface SHALL win — the ball SHALL be drawn on the resolved surface, not the flavor one

### Requirement: Stable, Deterministic Hole Appearance

A given hole SHALL look the same every time it is rendered — across shots, rounds, and sessions — with only the pin position changing per round. The synthesized cosmetic layout SHALL be derived deterministically from a stable per-hole layout seed so a course's holes read as permanent, designed venues.

#### Scenario: A hole's layout is stable across rounds

- **WHEN** the same hole is played on different rounds of an event
- **THEN** its fairway shape, hazards, and vegetation SHALL be identical, and only the pin position SHALL change

#### Scenario: Layout is reproducible

- **WHEN** the same hole is rendered in two separate sessions
- **THEN** the synthesized layout SHALL be identical, derived from the same layout seed

### Requirement: Biome-Styled Visual Themes

The 2D hole SHALL be styled by a visual theme selected from the host course's environment classification, so that each biome reads distinctly (grass shades, vegetation type, bunker and hazard styling). The mapping SHALL be: Parkland, Links, Desert, Alpine (Mountain), Heathland (Woodland), and a water-heavy Tropical theme for the Coastal classification.

#### Scenario: Theme follows the course classification

- **WHEN** a hole on a Links course and a hole on a Desert course are rendered
- **THEN** each SHALL use its biome theme — the Links hole with links styling, the Desert hole with desert styling

#### Scenario: Coastal renders as tropical

- **WHEN** a hole on a Coastal-classified course is rendered
- **THEN** it SHALL use the water-heavy Tropical theme

### Requirement: Shot Playback Animation

Each resolved shot SHALL be played back visually on the 2D hole — the ball travelling from its start to its resulting position, ending on the resolved surface — so the player sees the result of the shot they took.

#### Scenario: A shot is animated to its result

- **WHEN** the simulation resolves the player's shot
- **THEN** the 2D view SHALL animate the ball from its origin to a resting position consistent with the resolved carry, remaining distance, and final surface

### Requirement: Flyover Introduction Per Hole

Each hole SHALL be introduced by a brief contextual card carrying the hole's identity (number, par, length) and the event's scene imagery, which SHALL recede to reveal the persistent 2D schematic as the play surface. The scene photograph SHALL serve this introductory and identity role rather than competing with the schematic during play.

#### Scenario: The intro precedes play

- **WHEN** the player reaches a new hole
- **THEN** a brief intro card showing the hole number, par, length, and scene imagery SHALL appear and then give way to the 2D hole for shot-by-shot play
