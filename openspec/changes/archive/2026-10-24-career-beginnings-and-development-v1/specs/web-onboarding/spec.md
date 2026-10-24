## ADDED Requirements

### Requirement: Starting Background Choice

The existing golfer-creation flow SHALL offer Club Champion, Elite Amateur, and Highly Rated Prospect as one choice separate from Playing Style and starting age. It SHALL explain their relative starting challenge and that Playing Style shapes strengths and weaknesses. Elite Amateur SHALL be the standard/default choice. No new onboarding screen SHALL be required.

#### Scenario: Background choices are understandable

- **WHEN** the player reviews the creator
- **THEN** all three backgrounds SHALL be available with concise descriptions distinguishing a challenging climb, the standard journey, and a stronger start
- **AND** Playing Style and starting age SHALL remain independently selectable

#### Scenario: Chosen background is submitted

- **WHEN** the player creates a golfer with a selected Starting Background
- **THEN** the existing career-creation flow SHALL submit it with the other golfer choices and persist the resulting career

#### Scenario: Standard choice remains available

- **WHEN** the player accepts the standard/default Starting Background
- **THEN** the new golfer SHALL have the Elite Amateur initial build for the chosen age and Playing Style
