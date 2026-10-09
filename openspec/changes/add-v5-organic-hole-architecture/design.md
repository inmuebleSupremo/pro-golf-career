## Context

V4 already separates a deterministic `CoursePlan`/`HoleBrief`, V3 spatial semantics, V4 semantic hazards and
authoritative `CourseGeometry`. The V3 semantic model was intentionally conservative: one optional, extremely rare
turn; scalar fairway width; one ellipse green; and a target bridge designed before click-to-aim and V4 target
activation. It is not a suitable permanent limit on course architecture.

The architecture must retain these invariants:

- `sim.*` stays pure, deterministic, immutable-leaning and common to human and AI.
- the generator pin selects retained V1--V5 implementations; restoring a career never interprets an old pin as V5;
- `CourseGeometry` is the only surface/settlement/render authority;
- semantic plans are regenerated from pinned provenance, never serialized as duplicate polygons;
- V5 only describes current endpoint and lie mechanics truthfully.

## Decisions

### D1 — V5 is a retained generator with an architectural plan

V5 is a new version. `CourseArchitectureProfile`, `CourseArchitecturePlan`, and `HoleArchitecturePlan` are pure
generated metadata associated with V5 holes. V1--V4 data models and output remain exact. New-world current-version
adoption occurs only after implementation approval; existing world pins and historical results remain untouched.

The profile is not a finite course-style/template enum. It is a bounded vector of independently generated
tendencies: routing movement, lateral asymmetry, landing-width rhythm, approach openness, green-defence emphasis
and biome-constrained recovery/hazard plausibility. Existing `CourseDesignProfile` still supplies its established
strategic/width/recovery calibration axes; the two are composed, not conflated.

### D2 — Generate constrained candidates, then select an 18-hole composition

For each numbered par/length/archetype brief, V5 derives a bounded set of seed-indexed candidates. Candidate
generation samples continuous values rather than choosing a template: route-knot count/location/deflection,
left/right width stations, landing-area envelopes/offsets, approach corridor, green footprint/aspect/rotation/open
sector, and currently supported hazard/recovery intent. The values are constrained by the brief, course profile,
biome, current reach/target facts and canonical geometry rules.

Candidate evaluation is pure and deterministic. It first rejects invalid polygons, disconnected corridors, invalid
green/pin support, blocked target cores, impossible current progression, unsupported water relief, or an untruthful
obstacle claim. It then scores course-identity fit, route/landing/approach quality and a normalized structural
signature. Course selection maximizes the accepted candidates' score minus pairwise near-duplicate/repetition
penalties, subject to par and difficulty budgets. Candidate ordinal is the final deterministic tie-breaker. Fixed
candidate counts and explicit exhaustion failure avoid ambient retry dependence.

This is intentionally not a global unrestricted optimizer: a deterministic greedy/backtracking bounded selector is
enough for eighteen independently generated candidates and is auditable in a test corpus.

Candidate quality is relational rather than geometric-only. A route turn changes which width station can accept a
normal tee shot; that landing position changes remaining distance and the valid final approach direction; green
entry/protected/bailout sectors then make one existing canonical landing/rough/bunker/water exposure preferable to
another. A candidate with a visually distinct bend but no supported landing, approach or exposure consequence is
rejected or scored as ordinary rather than advertised as strategic. Conversely, a simple forgiving corridor is a
valid selected identity when it supplies deliberate scoring relief and improves course rhythm. V5 does not impose a
quota of dramatic holes.

### D3 — V5 geometry vocabulary stays simple and connected

`HoleArchitecturePlan` replaces V3 planning for V5 only:

| Element | V5 representation and bound |
| --- | --- |
| Route | sampled centreline classified as straight, gentle-moving, dogleg or rare double-dogleg; no self-intersection or branch |
| Fairway | ordered left/right width stations swept into one connected simple corridor; stations can create a neck, widening, diagonal landing area and asymmetric preferred side |
| Landing | ordered safe/primary/aggressive areas with route-relative cores and real next-shot/approach consequences where published |
| Approach | final route direction plus open/protected/bailout sectors and a minimum playable entry corridor |
| Green | deterministic simple validated footprint (not only an ellipse), dimensions/orientation and sector semantics |
| Hazards/recovery | route-, landing- and green-related variable bunker complexes plus biome-plausible canonical tree regions, compiled only to existing truthful endpoint/lie surfaces |

The compiler uses deterministic densely sampled, y-monotone corridors with smooth bounded centreline/width
interpolation and simple polygons. Green contours use low-frequency deterministic radial harmonics rather than
per-vertex noise. It validates topology and canonical precedence rather than adding a mesh, boolean geometry
dependency or a second collision system.

Bunker complexes are not a fixed per-hole count: an ordinary open hole may have none or one key bunker, while a
defended green, narrowing landing area or dogleg can receive separated fairway groups and multi-angle greenside
guards. Their position, side, extent and irregular outline derive from current route/width/approach facts and retain
only existing bunker endpoint/lie behaviour. Trees are canonical `TREES` regions with environment-conditioned
clusters, corridors and openings placed in recovery space; they are neither collision volumes nor claims about
airborne clearance.

### D3a — environment is a course-level composition, not a paint label

Before V5 terrain compilation, a bounded deterministic environmental-character pass ranks the actual eighteen hole
briefs and assigns continuous/seeded enclosure and coastal-exposure tendencies. Parkland retains an open rhythm but
places in-bounds scattered or broken-lined trees on ordinary holes; woodland assigns markedly more enclosed and
wooded corridors while retaining wider openings; links stay substantially open; coastal/links courses assign several
eligible holes a long side-shore relationship. The pass uses hole length, route, recovery and course identity facts,
not a catalogue of named layouts.

Coast is compiled as elongated canonical water/shoreside regions aligned with a selected route side, sometimes with
a bounded inlet near an approach. It is deliberately not an island, bridge, disconnected playable land mass or a
forced-carry claim. A shoreline is only reported when the water geometry materially extends alongside the playable
hole, not when a small isolated water ellipse happens to exist.

Environmental evidence is derived from the actual canonical regions: fully in-bound tree regions, holes with
in-bound/fairway-adjacent trees, broken wooded-corridor holes, fairway versus greenside bunker complexes and their
areas/grouping, water area and shoreline-adjacent holes. Aggregate region totals alone are insufficient.

### D4 — Current target/AI semantics constrain, but do not prevent, variation

V5 exposes a deterministic next viable route/landing target to the current target planner. A target is accepted
only if a legal existing club/family can reach its core and canonical terrain supports it. The existing human
click-to-aim control stays literal and the same planner facts guide AI. Where the current mechanics cannot make a
route alternative meaningful, V5 uses a connected, non-forced corridor and does not advertise a false choice.

V5 may create a dogleg whose preferred landing/approach side matters at endpoint settlement. It may not claim trees
block an inside line, water requires a carry, or a ball crossing terrain during release incurs a penalty. Those are
future physics/AI milestones, not a reason to conceal the physical limitation in artwork or copy.

### D5 — Quality evidence is a committed engineering surface

Implementation adds a pure `CourseQualityReport` (or equivalent test-only diagnostics) and a committed corpus
contract. It compares V4 and V5 across known regression seeds and a locked unseen-seed suite without relaxing V4
history. It records geometry validity, target/relief/pin safety, scoring/penalty/recovery envelopes, routing and
fairway signatures, landing/approach choices, green forms, hazard roles and within-course nearest-neighbour
distance/repetition.

The diagnostic produces a deterministic contact sheet from authoritative effective canonical polygons, with no
client reconstruction. It labels course identity, seed/version, hole number/par/length and normalized signature.
Human review is an acceptance gate: automated thresholds identify repetition/pathology but cannot prove a convincing
course.

Each completion report includes the entire contact sheet and quality report for every stated V4/V5 comparison
course: established regression coordinates plus a locked unseen set, across multiple environment/profile inputs.
It may call out instructive holes but may not cherry-pick them as the reported evidence or exclude weak generated
holes. Review instructions name the generated artifact path/entry page so evaluators can inspect the actual
canonical output before acceptance.

### D6 — Migration and calibration

V5 changes only future new-world generation after adoption. V1--V4 registry fixtures remain exact. V5 needs its
own pre-committed validity, distribution, repetition, round-resolution and scoring envelope. A failure is classified
as historical regression, V5 geometry/playability pathology, V5 diversity failure, or V5 calibration miss; it is
not fixed by weakening prior fixtures or silently modifying a legacy generator.

## Alternatives Rejected

- **More V4 hazard randomness:** density cannot solve the repeated straight-corridor architecture.
- **A catalogue of showcase holes:** cosmetic variation hides repetition and makes unseen seeds predictable.
- **Branching fairways now:** without obstacle/carry authority, apparent forced alternatives would be dishonest and
  AI complexity would grow before base routing is proved.
- **Put all semantics in `CourseGeometry`:** it conflates immutable design intent with terrain authority and makes
  versioned diagnostics/selection fragile.
- **Wait for elevation/flight physics:** it postpones a large visible improvement that current endpoint geometry and
  targeting can honestly support.

## Risks and mitigations

- **Candidate selection adds opaque complexity.** Keep the candidate count, score components, rejection reasons and
  tie-break order fixed and exposed in diagnostics.
- **Richer curves create invalid polygons or unplayable targets.** Validate each candidate before selection and
  assert canonical topology, cores, route progression, pin support and relief.
- **Metrics are gamed by ugly novelty.** Combine structural measures with scoring/playability envelopes and required
  contact-sheet review.
- **Course identity becomes a new template taxonomy.** Use independent axes/continuous parameters and pairwise
  structural distance, not named hole layouts.
- **Visible terrain overpromises physics.** Restrict V5 features to current endpoint/lie consequences; defer flight,
  ground traversal and collision claims.
- **Smooth compilation folds a corridor.** Sample with a monotone longitudinal parameter, bound lateral derivatives
  and retain topology validation; fall back only to a lower-curvature canonical sample, never presentation-only art.

## Migration plan

1. Add V5 model/compiler/evaluator behind an explicit version pin while preserving V1--V4 exact fixtures.
2. Lock V5 corpus, metrics, gallery and calibration envelope before tuning candidate weights.
3. Adopt V5 only for new worlds after automated and manual acceptance; restore prior saves through their original
   pins without data conversion.
4. Keep future obstacle/carry/elevation mechanics in separately approved changes that extend V5 plans rather than
   reinterpreting historical terrain.
