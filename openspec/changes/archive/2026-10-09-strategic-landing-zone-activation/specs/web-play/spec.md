## ADDED Requirements

### Requirement: Truthful non-binding strategic guidance

The play UI SHALL render only server-authored, distinct strategic guidance options in the existing target-guidance controls. It SHALL display each option's role and concise server-authored route/exposure explanation, and MAY display its suggested club/family. Selecting an option SHALL set its literal aim point only; it SHALL not silently alter the player's club, family, shape, or free manual targeting ability.

#### Scenario: Player understands and may override a strategic option

- **WHEN** a V4 shot exposes SAFE and AGGRESSIVE guidance options
- **THEN** the player can see their different server-authored positional/exposure explanations and select either target
- **AND THEN** the player can still choose any legal club/family/shape and a free literal target before submitting.

#### Scenario: No false button triplet

- **WHEN** the server supplies only ordinary PRIMARY guidance
- **THEN** the UI SHALL show the ordinary target without duplicate SAFE/AGGRESSIVE controls
- **AND THEN** it SHALL not infer alternatives from SVG terrain or client calculations.

### Requirement: Availability-backed shot controls and click-first aiming

The play UI SHALL initialize and maintain its non-putting club, family, and shape selection from the current server-authored availability projection. It SHALL preserve a still-legal player selection and SHALL not submit a default combination that the projection marks unavailable. The server remains the final authority for submitted intents. The hole click/tap target is the normal aiming interaction; the UI SHALL retain accessible numerical X/Y precision controls under a compact disclosure and SHALL not render directional aim-nudge buttons.

#### Scenario: A recovery lie changes the legal default without removing legal choice

- **WHEN** the current lie makes a long club or ordinary technique unavailable and a legal recovery combination is projected
- **THEN** the controls select a legal club, family, and shape
- **AND THEN** a player may still choose any other combination that the current projection marks legal.

### Requirement: Expected shot-validation feedback is actionable

When the server rejects a submitted shot with an expected `BAD_REQUEST` validation error, the play UI SHALL display the safe validation message. It SHALL show generic retry feedback for network or unexpected failures and SHALL not expose implementation details from those failures.

#### Scenario: An invalid manual aim is explained

- **WHEN** a player submits an aim point outside the server's planning envelope
- **THEN** the server rejects the shot without resolving it
- **AND THEN** the UI presents the expected validation message so the player can adjust the target.
