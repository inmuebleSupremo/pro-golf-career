## MODIFIED Requirements

### Requirement: Create a Custom Golfer

The player SHALL be able to create a custom golfer by choosing a first name, last name, nationality, start age, starting playing-style archetype, Starting Background, and optional handedness. An omitted background SHALL default to Elite Amateur and omitted handedness SHALL default to `RIGHT` at the application/API boundary. The created golfer SHALL be human-controlled and SHALL become the player's designated golfer. Creation SHALL be deterministic from these inputs.

#### Scenario: A created golfer is human-controlled and designated

- **WHEN** the player creates a golfer with a chosen identity, age, Playing Style, and Starting Background
- **THEN** a human-controlled golfer SHALL be created from those choices and SHALL become the player's designated golfer

#### Scenario: Creation is deterministic

- **WHEN** two golfers are created with the same name, nationality, start age, Playing Style, Starting Background, and handedness
- **THEN** they SHALL have identical starting attributes and identity

#### Scenario: Omitted options remain compatible

- **WHEN** an older caller creates a golfer without Starting Background or handedness
- **THEN** the golfer SHALL receive the Elite Amateur starting build and `RIGHT` handedness
- **AND** all other creation inputs SHALL retain their established meanings

### Requirement: Archetype Shapes the Starting Build

A created golfer's playing-style archetype SHALL shape the relative starting attributes and potential: strengths SHALL start higher, weaknesses SHALL start lower, and an All-Rounder SHALL start balanced. Starting Background SHALL adjust initial ability without altering the archetype's potential or erasing its relative strengths and weaknesses. Start age SHALL continue to affect initial ability for every background.

#### Scenario: A specialist retains its shape across backgrounds

- **WHEN** golfers of the same age and specialist Playing Style are created with different Starting Backgrounds
- **THEN** each SHALL retain that style's relative strengths and weaknesses and the same potential

#### Scenario: An All-Rounder starts balanced

- **WHEN** a golfer is created with the All-Rounder Playing Style at any Starting Background
- **THEN** its starting attributes SHALL remain balanced

#### Scenario: Age remains meaningful

- **WHEN** otherwise identical golfers choose different allowed starting ages
- **THEN** their age-based starting ability SHALL differ under the chosen background

## ADDED Requirements

### Requirement: Starting Background Sets Initial Ability

The creator SHALL offer Club Champion, Elite Amateur, and Highly Rated Prospect as distinct initial-ability levels, ordered from challenging to stronger. Elite Amateur SHALL preserve the pre-change creation result for the same age and Playing Style. Background SHALL NOT raise potential, change tour entry, or introduce ongoing background-specific rules. Numerical offsets SHALL be calibrated against the age-18 All-Rounder targets of approximately 60, 63, and 69–70.

#### Scenario: Three distinct openings

- **WHEN** three otherwise identical age-18 All-Rounders choose one background each
- **THEN** Club Champion SHALL start below Elite Amateur, which SHALL start below Highly Rated Prospect
- **AND** Elite Amateur SHALL match the former starting attributes and potential

#### Scenario: Background does not alter established careers

- **WHEN** a career created before this choice is loaded
- **THEN** its existing starting and current attributes and potential SHALL remain as saved
