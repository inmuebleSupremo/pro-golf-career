## MODIFIED Requirements

### Requirement: Complete Explainable Outcome

Every resolved shot SHALL produce a complete Outcome containing at minimum: final surface, distance remaining to the pin, lateral result, any hazard entered, penalties incurred, resulting shot count, canonical contact position, and resulting playable settlement position. For a penalty, the outcome SHALL distinguish the reported physical contact from the legal recovery or replay position. The final playable lie SHALL equal canonical `surfaceAt` at the playable settlement position. The Outcome SHALL be explainable — the dominant contributing factors (e.g., strong crosswind, poor lie, aggressive strategy, fatigue, excellent execution) SHALL be recoverable from the resolution.

#### Scenario: Outcome includes spatial settlement

- **WHEN** a shot is resolved on a canonical production hole
- **THEN** the produced Outcome SHALL include finite contact and playable-settlement positions, final surface, distance remaining, penalties, and shot count, with no field left undefined

#### Scenario: Penalty contact and recovery are distinct

- **WHEN** a shot enters water or out of bounds
- **THEN** the Outcome SHALL retain the contact position and identify the distinct legal recovery or replay position used for the next shot

#### Scenario: Dominant factors are recoverable

- **WHEN** a resolved shot deviates notably from its expected result
- **THEN** the resolution SHALL expose the dominant contributing factors so the result can be explained rather than appearing arbitrary
