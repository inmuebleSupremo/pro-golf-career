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

Every design-aware `HoleBrief` SHALL express stable generation-facing intent: ordered number, par, par-relative length band, strategic archetype, and inherited recovery intent. V2 SHALL translate only its bounded scalar inputs before canonical geometry compilation. V3 MAY translate the same brief into route, landing-zone, approach-side, and green-complex semantics through the separately defined strategic-hole-routing capability. A brief itself SHALL not become canonical terrain, a player control, a named hazard-role model, or a shot-control model.

#### Scenario: Version-appropriate brief compilation

- **WHEN** the same class of brief is compiled by V2 and V3
- **THEN** V2 SHALL preserve its bounded scalar behavior while V3 MAY add only the approved semantic routing/green layer before canonical compilation

#### Scenario: Brief does not overtake later systems

- **WHEN** a V3 brief-derived semantic plan is inspected
- **THEN** it SHALL not claim player free aim, club-specific targets, shot type/shape/trajectory, role-based hazards, contours, elevation, or spatial putting

### Requirement: Three archetypes have bounded present-day effects

The design-aware generator SHALL support `POSITIONAL`, `BALANCED`, and `RISK_REWARD` strategic archetypes. V2 SHALL retain its existing bounded scalar expressions. V3 SHALL additionally map them to the constrained spatial meanings defined by strategic-hole-routing: positional controlled/preferred-side play with a safer inferior alternative where applicable, balanced neutral primary play, and an eligible risk/reward safe/aggressive spatial choice. No version SHALL imply a branch network, unrestricted doglegs, or player-selectable route controls.

#### Scenario: V3 archetype remains bounded

- **WHEN** the fixed V3 corpus is generated
- **THEN** its archetype mappings SHALL satisfy the committed spatial-plan and feasibility contract without creating unapproved route or shot mechanics

### Requirement: Differentiation evidence is locked before tuning

Before V2 parameter tuning, maintainers SHALL lock a reviewed deterministic calibration contract derived from the immutable V1 baseline. It SHALL name the fixed 48-coordinate V1 corpus, the all-combinations V2 plan/geometry corpus, the fixed scoring-anchor profiles and round sample, the numerical profile/biome envelopes, minimum directional separations, and repetition allowance. The V1 baseline and calibration contract SHALL not be relaxed by the V2 tuning change itself.

#### Scenario: Intended differentiation is evaluated separately from regression

- **WHEN** V2 calibration is run
- **THEN** planned profile deltas SHALL be judged against their profile-specific contract envelopes, while V1 fixture drift, aggregate out-of-envelope scoring/exposure, invalid geometry, or failed round resolution SHALL be treated as regressions or pathologies

