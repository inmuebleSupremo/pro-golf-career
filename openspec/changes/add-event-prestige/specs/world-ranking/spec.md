## MODIFIED Requirements

### Requirement: Ranking Points Award

A completed Tournament SHALL award ranking points to each competitor as a function of finishing position, the Tournament's tier, **the Tournament's event prestige**, and field strength. Better finishing positions SHALL award more points; stronger fields, higher tiers, and higher prestige SHALL award more points. When prestige is unspecified it SHALL be treated as Regular (the neutral weight), preserving prior point awards.

#### Scenario: Better finish earns more points

- **WHEN** points are awarded for a completed Tournament
- **THEN** a competitor finishing ahead of another SHALL receive points greater than or equal to that other competitor

#### Scenario: Tier and field strength scale points

- **WHEN** two tournaments with identical finishing positions differ in tier or field strength
- **THEN** the higher-tier or stronger-field tournament SHALL award more points for the same position

#### Scenario: Prestige scales points

- **WHEN** two tournaments with identical tier, field strength, and finishing position differ in event prestige
- **THEN** the higher-prestige tournament SHALL award more points, so a major awards the most and a regular event the least

#### Scenario: Field strength bootstraps before convergence

- **WHEN** field strength is required but rankings have not yet converged
- **THEN** a defined proxy (e.g. tier-based) SHALL be used so early events award sensible points
