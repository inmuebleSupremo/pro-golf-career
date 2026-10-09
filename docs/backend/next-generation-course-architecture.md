# Next-Generation Procedural Course and Hole Architecture

## Purpose

This programme replaces the V4 assumption that a hole is principally a length, a narrow route, and one or two
hazard ellipses. Its outcome is a deterministic, backend-authoritative course generator that composes eighteen
related but individually recognisable golf holes. It is a programme design, not an implementation specification;
the first implementable slice is `add-v5-organic-hole-architecture`.

## Current constraint diagnosis

V4 has valuable foundations: a pinned deterministic version, immutable course plan and profile, route-relative
landing zones, canonical terrain polygons, semantic hazard provenance, and an authoritative renderer. It is not
yet an architectural generator.

- `V3HolePlanner` permits zero or one intermediate route anchor, but chooses an eligible dogleg only 0.5--2% of
  the time. Nearly every ordinary route is therefore a tee-to-green corridor.
- The compiler sweeps one symmetrical route corridor. Its only material fairway changes are local scalar changes
  around landing zones and the final approach; it cannot express independently shaped left/right landing space,
  a neck, a diagonal landing area, or a changing line of play.
- A green is a rotated ellipse. Its semantic sides exist, but there is no footprint vocabulary or approach-space
  evaluation beyond that oval.
- V4 hazards are deliberately bounded to a small number of isolated ellipse-like features. They make selected
  surfaces meaningful, but cannot form a coherent fairway/green complex or substantially vary hole identity.
- The profile is a useful three-axis calibration input, but its recurring archetype/length pattern is too small
  to compose a course's rhythm or prevent structurally similar holes within the same eighteen.
- Current physics is endpoint/contact based. Trees are difficult lies, not blocking volumes; water is not an
  airborne forced carry; ground release does not yet traverse and resolve intervening surfaces. The generator
  must not represent those limitations as hard architectural constraints.

## Target architecture

The pipeline stays simulation-pure and deterministic. It becomes a constrained generate--evaluate--select
pipeline rather than a catalogue lookup:

```text
seed + generator pin + biome
  -> CourseArchitectureProfile + composition budget
  -> independent latent hole candidates (continuous routing, width, approach, green and hazard parameters)
  -> pure feasibility / playability / diversity evaluation
  -> deterministic course-level selection of 18 compatible candidates
  -> immutable HoleArchitecturePlan
  -> canonical terrain compiler
  -> current settlement, AI/default targets and canonical SVG
```

`CourseGeometry` remains the sole authority for surface lookup, settlement and rendering. A plan explains why a
layout exists; it never becomes a second terrain model or a saved polygon copy.

### Course identity and composition

V5 adds a deterministic `CourseArchitectureProfile` beside the retained V2 design profile. It is a compact set
of independent, continuously parameterised tendencies, not a named-style template: routing movement, landing
width rhythm, approach openness, green-defence emphasis, and plausible biome surface mix. It supplies course
budgets and correlations, such as a links venue favouring wider, exposed approaches and a woodland venue favouring
framed recovery edges. It does not prescribe “the five hole types” a course must repeat.

The course planner generates an oversubscribed deterministic pool for each numbered par/length brief. Each
candidate is produced from independent seed-derived values for route knots, left/right width stations, landing
areas, green footprint/aspect/orientation, supported hazard intent and safe recovery sectors. A pure score accepts
only valid candidates and ranks combinations by identity fit, playable routing, difficulty envelope and distance
from already selected holes. Stable tie-breaking uses the candidate seed/ordinal, so retry count and platform do
not affect output. There is a bounded candidate budget and explicit failure rather than ambient random retry.

Golf quality is relational: a route affects viable tee-shot space; width and landing choice affect remaining
distance and angle; approach/green sectors affect whether that position is genuinely preferable; and existing
endpoint surfaces affect supported exposure. A curve or relocated hazard with no such consequence is not a
strategic design success. Simple forgiving holes remain deliberate rhythm and contrast.

### Flexible hole plans

`HoleArchitecturePlan` supersedes the intentionally narrow V3 plan for V5 only. It contains a centreline with
bounded smooth segments and zero, one, or two directional changes; ordered landing areas; asymmetric width
stations; an approach corridor; a simple green footprint plus declared open/protected/bailout sectors; and V4-style
supported hazard intent. V5 uses one connected playable fairway corridor, not a branching route network.

The first V5 compiler may use sampled polylines and simple polygons/convex or deliberately validated simple green
footprints. It must not add a mesh, generic boolean geometry engine, authored templates, or a second collision
system. It will allow ordinary straight holes, gentle movement, meaningful doglegs and varied approach angles, but
will not claim a compulsory carry or a tree-blocked shortcut before those mechanics exist.

### No template cloning

An archetype remains a design constraint, not a layout class. Candidate differences must come from several
independent measured dimensions: normalized centreline curvature/turn location, left/right width profile,
landing-area placement and approach advantage, green compactness/orientation/open sector, hazard-role/anchor
distribution, recovery-side mix, length and par. Course selection applies minimum separation across a weighted
feature vector and a repetition penalty for near-duplicate local geometry. A useful showcase hole belongs in the
test gallery as a threshold example only; no generator branch may select it or a cosmetic derivative as a template.

## Quality evidence and human review

A pure `CourseQualityReport` must be generated for a committed corpus containing known regression coordinates and
previously unseen deterministic coordinates. It records validity, playability and calibration facts plus these
version-comparable measures:

| Dimension | Examples of measured evidence |
| --- | --- |
| Routing | normalized route length/chord ratio, cumulative turn, turn count/location/direction, approach angle |
| Fairway/landing form | sampled left/right widths, width variance, asymmetry, neck/widen locations, landing-area count and separation |
| Green/approach | footprint compactness/aspect/orientation, open/protected/bailout sectors, entry width and approach alignment |
| Strategy | safe/primary/aggressive availability, supported exposure differential, next-shot distance differential, guard/approach challenge |
| Hazards/recovery | role/surface/anchor mix, distance to landing/green sectors, reachable exposure, relief and recovery facts |
| Course composition | nearest-neighbour structural distance, repeated-signature count, par/length rhythm, identity-axis adherence and difficulty distribution |
| Safeguards | simple polygons, clear target cores, connected playable corridor, pin eligibility, legal relief, scoring/penalty/recovery envelopes |

The diagnostic additionally emits a deterministic SVG/PNG-ready contact sheet from the same effective canonical
geometry the game renders: one labelled mini-map per hole plus course identity and quality summary. Reviewers use
fixed known seeds to catch regression and a version-controlled unseen-seed set revealed only when a corpus is
locked. Metrics flag suspicious sameness or pathology; human review decides whether a contact sheet reads as a
credible, coherent course rather than as a statistical pass.
Every reported V4/V5 course appears as a full 18-hole sheet, including weak examples; showpieces can be annotated
but never selected as the evidence set or fed back as source templates.

## Gameplay dependency boundary

V5 can safely improve connected fairways, landing opportunities, approach angles, green footprints, endpoint
hazards and AI/default route targets now. It must describe a water feature only as an endpoint/contact risk and a
tree/recovery area only as an existing lie consequence.

| Capability | Dependency | Planned milestone |
| --- | --- | --- |
| More varied connected routing, landings, approaches and greens | V5 plan/compiler, target selection and current endpoint settlement | 1 |
| Hazard complexes whose ground crossing matters | authoritative ground-path/surface traversal and recovery validation | 2 |
| Forced carries, creek crossings and tree clearance/collision | sampled flight/ground intersection against canonical feature volumes | 3 |
| Richer club/route/target policy | candidate facts plus capability-aware AI; no future RNG | 1, then 3--4 |
| Elevation, slope, contour and lie physics | terrain/elevation field and resolver/AI integration | 4 |

## Delivery sequence

1. **V5 organic hole architecture.** Add course identity axes, candidate generation/evaluation/selection,
   asymmetric connected fairway/landing/approach geometry, varied simple green footprints, compatible target
   progression, contact sheets and variety metrics. Player-visible result: ordinary courses visibly contain
   straight, moving and dogleg holes with varied landing/approach/green character. It uses only mechanics that
   already exist and creates no fictional obstacle.
2. **Hazard complexes and ground-surface traversal.** Extend canonical feature plans from isolated guards to
   coherent bunker/waste/recovery/water complexes only where endpoint and ground movement can be resolved
   truthfully. Add release-path interaction, relief and calibrated hazard exposure. Player-visible result:
   landing and release choices around hazards have reliable consequences. Main risk: current rollout calibration.
3. **Flight, carry and obstacle authority.** Add deterministic airborne/ground segment intersection, tree/obstacle
   volumes, carry thresholds and explicit recovery rules; then enable true forced carries, corner-cut consequences
   and alternate routes. Player-visible result: a visible obstacle or water crossing means what it appears to
   mean. Main risk: broad scoring and AI-policy recalibration.
4. **Terrain/elevation and strategic refinement.** Add an authoritative terrain field for elevation, slopes and
   green contours, integrate club/route AI with those facts, and refine course-level difficulty/identity selection.
   Player-visible result: terrain and approach direction change shot outcomes and course management. Main risk:
   putting and condition calibration; this is deliberately not a prerequisite for V5.

Each is a separate retained generator/physics change with its own regression corpus. Historical V1--V4 courses
remain pinned and regenerate unchanged; V5 is selected only for new worlds after explicit approval.

## Landscape-aware corrective architecture (V6 retained successor)

Visual review of the V5 gallery confirmed that organic corridors are necessary but insufficient: a course cannot
read as coastal, woodland or parkland when every hole is generated in a private local coordinate system and framed
only by its playable boundary. Increasing isolated tree, bunker or water polygons cannot create a coherent coast,
woodland clearing or managed parkland field.

V6 implements a deterministic `CourseLandscapePlan` in shared course-yard coordinates. It places all
eighteen local holes into one bounded course frame and generates a small set of continuous landscape systems before
final hole selection: coastline/bay and water side, large woodland masses/clearings, parkland fields/copses, open
links heath/dune ground, and optional lakes. Hole candidates receive the relevant local samples and must respond to
them in routing, fairway width, recovery side, green placement and bunker complex planning. Placements are selected
from bounded seed-derived candidates with non-overlap, transition-distance and landscape-relationship constraints;
they are not selected from a course-map or hole-template catalogue.

The authority boundary remains strict. `CourseGeometry` owns every in-play surface and all resolver consequences.
`CourseLandscapePlan` owns immutable non-playable context and local/course transforms. Renderer detail may be
deterministically derived only from supplied landscape masks and stable seeds. Where a context feature becomes
visible inside playable land, its gameplay surface must be compiled from the same shared feature or the context must
be masked there. Context never creates tree collision, a forced carry, an island/bridge rule, elevation or ground
traversal behaviour.

The gallery provides two views of the same model: a complete course-scale map showing shared features and
all placed holes, plus padded context-aware local maps corresponding to normal play rendering. Visual acceptance is
that several ordinary holes visibly share one coastline, woodland mass or parkland system—not merely that their
per-hole feature counts differ.

### Retained versioning

V5 remains frozen as its own explicit generator provenance. V6 is the landscape-aware successor and regenerates its
shared context, placements and local canonical terrain from the existing seed hierarchy. `CURRENT_GENERATOR_VERSION`
remains V5 until V6 completes separate manual visual/gameplay acceptance; restoring V1--V5 saves never substitutes
V6 or changes historical geometry.

## Approval decision

Approve the V5 semantic representation as a **single connected corridor with up to two bounded directional
changes and asymmetric width stations**, rather than adopting branch networks in the first slice. It delivers a
large ordinary-hole variety increase while keeping current human/AI target semantics honest. Branching fairways
and mechanically mandatory shortcut/carry choices should wait for milestone 3's collision/carry authority.

## 9 October 2026 — V6 experimental checkpoint and visual review result

V6 is preserved as an experimental WIP checkpoint only. Manual visual review rejected the landscape-aware
prototype: its full-course gallery still reads as an artificial, regimented arrangement of holes rather than a
believable golf course. It is not approved for integration, adoption as `CURRENT_GENERATOR_VERSION`, OpenSpec
archival, or further implementation without a new design decision. V5 organic geometry remains a separate,
potentially valuable foundation rather than evidence that V6 is accepted.

The six user-authored SVG references in `docs/course-holes` — `desert001.svg`, `heathland001.svg`,
`links001.svg`, `mountain001.svg`, `parkland001.svg`, and `tropical001.svg` — are central visual and
architectural references. They are not generator templates. The next session begins by reviewing their shared
and biome-specific course-design principles, then deciding which existing generation systems can be retained and
which need redesign to produce genuinely generative course quality.
