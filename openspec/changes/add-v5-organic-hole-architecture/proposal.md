## Why

V4 made routes, landing zones, green intent and hazard purpose explicit, but ordinary generated holes still compile
from an almost straight, symmetric corridor, a rotated oval green, and a tiny hazard vocabulary. In particular,
the current V3 planner permits one anchor but selects doglegs only 0.5--2% of eligible holes. A course can satisfy
its plan and still feel like eighteen cosmetic variations of one geometry.

This first next-generation slice must make normal newly generated courses visibly and strategically more varied
without waiting for terrain physics or claiming mechanics that do not exist.

## What Changes

- Retain the implemented V5 organic-hole foundation as an explicit generator version; preserve exact V1--V4
  regeneration and saved-career pins.
- Add a pure, deterministic candidate-and-selection architecture: course-level architectural intent, multiple
  constrained candidates per brief, pure feasibility/quality scoring, and deterministic whole-course selection.
- Replace V5's V3-shaped route corridor with one connected, asymmetric fairway/landing/approach plan that can
  express straight, gentle and meaningful dogleg routing (up to two bounded direction changes), width rhythm and
  varied simple green footprints.
- Preserve canonical polygons as sole surface/render authority; compile purposeful, variably dense bunker complexes
  and biome-plausible tree/recovery regions from the same route, landing and green intent. Keep target progression
  and AI/human parity compatible with the richer route plan.
- Add deterministic course-quality metrics and canonical-geometry contact-sheet diagnostics for fixed regression
  and locked unseen seed corpora.

V5 accepts a candidate only when its geometry creates a supported golfing consequence, not merely a novel outline:
landing forgiveness, next-shot distance, approach-side access, or existing bunker/rough/water exposure must vary
coherently with route, widths and green sectors. The generator retains ordinary forgiving holes alongside demanding
ones; it neither makes all eighteen dramatic nor retains weak examples only by hiding them from review.

## Capabilities

### New Capabilities

- `organic-hole-architecture`: V5 course identity, candidate generation/evaluation/selection, rich connected-hole
  plans, canonical compilation, diversity diagnostics and playability safeguards.

### Modified Capabilities

- `course-generation`: V5 is a retained explicit generator version.
- `course-design`: course composition uses architecture identity and whole-course anti-repetition selection.
- `strategic-hole-routing`: V5 route/landing/approach semantics support richer but non-branching progression.
- `hole-spatial-model`: V5 plans remain metadata while canonical geometry stays authoritative.
- `course-persistence` and `save-persistence`: existing version-pinned regeneration supports V5 without polygon
  snapshots or destructive career migration.

## Impact

- **Simulation/course:** pure immutable V5 records, candidate planner/compiler/evaluator, generator registry,
  deterministic diagnostics and calibration corpus.
- **Shot and AI:** route progression consumes V5-safe semantic targets and existing resolver facts; no separate
  human/AI physical path and no future RNG is read.
- **Rendering:** the existing canonical SVG renderer receives richer polygons. A diagnostic contact sheet is
  development evidence, not a browser terrain generator or required player feature.
- **Persistence:** retained version provenance only; no course-polygons, plans or history rewritten in saves.

## Non-Goals

- Branching fairways, multiple independently playable routes, arbitrary authored templates, terrain meshes,
  polygon boolean tooling or a generic course editor.
- Tree collision/clearance, forced carries, airborne water/obstacle interception, hazard crossing during ground
  movement, elevation/slope/contours, spatial putting, or a club-distance retune.
- New player controls, frontend strategic authority, hazard overlays, or a cosmetic-only obstacle represented as
  mechanically blocking.

## Refinement: Canonical landform and composition quality

The first complete V5 gallery established that routing and fairway variation are materially better, but also exposed
three release-blocking weaknesses: bunker output was inherited as a single sparse V4 feature, tree intent was not
compiled into visible canonical tree regions, and corridor/green boundaries showed station facets. This refinement
adds coherent variable bunker complexes, route-aware tree clusters/corridors/openings, and bounded smooth canonical
sampling. It also replaces the accidental over-selection of double-turn routes with an explicit whole-course mix of
straight, gentle-moving, dogleg and rare double-dogleg holes. These are architecture and geometry changes, not SVG
decoration or a promise of new collision/carry rules.

## Refinement: environmental architectural identity

The second complete-gallery review found that aggregate feature counts concealed weak environmental identity: in
particular, parkland tree regions could sit outside the playable boundary, coastal labels lacked a coherent shore,
and links/woodland/parkland did not yet read as distinct ordinary courses. V5 now needs a deterministic course-level
environment character pass. It SHALL compose variable tree enclosure (open, scattered, broken-lined, wooded) and
coastal exposure across the complete routing, then compile those choices as canonical in-bounds terrain where
appropriate. Reports SHALL distinguish in-bounds and fairway-adjacent vegetation, bunker purpose/scale/grouping,
and shoreline-related water from raw region totals. These improvements remain endpoint/lie truthful; they do not
claim tree collision, carries, islands, bridges or disconnected-land support.

## Evaluation Commitment

The implementation reports V4-versus-V5 complete-course evidence for established regression seeds and a locked
previously unseen corpus across environments and profile combinations. The gallery includes every generated hole in
each reported course; it SHALL NOT be filtered to showcase candidates or omit weak layouts. Quantitative measures
are diagnostic gates, not evidence by themselves that a course is good.

## Approval Required

Approve V5's initial routing boundary: one connected corridor with zero to two bounded directional changes and
asymmetric width stations. This deliberately defers branch networks and hard forced-route claims until authoritative
flight/obstacle/carry mechanics exist.
