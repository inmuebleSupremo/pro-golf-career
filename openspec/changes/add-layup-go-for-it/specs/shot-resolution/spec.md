## MODIFIED Requirements

### Requirement: Situational Shot Decisions

The simulation decision policy SHALL choose each shot from the situation — the remaining distance, the ball's lie, the pin placement, the golfer's attributes, and the hole's par — not from a fixed per-golfer strategy alone. From a difficult lie (deep rough, bunker, recovery, trees) the golfer SHALL play conservatively to recover, regardless of disposition; from a clean lie it SHALL keep its disposition. On a scoring approach the golfer SHALL aim a fraction of the way toward the pin determined by its disposition and by shot confidence (the fraction fading to zero as the shot lengthens). On a long approach to a **par 5** (never a tee shot, and never a par 4/3, which have no stroke to spare) the golfer SHALL decide whether to go for the green or lay up: an aggressive disposition, or any golfer who can reach the green comfortably given their own reach, SHALL go for it; otherwise the golfer SHALL lay up to leave a comfortable full-wedge distance, aimed at the green centre. The policy SHALL remain pure and deterministic.

#### Scenario: A difficult lie forces conservative recovery

- **WHEN** the policy decides a shot from a bunker or deep rough
- **THEN** it SHALL play conservatively regardless of the golfer's disposition

#### Scenario: Aggression attacks the pin, conservatism plays the centre

- **WHEN** an aggressive and a conservative golfer decide the same confident approach to a tucked pin
- **THEN** the aggressive golfer SHALL aim toward the pin and the conservative golfer SHALL aim at the green centre

#### Scenario: Pin attack fades with distance

- **WHEN** the same aggressive golfer decides a short approach versus a long approach to a tucked pin
- **THEN** it SHALL aim more toward the pin on the short approach, and play the centre on a long approach

#### Scenario: Lay up or go for it on a par 5

- **WHEN** a golfer faces a long approach to a par-5 green that is not comfortably within their reach
- **THEN** an aggressive golfer SHALL go for the green while a conservative golfer SHALL lay up to a full-wedge distance; on a par 4 or 3, or from the tee, the golfer SHALL always go for the green

#### Scenario: A long hitter goes for a green a short hitter lays up

- **WHEN** a long-hitting and a short-hitting golfer of the same conservative disposition face the same par-5 approach
- **THEN** the long hitter (who can reach comfortably) SHALL go for the green while the short hitter SHALL lay up
