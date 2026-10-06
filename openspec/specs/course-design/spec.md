# course-design Specification

## Purpose
TBD - created by archiving change course-design-foundations. Update Purpose after archive.
## Requirements
### Requirement: Deterministic course design identity

Each newly generated design-aware Course SHALL have a deterministic `CourseDesignProfile` that is distinct from its environment classification and any tournament `CourseSetup`. The profile SHALL express only a compact, composable set of playable venue-intent dimensions: strategic emphasis, width tendency, and recovery severity. It SHALL be stable for the course's generator version and seed, and SHALL influence generated design rather than acting solely as name, texture, or biome metadata.

#### Scenario: Profile is stable and distinct from biome and setup

- **WHEN** a design-aware course is regenerated from the same seed and generator version
- **THEN** it SHALL have the same design profile, while its biome and an event's temporary setup remain separate inputs

#### Scenario: Profile has mechanical effect

- **WHEN** two deterministic design-aware courses have different profile dimensions
- **THEN** their generated plans or bounded generation inputs SHALL show at least one corresponding measurable difference

### Requirement: Coherent eighteen-hole plan

Before individual holes are compiled, a design-aware Course SHALL generate an immutable `CoursePlan` containing eighteen ordered `HoleBrief`s. The plan SHALL preserve par 72 with two par 3s, five par 4s, and two par 5s on each nine; provide at least two par-relative length bands within each par class; contain positional and risk/reward opportunities among its par 4s and par 5s; and avoid more than three adjacent briefs with the same strategic archetype.

#### Scenario: Plan composes both nines deliberately

- **WHEN** a design-aware course plan is generated
- **THEN** each nine SHALL contain two par 3s, five par 4s, and two par 5s in a deterministic non-repetitive order

#### Scenario: Plan contains strategic and length variety

- **WHEN** a design-aware course plan is inspected
- **THEN** its briefs SHALL meet the required length-band and archetype diversity constraints without relying on ambient randomness

### Requirement: Lightweight hole briefs guide current generation

Every design-aware `HoleBrief` SHALL express only currently actionable intent: ordered number, par, par-relative length band, strategic archetype, and inherited recovery intent. The generator SHALL translate that intent into bounded existing generation inputs before canonical geometry compilation. The brief SHALL NOT claim unimplemented routes, target zones, approach-side rules, bunker roles, green contours, or shot controls.

#### Scenario: Brief compiles through canonical terrain

- **WHEN** a V2 hole brief is generated
- **THEN** its bounded length, width, and current hazard/recovery inputs SHALL be compiled through the authoritative existing canonical geometry path

#### Scenario: Future strategic detail is not fabricated

- **WHEN** a brief is inspected before later routing and green-complex changes
- **THEN** it SHALL not represent unimplemented detailed routes, hazard roles, or green-reading mechanics as present functionality

### Requirement: Three archetypes have bounded present-day effects

The design-aware generator SHALL support exactly the `POSITIONAL`, `BALANCED`, and `RISK_REWARD` strategic archetypes in this slice. `POSITIONAL` SHALL bias currently expressible length, width, recovery, and flanking-hazard inputs toward accuracy/recovery pressure; `BALANCED` SHALL use the neutral, profile-composed range; and `RISK_REWARD` SHALL bias eligible par 4s or par 5s toward a short/reachable current range with bounded miss-consequence exposure. These are distributional constraints over the locked deterministic corpus, not a claim that the current generator has multiple routes, chosen lay-ups, or named hazard roles.

#### Scenario: Archetypes produce distinguishable current intent

- **WHEN** the fixed design-aware corpus is generated
- **THEN** positional briefs SHALL have the committed higher standard/long-band share and risk/reward briefs SHALL have the committed higher eligible short/reachable-opportunity share, with balanced briefs using the neutral range

#### Scenario: Routing detail remains deferred

- **WHEN** a risk/reward or positional brief is examined
- **THEN** it SHALL not claim a player-selectable safe/carry route, prescribed landing zone, approach angle, or role-based hazard placement

### Requirement: Differentiation evidence is locked before tuning

Before V2 parameter tuning, maintainers SHALL lock a reviewed deterministic calibration contract derived from the immutable V1 baseline. It SHALL name the fixed 48-coordinate V1 corpus, the all-combinations V2 plan/geometry corpus, the fixed scoring-anchor profiles and round sample, the numerical profile/biome envelopes, minimum directional separations, and repetition allowance. The V1 baseline and calibration contract SHALL not be relaxed by the V2 tuning change itself.

#### Scenario: Intended differentiation is evaluated separately from regression

- **WHEN** V2 calibration is run
- **THEN** planned profile deltas SHALL be judged against their profile-specific contract envelopes, while V1 fixture drift, aggregate out-of-envelope scoring/exposure, invalid geometry, or failed round resolution SHALL be treated as regressions or pathologies

