# tournament-play Specification

## Purpose
TBD - created by archiving change add-tournament-engine. Update Purpose after archive.
## Requirements
### Requirement: Tournament Lifecycle

A Tournament SHALL progress through its lifecycle in order: Scheduled → Registration Open → Field Confirmed → Round 1 → Round 2 → Cut Evaluation → Round 3 → Round 4 → Playoff (if required) → Completed. States SHALL NOT be skipped, except that the Playoff stage is entered only when required.

#### Scenario: States occur in order

- **WHEN** a Tournament advances
- **THEN** it SHALL move to the next lifecycle state and SHALL NOT skip an intermediate state

#### Scenario: Playoff only when required

- **WHEN** the final round completes with a single outright leader
- **THEN** the Tournament SHALL proceed to Completed without a Playoff

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

### Requirement: Scoring

Each completed hole SHALL contribute to a competitor's cumulative Tournament score, measured as strokes relative to Course par. Lower scores represent better performance.

#### Scenario: Hole scores accumulate relative to par

- **WHEN** a competitor completes holes
- **THEN** their Tournament score SHALL equal total strokes minus the par of the holes played, updated as holes complete

#### Scenario: Lower is better

- **WHEN** two competitors are compared
- **THEN** the one with the lower cumulative score SHALL rank ahead

### Requirement: Live Leaderboard

A Tournament SHALL maintain a live leaderboard ranking active competitors by cumulative score. It SHALL be recomputed whenever a score changes, and ties SHALL be handled consistently (tied competitors share a position).

#### Scenario: Leaderboard recomputes on score change

- **WHEN** a competitor's score changes
- **THEN** the leaderboard positions SHALL be recomputed

#### Scenario: Ties share a position

- **WHEN** two competitors have identical cumulative scores
- **THEN** they SHALL share the same leaderboard position, applied consistently

### Requirement: Rounds Resolve Under Playing Conditions

Each competitor's round SHALL be resolved under the Tournament's Playing Conditions for that round, applied through the shared round-resolution engine's environment inputs. Playoff holes SHALL use the final round's conditions. A Tournament with no supplied weather SHALL default to calm conditions, preserving standalone-event behaviour.

#### Scenario: A round uses that round's conditions

- **WHEN** a competitor's round is resolved
- **THEN** the round SHALL be resolved under the Tournament's Playing Conditions for that round, applied identically to every competitor of the round

#### Scenario: Calm is the default without weather

- **WHEN** a Tournament is played without supplied weather
- **THEN** every round SHALL resolve under calm conditions, matching prior standalone behaviour

### Requirement: Situational Pressure

A Tournament SHALL apply situational competitive pressure to the golfers it resolves, feeding each competitor a pressure value that the shared shot engine consumes. The pressure SHALL be derived from the competitive situation — the round (none in the opening rounds, building on the closing rounds), the event's prestige (higher prestige, more pressure), and the competitor's contention measured as strokes behind the leader going into the round (most at the lead, decaying to none once out of contention). The same pressure SHALL apply to the whole auto-resolved field and to the interactive human player, computed from the same pre-round standings, and a sudden-death playoff SHALL be resolved at peak pressure. The derivation SHALL be deterministic — no randomness — so a fully-simmed interactive event stays identical to automatic resolution and same-seed worlds reproduce.

#### Scenario: Contenders on the final round feel pressure

- **WHEN** the final round of a high-prestige event is resolved
- **THEN** the competitors near the lead SHALL be resolved under greater pressure than those far behind, and than the opening rounds of the same event

#### Scenario: The interactive player feels the same pressure as the field

- **WHEN** the human player plays a round of their event interactively
- **THEN** they SHALL be resolved under the same situational pressure the automatic path would compute for them from the pre-round standings, so a fully-simmed event matches automatic resolution

#### Scenario: Pressure generation is deterministic

- **WHEN** the same tournament situation is resolved more than once from the same seed
- **THEN** the pressure applied to each competitor SHALL be identical each time

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

