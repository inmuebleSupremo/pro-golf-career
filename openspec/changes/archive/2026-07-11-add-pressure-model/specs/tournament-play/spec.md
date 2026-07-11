## ADDED Requirements

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
