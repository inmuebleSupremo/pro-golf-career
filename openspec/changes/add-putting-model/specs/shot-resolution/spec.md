## ADDED Requirements

### Requirement: Putting Resolution

A shot played from the green SHALL be resolved by a dedicated putting model rather than the full ball-flight model. The putting model SHALL determine whether the ball is holed from an explicit make probability that increases as distance to the hole decreases and as putting skill increases, so that short putts hole out near-certainly and holing rates fall off realistically with distance. A putt that is not holed SHALL finish a short distance from the hole that converges toward it, so that a hole always holes out in a realistic number of putts without reaching the per-hole shot cap. Because the ball rolls along the green, a putt SHALL be immune to wind and to lie penalties that apply to full ball-flight shots. A putt SHALL still be resolved through the single shared, deterministic model and SHALL produce a complete outcome like any other shot.

#### Scenario: Short putts hole out near-certainly

- **WHEN** a putt is resolved from tap-in range
- **THEN** it SHALL be holed with very high probability, and a hole SHALL NOT accumulate an unrealistic number of putts or reach the per-hole shot cap

#### Scenario: Holing rate falls off with distance and rises with skill

- **WHEN** putts are resolved over a large sample
- **THEN** the fraction holed SHALL decrease as the distance to the hole increases, and a golfer with higher putting attributes SHALL hole a greater fraction than a golfer with lower putting attributes from the same distance

#### Scenario: Putts are immune to wind and lie

- **WHEN** the same putt is resolved under calm conditions and under strong wind (or a poor course lie)
- **THEN** the putt outcome distribution SHALL be unchanged, because a putt is sheltered from wind and played from the putting surface

#### Scenario: A missed putt leaves a converging tap-in

- **WHEN** a putt is not holed
- **THEN** the ball SHALL finish nearer the hole than it started, leaving a distinct short putt, so the hole holes out in a realistic number of strokes
