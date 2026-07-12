## ADDED Requirements

### Requirement: Penalty Hazard Recovery

When a resolved shot finishes in a penalty hazard, the round loop SHALL apply a recovery rule determined by the **kind** of hazard, adding exactly one penalty stroke in every case. The two hazard kinds SHALL recover differently:

- **Out of bounds** (and equivalently a lost ball) SHALL use **stroke-and-distance**: the next shot replays from the same spot the hazard shot was played from, losing the distance that shot gained.
- **Water** SHALL use a **water-drop**: the ball is dropped near where it entered the hazard and played forward, so the next shot's distance to the pin is the hazard shot's own distance-remaining plus a small fixed setback representing near-edge and lateral relief, and it is played from a rough lie. The drop SHALL advance the ball toward the hole relative to the previous spot (a water carry costs a stroke and some distance, not a full shot).

This rule SHALL be applied identically by every entry point that resolves a sequence of shots (`resolveRound` and the interactive playable round/hole), so distribution and fidelity equivalence across entry points is preserved.

#### Scenario: Out of bounds replays from the previous spot

- **WHEN** a shot finishes out of bounds
- **THEN** one penalty stroke SHALL be added and the next shot SHALL be played from the same spot as the shot that went out of bounds (stroke-and-distance)

#### Scenario: Water is dropped near the hazard and played forward

- **WHEN** a shot finishes in water
- **THEN** one penalty stroke SHALL be added and the next shot SHALL be played from a dropped position nearer the hole than the previous spot — its distance to the pin equal to the water shot's distance-remaining plus a small fixed setback — from a rough lie

#### Scenario: Same recovery in every resolution entry point

- **WHEN** the same hazard outcome occurs under `resolveRound` versus an interactive playable round or hole for the same inputs and seed
- **THEN** the recovery rule applied SHALL be identical, so a fully-simmed round remains identical to the automatic round resolution
