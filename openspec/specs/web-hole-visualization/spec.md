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

The 2D layer SHALL be a truthful representation of the simulation and SHALL NOT contradict any resolved mechanical fact. The hole's canonical terrain, active pin/cup position, and each resolved shot's contact, final playable ball state, distance remaining, and penalty SHALL be represented literally. The client SHALL NOT synthesize which flank a gameplay hazard occupies, dogleg lean, hazard longitudinal position, fairway-width variation, or any other gameplay terrain. Presentation details not represented by canonical geometry — vegetation scatter, texture, lighting, organic edge treatment, and camera treatment — MAY be synthesized only when they cannot imply or obscure a different gameplay surface.

#### Scenario: The pin is drawn at its real position

- **WHEN** the active round's cup has a canonical position
- **THEN** the flag SHALL be drawn at that position

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
