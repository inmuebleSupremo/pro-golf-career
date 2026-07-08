# world-ranking Specification

## Purpose
TBD - created by archiving change add-world-ranking. Update Purpose after archive.
## Requirements
### Requirement: Current World Ranking

The system SHALL maintain an official World Ranking of eligible golfers. Every eligible golfer SHALL have exactly one current ranking position as of a given date, and positions SHALL be a total order (ties broken deterministically).

#### Scenario: Every eligible golfer has one ranking

- **WHEN** the World Ranking is computed as of a date
- **THEN** every eligible golfer SHALL have exactly one ranking position

#### Scenario: Ranking is an ordering by ranking value

- **WHEN** two golfers have different ranking values
- **THEN** the one with the higher ranking value SHALL hold the better (numerically lower) position

### Requirement: Ranking Eligibility

Only eligible golfers SHALL appear in the active World Ranking. A golfer that becomes ineligible (e.g. retired) SHALL be removed from the active ranking, while their historical ranking records are preserved.

#### Scenario: Ineligible golfer removed from active ranking

- **WHEN** a golfer becomes ineligible
- **THEN** they SHALL no longer appear in the active ranking, and their historical records SHALL remain intact

### Requirement: Ranking Points Award

A completed Tournament SHALL award ranking points to each competitor as a function of finishing position, the Tournament's tier, and field strength. Better finishing positions SHALL award more points; stronger fields and higher tiers SHALL award more points.

#### Scenario: Better finish earns more points

- **WHEN** points are awarded for a completed Tournament
- **THEN** a competitor finishing ahead of another SHALL receive points greater than or equal to that other competitor

#### Scenario: Tier and field strength scale points

- **WHEN** two tournaments with identical finishing positions differ in tier or field strength
- **THEN** the higher-tier or stronger-field tournament SHALL award more points for the same position

#### Scenario: Field strength bootstraps before convergence

- **WHEN** field strength is required but rankings have not yet converged
- **THEN** a defined proxy (e.g. tier-based) SHALL be used so early events award sensible points

### Requirement: Rolling Decay

Ranking points SHALL decay over a rolling window (approximately two years) measured from the Tournament's scheduled date to an as-of date. A golfer's ranking value SHALL be the sum of their decayed points; points older than the window SHALL contribute nothing.

#### Scenario: Older results contribute less

- **WHEN** a golfer's ranking value is computed as of a later date
- **THEN** points from older tournaments SHALL contribute less than equivalent points from recent tournaments

#### Scenario: Expired points drop out

- **WHEN** a tournament's points are older than the rolling window relative to the as-of date
- **THEN** they SHALL contribute nothing to the ranking value

### Requirement: Ranking Integrity and Reproducibility

Every golfer SHALL be ranked by the same methodology regardless of control type, and the ranking SHALL be deterministic: the same set of tournament results and the same as-of date SHALL always produce the same ranking.

#### Scenario: Identical methodology for all control types

- **WHEN** a human-controlled and a simulation-controlled golfer have identical results
- **THEN** they SHALL receive identical ranking points and standing

#### Scenario: Reproducible ranking

- **WHEN** the same set of tournament results and as-of date are supplied twice
- **THEN** the resulting ranking SHALL be identical

### Requirement: Ranking Domain Boundary

The Ranking System SHALL be analytical only. It SHALL NOT modify player attributes, tournament outcomes, or any gameplay state. It consumes tournament results and produces standings.

#### Scenario: Ranking never mutates gameplay

- **WHEN** rankings are computed or updated
- **THEN** no player attribute or tournament result SHALL be changed by the ranking system

