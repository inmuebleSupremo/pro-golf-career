## Context

`CourseGenerator` currently creates a deterministic course from a course seed and biome: it shuffles a fixed 4/10/4 par template, then independently samples length, width, green size, booleans, and elevation for each hole. `CanonicalGeometryGenerator` faithfully compiles those scalar values into immutable polygons, and `RoundHole`/`ShotResolver` settle play against those polygons. This spatial chain is correct and must remain the only gameplay geometry authority.

The missing layer is intent. `CourseIdentity.style` is currently a naming token, biome controls weather-facing exposure and small water/tree biases, and event `CourseSetup` modifies the playing presentation of an already generated course. None of those is a course-design profile. They must remain distinct:

| Concept | Owner | Purpose | Must not become |
| --- | --- | --- | --- |
| Biome/classification | `EnvironmentClassification` | Environmental character, location, weather exposure and bounded environmental bias | Course strategy identity |
| Design profile/plan | Course generation | Deliberate venue character and 18-hole composition | Tournament setup or rendered-only metadata |
| Event setup | `CourseSetup` | Pin, width, and wind conditions for one event | Permanent venue design |
| Canonical geometry | `CourseGeometry` | Authoritative playable terrain | A second representation of design intent |

The other material constraint is persistence. `Course.generatorVersion` is stamped today, but `World.restore(seed, config, snapshot)` regenerates its course pool through the current `CourseGenerator.generate(...)`; neither `WorldSnapshot` nor `SaveGame` pins the version. A future generator change would therefore rewrite existing saved venues. The save envelope is currently format v2 and rejects other formats, so adding provenance must include an explicit v2 compatibility path rather than treating missing data as the latest generator.

## Goals / Non-Goals

**Goals:**

- Make each generated venue have a deterministic, playable design identity that affects a coherent 18-hole plan and current generation inputs.
- Retain V1 generation byte-for-byte for historical courses, old saves, and regression comparison.
- Add V2 design-aware generation without changing the canonical terrain model, resolver, event setup seam, or frontend contract.
- Persist and restore generator provenance robustly, including existing v2 saves with no generator-version field.
- Protect composition, determinism, geometry validity, and calibration without forcing every legitimate profile to score identically.

**Non-Goals:**

- Multi-route fairways, detailed doglegs, land/property routing, role-based bunkers, advanced water layouts, green complexes, terrain contours, or SVG redesign.
- Club/aim/shot-type/shape controls, advanced ball flight, spatial putting, or retirement of current carry/lateral and strategy compatibility seams.
- Serialising complete generated course polygons as a save workaround.
- Adding a player-facing course-profile API/UI in this slice.

## Decisions

### 1. Use three composable profile axes, not a named-course taxonomy

The V2 pure course-domain model shall introduce a small immutable `CourseDesignProfile` selected deterministically from the course seed. It contains only:

- `StrategicEmphasis`: `POSITIONAL`, `BALANCED`, or `RISK_REWARD`.
- `WidthTendency`: `GENEROUS`, `BALANCED`, or `EXACTING`.
- `RecoverySeverity`: `FORGIVING`, `BALANCED`, or `PENAL`.

These are independent enough to produce useful combinations without creating dozens of categories. A profile may derive bounded generation weights, but it shall not duplicate biome, weather, or `CourseSetup`; it is permanent venue intent. Its values are stored on/generated with `Course`, so repeated appearances remain recognisable.

An independent permanent difficulty scalar is intentionally omitted from this first model. The three axes express inherent design pressure, while event `CourseSetup` remains the temporary competitive difficulty lever. Calibration diagnostics will measure their combined scoring effect before a later change proposes a separate permanent challenge dimension.

Alternative: named profiles such as “links”, “resort”, or “penal”. Rejected because those conflate environment and strategy, expand taxonomy before routing exists, and encourage decorative rather than mechanical identity. Alternative: dozens of numeric sliders. Rejected because the present generator cannot use or calibrate them honestly.

### 2. Plan the course before compiling any hole geometry

V2 shall generate `CoursePlan(profile, briefs)` from a dedicated course-plan seed derived from the established course seed hierarchy. The plan contains exactly 18 ordered `HoleBrief`s, preserving par 72 with two par 3s, five par 4s, and two par 5s on each nine.

Each `HoleBrief` shall contain only the currently useful, generation-facing facts: hole number, par, par-relative `LengthBand` (`SHORT`, `STANDARD`, `LONG`), `StrategicArchetype` (`POSITIONAL`, `BALANCED`, `RISK_REWARD`), and bounded recovery intent inherited from the profile. It shall not yet claim tee-shot target zones, approach-side logic, specific bunker roles, green contours, or route geometry.

The planner shall ensure: at least two length bands within each par class; both positional and risk/reward par-4/par-5 opportunities; no more than three adjacent briefs with the same archetype; and an intentionally varied front/back composition. The exact placement is seeded, so no course depends on ambient randomness.

Alternative: have each hole choose itself independently and add profile metadata later. Rejected because it does not solve the deliberate-composition problem. Alternative: create a large fixed library of real-world-style hole templates. Rejected because it would overpromise strategy before routing/green work exists and freeze future generation unnecessarily.

### 2a. Use exactly three current, bounded archetypes

The three archetypes are intentionally not a new taxonomy. They are different ways to choose existing scalar inputs, so they affect generated play today while staying honest about the present one-corridor generator.

| Archetype | Golfing character and distinction | Current-generator expression | Explicitly deferred |
| --- | --- | --- | --- |
| `POSITIONAL` | Accuracy and recovery discipline matter more than raw attack. It is the deliberate, control-oriented contrast to a reachable risk/reward hole. | Bias toward `STANDARD`/`LONG` par-relative bands, with bounded pressure from narrower fairway selection and/or more penal existing recovery and flanking-hazard eligibility. | Alternate landing corridors, a prescribed safe side, approach angle, and named strategic hazard roles. |
| `BALANCED` | A neutral all-round test. It is the control case: neither deliberately restrictive nor deliberately temptation-led. | Bias toward the middle of the applicable length range, profile-normal width, and baseline biome-composed hazard/recovery eligibility. | It does not mean featureless terrain or a universal scoring target. |
| `RISK_REWARD` | A plausible aggressive opportunity whose lower length band can be converted, but whose existing miss consequences are appreciable. It differs from positional through shorter/reachable par 4 or par 5 opportunities and a bounded exposure trade-off. | Bias toward `SHORT` par 4s and lower-end/reachable par 5s, plus bounded increases in existing flanking bunker/water/tree eligibility or recovery pressure. The planner must only assign it where the par has a current meaningful short/reachable band. | A player-selected safe versus carry route, exact lay-up distances, forced carries, target selection, and a guaranteed hazard location. |

Profile axes and archetype offsets compose but remain bounded: a generous profile cannot be silently turned into a penal design by an archetype, and an archetype cannot erase biome's environmental bias. The measurable V2 tests below verify the resulting ordered distributions, rather than asserting that every individual hole has every characteristic.

### 3. Compile only the intent current generation can make real

The V2 generator shall translate briefs into existing generator inputs before calling the canonical compiler:

- `LengthBand` selects a bounded subrange within the current par range.
- `WidthTendency` applies a bounded multiplier/selection to fairway width.
- `StrategicEmphasis`, `StrategicArchetype`, and `RecoverySeverity` make bounded, deterministic adjustments to existing bunker/water/tree eligibility and current recovery-width inputs.
- Biome remains responsible for environmental bias; profile values may compose with, never replace, that bias.

This produces observable differences in plan composition, hole lengths, widths, and reachable current terrain without pretending to generate strategic corridors. `CanonicalGeometryGenerator` remains the single compiler of gameplay polygons, and `GeneratedHole`/`HoleModel` remain the resolver seam.

Any new parameter must have a direct V2 use and a testable observable effect. The generated profile/plan may be exposed internally on `Course`, but no player-facing API is required until a later product slice establishes its use.

### 4. Pin generation to an explicit retained registry

Replace the implicit single `CourseGenConstants.GENERATOR_VERSION` entry point with a pure version-selected registry/API. `generate(coordinate, classification, version)` selects exactly one supported implementation. The existing implementation is retained as V1, preserving current output exactly. V2 is the first profile/plan-aware implementation. A convenience “latest” generator may be used only when creating a new world; restore code must never call it.

Unsupported versions shall fail explicitly. Historical implementations remain small pure code paths/compatibility adapters in the simulation domain as long as supported saves can reference them. A future removal requires a separately approved migration proving equivalent replacement or deliberately ending support; it must not silently reinterpret a saved version.

Alternative: serialize every `Course`/polygon. Rejected because it duplicates immutable seed-derived data, enlarges saves, and does not define how later generated venues preserve semantic version behavior. Alternative: force all old saves to V2. Rejected because it changes established venues and history.

### 5. Pin provenance at world snapshot level, with an explicit legacy default

All current course pools are created as one world-level generation cohort during world creation and are then referenced by that fixed pool; the existing lifecycle does not append independently generated venues to a career. Therefore one `WorldSnapshot.courseGeneratorVersion` is sufficient and unambiguous for the current save/restore lifecycle. New worlds capture their selected version; restore regenerates the complete pool with that exact version. The `Course.generatorVersion` stamp remains an integrity/read-model fact and must agree with the world pin.

The application save layer shall introduce a new envelope format that can read both the existing v2 payload and the new versioned payload. Missing/null generator provenance in a valid v2 save means V1—not latest. New saves write the current format and an explicit version. There is no whole-geometry migration. Tests must exercise disk JSON/load, not only in-memory snapshots.

This foundation does not add mixed-version pools. A future feature that adds venues to an existing world must choose the world-pinned version unless it introduces explicit per-venue provenance and migration rules. This is an intentionally small policy, not a migration framework.

The objective compatibility proof has two layers. First, retained V1 golden-course fixtures use the existing historical course-generator seed/classification coordinates and compare complete V1 course records and canonical geometry exactly. Second, representative on-disk saves from the current v2 envelope—covering a newly created career, a progressed career with history, and any existing save fixture used by the persistence suite—are loaded with omitted provenance and must restore as V1 with the same venue identifiers, generator stamps, and geometry. Explicit V1 and V2 saves are tested separately; an unknown pin fails before a usable world is returned. These fixtures prove behavior, not preservation of the original source-file layout.

### 6. Lock calibration evidence before V2 tuning

Calibration has a fixed methodology, chosen before implementation so V2 cannot select post-hoc passing thresholds:

1. **Immutable V1 corpus.** Before adding V2 parameter tuning, capture an exact baseline from 48 fixed historical seed/classification coordinates (eight per `EnvironmentClassification`) drawn from the existing generator test corpus; include the representative legacy save fixtures named in Decision 5. Check the coordinate list, complete V1 course/geometry fixtures, aggregate composition metrics, terrain exposure, and scoring diagnostic output into test resources or checked-in test constants. V1 assertions remain exact compatibility assertions.
2. **Fixed V2 corpus.** Run all 27 legal profile-axis combinations over the same 48 coordinates for plan and geometry checks (1,296 courses). For round/scoring calibration, run the fixed 9-profile anchor set—balanced/balanced/balanced plus each approved extreme and mixed positional/risk-reward combination specified in the calibration contract—across 12 fixed coordinates, 12 deterministic rounds per coordinate/profile, and the existing representative player/AI field configurations. The anchor list is committed with the V1 baseline before V2 tuning; it cannot be pruned based on results.
3. **Pre-commit contract.** After V1 measurement and before adjusting V2 mappings, record the baseline values, the chosen V2 anchor profiles, and numerical aggregate envelopes in a reviewed calibration-contract fixture. Envelopes may be derived from the V1 evidence, but the contract and V1 fixture must be committed independently of the later V2 tuning change. Changing either requires an explicit reviewed calibration change, never a quiet test update.
4. **Classify outcomes.** Intended profile differentiation is a directional, planned result; a regression is V1 fixture drift, aggregate scoring/exposure outside the locked envelope, or loss of current human/AI behavior; a pathology is invalid geometry, impossible/degenerate surface placement, failed round resolution, or a plan that violates its composition constraints. A legitimate harder profile is not itself a scoring regression, but it still must fit its pre-committed profile-specific envelope.

The locked contract must require all of the following acceptance evidence:

- Exact V1 reproduction for every baseline fixture, including canonical geometry, plus legacy-save restore through V1.
- All 1,296 V2 courses pass plan constraints, canonical-geometry validity, deterministic regeneration, and existing setup-effective-geometry/surface-classification invariants.
- Across the fixed corpus, mean width is ordered `GENEROUS > BALANCED > EXACTING`; recovery exposure is ordered `FORGIVING < BALANCED < PENAL`; and positional plans have a higher `STANDARD`/`LONG` share while risk/reward plans have a higher eligible `SHORT` par-4/lower-end par-5 opportunity share. The contract records the minimum separations before V2 tuning.
- Archetype runs satisfy the no-more-than-three-adjacent rule and the plan's front/back and par-relative variety rules; the corpus report flags repeated plan signatures beyond the pre-committed maximum.
- Bunker/water/tree and recovery-surface exposure remain inside each locked profile/biome envelope, so intended profile effects do not erase environmental character or create pathological terrain.
- Existing V1 strategy/scoring regressions run unchanged against V1. New V2 aggregate and per-anchor-profile scoring distributions must satisfy the locked envelopes; their averages need not equal V1 or one another.
- Human and AI use the same existing `HoleModel`/geometry seams to complete the fixed V2 rounds without resolver, settlement, or setup-specific geometry failures.

This is deliberately a calibration gate, not a permanent difficulty scalar or a claim that every course profile should score identically.

## Risks / Trade-offs

- **[Profile becomes cosmetic metadata]** → Require profile-to-plan and brief-to-generation mapping tests with measurable distribution differences.
- **[Profile changes destroy calibrated scoring]** → Keep parameter adjustments bounded, retain exact V1 tests, and gate V2 promotion on the independently locked calibration contract.
- **[V2 claims strategy it cannot spatially express]** → Limit V2 archetypes to length/width/current hazard/recovery inputs; defer routes, roles, and approaches.
- **[A save regenerates with latest code]** → Restore only through the snapshot pin; missing historical data defaults to V1.
- **[Historical implementations accumulate indefinitely]** → Treat removal as an explicit compatibility decision with tests and a migration/rejection policy, not casual cleanup.
- **[World-level pin blocks future new-venue variety]** → This is intentional for the existing fixed pool. A mixed-version venue policy is a later, separately specified extension.

## Migration Plan

1. Establish and lock the exact V1 generator, save, geometry, composition, exposure, and scoring baseline corpus.
2. Add version-pinned regeneration and save compatibility; retain V1 as current until the V2 gate passes.
3. Add compact profiles and lightweight briefs, then deterministic 18-hole planning.
4. Compile V2 intent through existing canonical geometry and measure the locked differentiation/quality evidence.
5. Verify scoring, determinism, human/AI behavior, and legacy compatibility; only then promote V2 as the current version for new worlds.
6. Run representative manual gameplay E2E for a V1-restored career and a new V2 world. Rollback before promotion keeps V1 current; after promotion, saved V2 worlds remain reproducible through V2 and are never “rolled back” by changing their pin.

## Resolved scope and genuine open questions

The approved scope is: the three profile axes and three archetypes above; V2 as the first design-aware version; a missing-provenance legacy-save default of V1; profiles/plans internal to generation; and the locked-before-tuning calibration process. There are no unresolved product or architecture decisions requiring approval before implementation. Exact numeric calibration values are intentionally deferred only until the immutable V1 baseline is measured and then become a reviewed, versioned contract before V2 tuning.
