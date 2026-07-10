# playable-event Specification

## Purpose
TBD - created by archiving change add-playable-event. Update Purpose after archive.
## Requirements
### Requirement: Interactive Player Tournament

The system SHALL provide an interactive tournament session in which the designated player plays their golfer's rounds in a real event while every other competitor is resolved automatically through the shared engine. It SHALL play the event round by round: for each round the player participates in, it SHALL present the player an interactive round for their golfer on the event's course under that round's playing conditions, accept the player's shot-by-shot decisions (or a sim), and resolve the rest of the field automatically for that round.

#### Scenario: The player plays a round and the field auto-resolves around them

- **WHEN** the player plays (or sims) a round of their event
- **THEN** the player's round SHALL be resolved from their own shot decisions and every other competitor's round SHALL be resolved automatically through the shared engine, and all scores SHALL be added to the one tournament

#### Scenario: The cut applies over the combined field

- **WHEN** the second round of the player's event completes
- **THEN** the cut SHALL be evaluated over the combined field including the player, and the player SHALL play the final rounds only if they made the cut

### Requirement: Always Skippable Event

The player SHALL be able to skip play at shot, hole, round, or whole-event granularity — auto-playing the remainder with the same automatic policy the simulation uses — so that no event need be played by hand and the game remains about decades, not shots.

#### Scenario: Simming the rest of the event

- **WHEN** the player chooses to sim the remainder of their event
- **THEN** the remaining rounds (and any playoff) SHALL be auto-played and the event SHALL complete with a full result

### Requirement: Interactive Sudden-Death Playoff

When the player's golfer is tied for the lead after the final round, the player SHALL play the sudden-death playoff interactively — playing each playoff hole for their golfer while the tied rivals are resolved automatically — until a single winner remains, and this SHALL remain skippable. When the player is not among the tied contenders, the playoff SHALL be resolved automatically.

#### Scenario: The player plays a walk-off playoff hole

- **WHEN** the player is tied for the lead after the final round and plays a sudden-death hole
- **THEN** the player's playoff hole SHALL be resolved from their decisions and the tied rivals' holes SHALL be resolved automatically, and the lowest score(s) SHALL advance until one competitor wins

### Requirement: The Played Event Counts

A completed player event SHALL produce the same kind of result as an automatically resolved event and SHALL feed every consumer identically — the World Ranking, the tour's Season Standings, every competitor's Career, the media feed, the statistics archive, prize money and entry/travel costs, and health — so that the player's play changes the world.

#### Scenario: The player's real result updates the world

- **WHEN** the player's event completes
- **THEN** its result SHALL update the ranking, tour standings, careers, media, statistics, economy, and health exactly as an automatically resolved event would, including any win, prize, or record achieved by the player

### Requirement: Fidelity to Automatic Resolution

A fully-simmed player event SHALL produce a tournament result identical to the automatic resolution of the same event — the same finishing order, per-competitor scores, cut, winner, and playoff outcome — for the same field, course, conditions, and seed, so that playing by hand differs from the automatic outcome only by the player's own decisions.

#### Scenario: A simmed event matches automatic resolution

- **WHEN** a player event is simmed in full for a given field, course, conditions, and seed
- **THEN** its tournament result SHALL equal the automatic resolution of the same event, competitor for competitor

