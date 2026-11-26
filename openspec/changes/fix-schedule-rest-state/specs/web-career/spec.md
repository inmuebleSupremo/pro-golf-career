## ADDED Requirements

### Requirement: Schedule Rest Decision Reflects Career State

The Schedule page SHALL display the player's standing blanket rest choice from the current career state. It SHALL refresh that choice after a successful change and SHALL not present an assumed playing decision while the authoritative choice is unavailable. Fitness availability remains a separate physical status.

#### Scenario: Returning to Schedule while resting

- **WHEN** a player chooses blanket rest and returns to Schedule after navigation, remount, browser refresh, or reopening a save containing that choice
- **THEN** Schedule SHALL show the resting decision and offer to resume playing

#### Scenario: Failed rest change

- **WHEN** changing the blanket rest choice fails
- **THEN** Schedule SHALL continue to display the last authoritative decision
