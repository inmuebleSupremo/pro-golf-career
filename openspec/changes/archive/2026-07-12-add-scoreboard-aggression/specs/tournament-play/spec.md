## ADDED Requirements

### Requirement: Scoreboard-Aware Strategy

On the closing rounds a competitor's shot strategy SHALL be bent by their position on the leaderboard, rather than always being their innate disposition. A golfer far enough behind the leader SHALL press — play aggressively to make up ground — regardless of disposition; a front-runner leading the field by a comfortable margin SHALL protect the lead by playing conservatively; a golfer in the pack, and every competitor in an opening round, SHALL play their innate disposition. The adjustment SHALL be derived from the pre-round standings, applied to the whole auto-resolved field and to the interactive human player identically, and SHALL be deterministic — no randomness — so a fully-simmed interactive event stays identical to automatic resolution.

#### Scenario: A chaser presses on the closing round

- **WHEN** a golfer well behind the lead plays a closing round
- **THEN** they SHALL play aggressively regardless of their innate disposition

#### Scenario: A comfortable leader protects

- **WHEN** a golfer leading the field by a comfortable margin plays a closing round
- **THEN** they SHALL play conservatively regardless of their innate disposition

#### Scenario: The pack and opening rounds keep the disposition

- **WHEN** a golfer is neither far behind nor comfortably leading, or the round is an opening round
- **THEN** they SHALL play their innate disposition

#### Scenario: The interactive player bends the same way

- **WHEN** the human player plays a closing round of their event
- **THEN** their strategy SHALL be bent by the scoreboard exactly as the automatic path would compute it from the pre-round standings, so a fully-simmed event matches automatic resolution
