## ADDED Requirements

### Requirement: Cut Evaluation

After Round 2, a Tournament with a cut SHALL evaluate it exactly once, using identical criteria for every competitor. Competitors meeting the cut advance to Round 3; those failing are eliminated from further competitive play. The cut result SHALL be permanently recorded. Formats without a cut SHALL omit this stage.

#### Scenario: Cut evaluated once after Round 2

- **WHEN** Round 2 completes in a tournament with a cut
- **THEN** the cut SHALL be evaluated exactly once, and not again

#### Scenario: Cut criteria are identical for all

- **WHEN** the cut is applied
- **THEN** every competitor SHALL be evaluated by the same criteria (top N plus ties advance)

#### Scenario: Missed-cut competitors are eliminated

- **WHEN** a competitor fails the cut
- **THEN** they SHALL not play Rounds 3–4 and their result SHALL record the missed cut

### Requirement: Playoff Resolution

If two or more competitors are tied for first place after Round 4, the Tournament SHALL conduct a playoff that continues until exactly one winner is determined. A Tournament SHALL NOT complete without a single winner.

#### Scenario: Tie for first triggers a playoff

- **WHEN** the final round ends with multiple competitors tied for the lead
- **THEN** a playoff SHALL be conducted among exactly those tied competitors

#### Scenario: Playoff yields exactly one winner

- **WHEN** a playoff is conducted
- **THEN** it SHALL continue until exactly one competitor remains ahead, who is the winner

#### Scenario: Playoff is reproducible

- **WHEN** the same tied competitors and seed are used
- **THEN** the playoff SHALL produce the same winner
