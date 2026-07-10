## MODIFIED Requirements

### Requirement: Four-Round Play Through the Shared Engine

Every competitor's round SHALL be resolved through the shared round-resolution engine — the same path for all control types, with no separate human or simulation scoring — **except that a Tournament MAY designate exactly one interactive competitor whose per-round score is supplied externally rather than computed by the shared resolver.** The interactive competitor's externally supplied score SHALL be treated identically to a computed score for all cut, leaderboard, playoff, and completion purposes; every other competitor SHALL still be resolved through the shared engine. When no interactive competitor is designated, resolution SHALL be unchanged. A Tournament consists of four rounds; scores carry forward between rounds; completed rounds are immutable.

#### Scenario: All competitors use the shared engine

- **WHEN** a round is played and no interactive competitor is designated
- **THEN** every active competitor's hole scores SHALL be produced by the shared round resolver, with no control-type-specific scoring

#### Scenario: An interactive competitor's round score is supplied externally

- **WHEN** a round is played and one competitor has been designated interactive with a submitted score for that round
- **THEN** that competitor's round score SHALL be the submitted score and every other active competitor's round SHALL be produced by the shared round resolver

#### Scenario: Scores carry forward and completed rounds are immutable

- **WHEN** a round completes
- **THEN** its scores SHALL be added to the running totals and SHALL NOT change afterward
