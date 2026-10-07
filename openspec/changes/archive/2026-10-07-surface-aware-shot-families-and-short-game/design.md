# Design: Surface-Aware Shot Families and Short Game

## Context and objective

The current system already has the right spatial contract: `BallStrikeIntent` supplies an individual club and an absolute canonical `AimPoint`; the resolver samples carry/lateral, projects one canonical contact, then `ShotSettlement` supplies the legal next `BallState`; observable shots expose those facts through `ShotTrace`.

What is missing is intentional technique and starting-lie meaning. Except for green/fringe putting routing and water/out-of-bounds recovery, the current resolver gives ordinary surfaces the same non-putting execution distribution. This change adds a small real short-game slice without claiming uncomputed flight or detailed ground physics.

## Intent and family model

Add a pure enum:

```text
ShotFamily = FULL | CONTROLLED | PITCH | CHIP | BUNKER
```

The engine-level non-putting intent becomes conceptually:

```text
BallStrikeIntent {
  club: ClubId
  aimPoint: AimPoint
  shotFamily: ShotFamily
}
```

`AimPoint` remains the intended carry/first-contact point. `ShotFamily` is a deliberate technique choice, never a request for carry, rollout, launch height, spin, randomness, or other execution parameters. The resolver derives those values. Green putting remains outside `BallStrikeIntent` family semantics and continues to use the existing putting model.

Make that existing route explicit as a minimal `PuttIntent`. It contains no line, speed, break, spatial aim, or family control. The application exposes `playPutt(expectedShotRevision)` rather than forcing a GraphQL input union; it preserves the existing putting resolver and only makes routing unambiguous.

`ShotDecision` remains a local compatibility adapter while the existing carry/lateral sampler is retired incrementally. It must be derived from the submitted/AI-produced intent, rather than independently chosen policy state. No human strategy selector or hidden strategy-to-family substitution returns.

## Eligibility and club compatibility

Introduce a pure authoritative service equivalent to:

```text
ShotFamilyEligibility.evaluate(startLie, ClubSpec, ShotFamily) -> Eligibility
Eligibility = allowed | disallowed(reason)
```

It is evaluated before resolution and is the server-side authority. Client guidance only mirrors it.

| Family | Compatible club families | Intended first-slice lie policy |
| --- | --- | --- |
| FULL | Driver, woods, hybrids, irons, wedges | Broadly allowed on tee/fairway/cut/rough; restricted where a full swing is plainly unsuitable. |
| CONTROLLED | Driver, woods, hybrids, irons, wedges | Same broad club compatibility as FULL, with lie-aware constraints. |
| PITCH | Pitching, gap, sand wedge | Fairway, cuts, rough, fringe, and appropriate recovery lies. |
| CHIP | Irons and wedges | Near-green ordinary lies, rough, fringe, and appropriate recovery lies. |
| BUNKER | Pitching, gap, sand wedge | BUNKER only. |

Rules that are physically or semantically impossible are invalid: green uses the dedicated putting path; BUNKER is the only family from bunker in this first generic-bunker slice; driver/woods are not allowed from tree lies. Recovery areas and deep rough should retain legal but poor choices where the surface conveys difficulty rather than an absolute obstruction. Their penalty belongs in execution profiles, not blanket UI hiding.

The canonical terrain currently exposes generic `BUNKER`, not fairway-bunker versus greenside-bunker semantics at the resolver seam. This change therefore must not pretend to make that distinction.

## Surface-aware execution profile

The resolver shall derive one immutable execution profile from start lie, selected `ClubSpec`, family, attributes, and `Environment`:

```text
ShotExecutionProfile {
  allowed
  effectiveCarryCap
  distanceDispersionMultiplier
  lateralDispersionMultiplier
  mishitMultiplier
  groundResponseRule
}
```

The values are internal calibration inputs, not GraphQL controls. Weather remains an `Environment` concern; actual starting surface is a separate source of lie effects and must not be folded into global `Environment.lieQuality`.

The initial profiles are intentionally bounded:

- **FULL** preserves existing baseline execution except for honest lie constraints/penalties.
- **CONTROLLED** trades lower effective carry cap for lower distance and lateral variance. Its gains are calibrated against its reduced commitment; it cannot dominate FULL at all distances.
- **PITCH** uses wedge skills, a bounded short carry, and smaller release than CHIP.
- **CHIP** uses iron/wedge skill mapping, a bounded short landing target, and larger release than PITCH. Individual club characteristics may influence that bounded release.
- **BUNKER** uses wedge skill, a short carry cap, greater execution variance than comparable pitch, and zero or negligible release.

Deep rough/recovery receive small, explicit reach and dispersion penalties. Bunker behavior is family-specific rather than a generic sand penalty applied to every shot. Exact numbers are calibration work, not API contract.

## Bounded authoritative ground response

This change adds only a landing-and-roll foundation:

1. The existing resolver samples and projects one canonical first contact toward the submitted `AimPoint`.
2. Water and out-of-bounds contact resolve immediately through existing recovery/replay rules; they never roll.
3. For eligible playable contacts, the family profile may derive one bounded forward displacement using the existing `ShotFrame`, selected club, and contact surface.
4. The response has one candidate endpoint. It does not bounce, sample speed, model spin/slope/firmness, or chain across terrain.
5. A roll may be accepted only when the candidate remains on the supported contact surface under canonical geometry classification. If it would leave that supported surface, the conservative deterministic outcome is no roll: final point equals contact.
6. The endpoint's canonical surface becomes the final playable lie. The contact surface remains separately recorded.

The first-slice response is intended chiefly for PITCH and CHIP. Any modest FULL/CONTROLLED response is permitted only if calibration proves it is required; baseline FULL must not change merely to manufacture contrast. BUNKER has no meaningful roll in this slice.

This endpoint-only rule deliberately does not promise path collision, terrain traversal, or cross-course rolling. Bump-and-run is deferred because its defining gameplay requires those richer semantics.

## Resolver, settlement, and outcome boundary

The execution layer owns sampled contact and any calculated bounded roll. `ShotSettlement` remains the sole authority for converting that execution result into the legal next `BallState` and for recovery/replay:

| Fact | Authority |
| --- | --- |
| Intended landing point | `BallStrikeIntent.aimPoint` |
| Actual first contact / contact surface | Resolver + canonical geometry |
| Optional bounded roll endpoint | Resolver execution profile + canonical geometry |
| Water/OB relief or replay | `ShotSettlement` |
| Final playable ball and final lie | `ShotSettlement.ball` |

Settlement must consume an already-computed normal final point; it must not invent physical roll after the fact. Conversely, execution must not duplicate water-drop or replay legality. A recovery transition wins over roll: no water/OB trace may contain a roll phase.

`ShotOutcome.finalSurface` must be specified and migrated as the final settled-ball surface, not an ambiguous contact alias. Contact surface remains available through settlement and trace. Existing fairway/GIR/proximity/statistics consumers require deliberate review under this distinction.

## Trace contract and ordering

Extend `ShotTrace` with a separate optional authoritative roll fact, equivalent to:

```text
ShotTrace {
  clubId
  origin
  intendedAimPoint
  contact
  roll: ShotTraceRoll?                 // contact → ground-response endpoint
  transition: ShotTraceTransition?     // recovery/replay only
  finalPoint
}

ShotTraceRoll { from, to }
```

`ShotTraceTransition` remains exclusively a rules-driven recovery/replay operation. A roll is calculated ground response and may be displayed as movement. They must never be conflated.

Valid first-slice sequences are:

| Case | Sequence |
| --- | --- |
| Normal FULL / controlled with no response | origin → contact → final, where final equals contact |
| PITCH | origin → contact → optional short roll → final |
| CHIP | origin → contact → optional larger roll → final |
| BUNKER | origin → bunker contact → final, normally equal |
| Water / OB | origin → penalty contact → recovery/replay transition → final |

Observable and summary modes retain the same sampled/settled outcome. Summary mode may omit only the trace allocation, never the ground response itself.

## Fringe and putting boundary

Green remains the existing dedicated putting model. The current resolver-side automatic rule that treats a fringe lie within ten yards as a putt regardless of selected intent must retire once deliberate short-game intent exists.

- Green: `PuttIntent` / `playPutt` only; normal ball-strike family validation must not accidentally bypass it.
- Fringe: deliberate `PuttIntent` / `playPutt` or an eligible ball-strike family may be chosen.
- Near-green fairway: eligible pitch/chip choices; putter-from-fairway remains deferred.

The minimal AI policy may deliberately emit `PuttIntent` for a close fringe lie. That is policy selection through the same public intent contract, not a resolver override of a submitted ball-strike family.

No spatial putting, speed, break, or green-slope system is introduced.

## GraphQL, guidance, UI, AI, and saves

Add `shotFamily` additively to `BallStrikeIntentInput`. For external compatibility, omission may map to `FULL` at the API boundary; engine ball-strike intent is non-null. Expose dedicated `playPutt(expectedShotRevision)` for the minimal `PuttIntent` route. Invalid club/family/lie combinations fail authoritatively before any stroke or trace is produced. Do not expose profile multipliers or rollout controls.

Extend `ShotGuidance` with per-current-lie/per-club family availability and concise disabled reasons. The UI implements only:

```text
Club → Technique → Landing target → Play shot
```

For roll-capable families, label the marker “Landing target”; do not predict a final resting point. Canonical playback consumes returned trace roll data only.

AI produces the same `ShotIntent` union as the human: `PuttIntent` on green and for its close-fringe putting choice; otherwise a family-bearing `BallStrikeIntent` (FULL by default, CONTROLLED for lay-ups, PITCH/CHIP near green when compatible, and BUNKER in sand). A ball-strike compatibility adapter derives carry solely from the submitted `AimPoint` and uses that same point as its frame; it does not recompute legacy AI carry/lateral decisions.

Pending submission is transient, so no save migration is presumed. Existing saves, historical outcomes, snapshot handling, and any persisted shot detail must nevertheless be tested for compatibility before implementation closes.

## Temporary compatibility and retirement conditions

`ShotDecision` and the carry/lateral sampler remain temporary internal compatibility code. They may retire only when the resolver natively consumes `ShotIntent` without a carry/lateral translation, deterministic seed behavior and calibration corpora remain stable, and all retained legacy fixture/save paths have migrated or been explicitly retired. Until then, the adapter must continue to be derived solely from a submitted `BallStrikeIntent`; it cannot make a second AI decision.

At the GraphQL boundary only, an omitted `shotFamily` continues to default to `FULL` for older callers. That default may retire only in a versioned API breaking-change window after compatible clients and persisted integrations have been migrated. Engine `BallStrikeIntent` remains non-null today.

## Calibration, performance, and risks

Calibration must cover scoring, GIR, scrambling, up-and-downs, bunker saves, short-game proximity, contact/final surface, penalties, and human/AI parity using fixed deterministic corpora. Existing course generator/version fixtures remain fixed. Intentional short-game improvement is permitted, but scoring collapse or universal superiority of controlled/chip options is not.

Ground response is one bounded endpoint calculation per resolved shot: no per-inch stepping, pathfinding, large terrain walks, or repeated polygon-intersection chains. Background simulation remains summary-only for trace allocation and must remain practical.

Primary risks are ambiguous final-surface semantics, client-derived rollout, family aliases, fringe silently becoming a putt, AI/intent divergence, and accidentally extending this bounded rule into trajectory or terrain-response simulation.

## Explicit deferrals

BUMP_AND_RUN, FLOP, PUNCH, STINGER, fade/draw, directional wind, apex, flight samples, true spin, bounce, slope, firmness simulation, arbitrary terrain traversal, forced carries, tree collision, spatial putting, green contours, equipment progression, and play-screen redesign remain out of scope.
