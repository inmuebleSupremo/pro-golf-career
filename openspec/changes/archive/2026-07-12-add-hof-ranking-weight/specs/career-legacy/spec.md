## MODIFIED Requirements

### Requirement: Hall-of-Fame Biennial Election

Induction into the Hall of Fame SHALL occur only through a periodic election held once every fixed number of seasons (the election cycle), never automatically. In an election, the world SHALL consider every golfer that is not already inducted and that meets the baseline eligibility — **whether active or retired** — and SHALL induct only the single highest-scored candidate that cycle. Golfers eligible but not selected SHALL remain candidates for future elections. Induction SHALL be permanent and SHALL be announced.

The candidate score SHALL weight a career by achievement prestige, favouring majors most heavily, then high-importance (signature) events, then regular professional wins, then development-tier (amateur) wins. The score SHALL additionally reward **ranking dominance** — a peak-position bonus that is greatest for reaching World #1, and a weight for each season finishing at World #1 — and career **earnings** as a small credential, so a golfer who reigned at the top of the world outscores a compiler of the same win total. Ranking dominance and earnings affect only the score, never the baseline eligibility. The election SHALL be deterministic: the same recorded results SHALL always produce the same induction, with ties broken by a stable rule.

#### Scenario: Elections are periodic, not automatic

- **WHEN** a golfer first meets the baseline eligibility in a non-election season
- **THEN** they SHALL NOT be inducted until the next election season

#### Scenario: Only the top candidate is inducted per cycle

- **WHEN** an election runs with several baseline-eligible, not-yet-inducted candidates
- **THEN** exactly one — the highest-scored — SHALL be inducted, and the others SHALL remain candidates for the next election

#### Scenario: Active and retired golfers are both electable

- **WHEN** an election runs
- **THEN** both active and retired golfers meeting the baseline SHALL be considered

#### Scenario: Score favours prestige

- **WHEN** two eligible careers are scored
- **THEN** the one with greater prestige-weighted achievement (majors weighted above signature events, above regular wins, above development-tier wins) SHALL score higher

#### Scenario: Ranking dominance and earnings raise the score

- **WHEN** two eligible careers have the same wins but one reigned at the top of the world (a higher career-high ranking and more seasons at World #1) or earned more
- **THEN** the more dominant / higher-earning career SHALL score higher, while neither ranking nor earnings changes whether a career meets the baseline

#### Scenario: Induction is permanent and reflected in goals

- **WHEN** a golfer is inducted
- **THEN** their induction SHALL persist, and a career goal of reaching the Hall of Fame SHALL be considered achieved only once they are inducted (not merely eligible)
