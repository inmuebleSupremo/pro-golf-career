## ADDED Requirements

### Requirement: Value Category Classification

Every numerical value used by the simulation SHALL belong to exactly one of five categories: **Attribute**, **Modifier**, **Rating**, **State**, or **Outcome**. The category determines whether a value may be stored, trained, or used as a calculation input.

- **Attribute** — permanent player ability, persisted, on the 0–100 scale; the only category eligible for development.
- **Modifier** — a temporary additive or multiplicative adjustment produced during a calculation; never persisted; expires when its source is removed.
- **Rating** — a derived summary recalculated on demand (e.g., Live Skill Rating); never directly trainable.
- **State** — a temporary player condition (e.g., Fatigue, current hole); mutable, non-permanent.
- **Outcome** — the final result of a calculation (e.g., landing position, score); never fed back into a calculation without first being reclassified as another category.

#### Scenario: Development targets only Attributes

- **WHEN** a development operation is applied to a value that is not classified as an Attribute
- **THEN** the operation SHALL be rejected before any state change occurs

#### Scenario: Outcomes are not reused as raw inputs

- **WHEN** a resolved Outcome (e.g., a landing distance) is required as an input to a subsequent calculation
- **THEN** it MUST first be reclassified into an explicit category (State or Modifier) rather than consumed directly as an Outcome

### Requirement: Fixed Calculation Pipeline

Every gameplay calculation that produces an Outcome SHALL apply its inputs in exactly this order, and SHALL NOT skip or reorder steps:

1. **Base Attribute** — the relevant permanent attribute value(s).
2. **Permanent Career Effects** — durable, career-long adjustments (e.g., long-term injury regression already baked into attributes is excluded; this step covers permanent effect modifiers, not attribute mutation).
3. **Temporary Modifiers** — fatigue, pressure, course mastery, and other expiring adjustments.
4. **Environmental Effects** — wind, rain, temperature, ground firmness, lie.
5. **Controlled Randomness** — a sample drawn from the resulting probability distribution.
6. **Safety-Net Mechanics** — clamping/dampening that reduces unrealistic or excessively punishing results without eliminating poor outcomes.
7. **Final Outcome** — the produced, immutable result.

Steps 1–4 shape the probability distribution; step 5 samples it; step 6 bounds the sample.

#### Scenario: Pipeline order is deterministic and total

- **WHEN** the same inputs are supplied to the pipeline
- **THEN** steps 1–4 SHALL produce an identical probability distribution regardless of invocation

#### Scenario: Randomness cannot precede shaping

- **WHEN** a calculation is performed
- **THEN** the random sample (step 5) SHALL be drawn only after all attribute, modifier, and environmental shaping (steps 1–4) has been applied

#### Scenario: Safety net applies after sampling

- **WHEN** a sampled value would represent an unrealistic or excessively punishing outcome
- **THEN** the safety-net step (step 6) SHALL adjust it, and this adjustment SHALL occur after sampling and before the final outcome is emitted

### Requirement: Mathematical Conventions

All calculations SHALL follow a single set of conventions so that every gameplay system speaks one mathematical language.

- Higher attribute values SHALL always represent better expected performance.
- Positive modifiers SHALL always improve expected outcomes; negative modifiers SHALL always reduce them.
- No calculation SHALL invert these conventions.

#### Scenario: Higher attribute never worsens expectation

- **WHEN** a single attribute is increased with all other inputs held constant
- **THEN** the expected value of the resulting outcome distribution SHALL be equal or better, never worse

#### Scenario: Modifier sign is consistent

- **WHEN** a negative modifier is applied with all other inputs held constant
- **THEN** the expected outcome SHALL be equal or worse, never better

### Requirement: Attribute Scale and Bounds

Permanent attributes SHALL use a common 0–100 scale with an expected professional range of 20–95. Intermediate calculation values MAY temporarily exceed these bounds internally, but no stored attribute SHALL persist outside 0–100.

#### Scenario: Stored attribute stays in range

- **WHEN** a development or regression operation would set a stored attribute outside 0–100
- **THEN** the value SHALL be rejected or clamped to the bound and validated before persistence

#### Scenario: Transient overflow permitted internally

- **WHEN** an intermediate modifier pushes a working value above 100 during a calculation
- **THEN** the calculation MAY proceed internally, but the emitted Outcome and any persisted Attribute SHALL respect their defined bounds
