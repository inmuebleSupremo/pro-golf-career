## Context

V4 has canonical terrain and semantic course planning already:

```text
V4 CourseGenerator -> HoleSpatialPlan / LandingZone / HoleRoute / HazardPlan
                   -> CourseGeometry
                   -> existing ShotResolver -> canonical contact -> bounded ground response -> settlement
```

`HoleSpatialPlan` has exactly one PRIMARY zone and may add SAFE and AGGRESSIVE zones. Its current `progressionTarget` deliberately returns `null` for straight routes to preserve the earlier green-centre aim frame. On the interactive side, `ShotGuidance` exposes three non-null `AimPoint`s and the play dock always displays Safe, Primary, and Aggressive. On the simulated side, `StrategyPolicy` selects club/family from remaining distance and uses `ShotAim` as a compatibility target; it does not jointly assess V4 landing zones, canonical hazards, and current playable club/family choices.

The requested feature activates those existing semantics. It must remain below the shared intent/resolver boundary: the planner proposes a legal target plan, and the existing resolver remains the sole authority for the random execution, flight, first contact, supported roll, hazards, recovery, and final ball.

## Goals / Non-Goals

**Goals**

- Make a V4 SAFE/PRIMARY/AGGRESSIVE option a real, understandable golfing choice only when it has a valid distinct landing/route consequence.
- Use the same pure candidate facts for AI planning and non-binding human guidance; preserve human literal free aim, club, family, and shape choice.
- Make AI choice deterministic and explainable from current visible/modelled facts, not from the next random resolution.
- Let canonical hazards and existing bounded ground response affect viability/exposure only where current rules support those facts.
- Preserve old courses and completed results; allow future unresolved V4 strokes to benefit without a migration UI.

**Non-Goals**

- Tree collision/clearance, airborne water or forced-carry rules, sampled flight-path collision, or ground-hazard traversal.
- Elevation, slope, green contours, putting changes, rollout-distance recalibration, club-distance recalibration, a generator rewrite, or a new player control surface/UI redesign.
- A probability engine, Monte Carlo planning, clairvoyant use of the upcoming RNG result, or a separate human/AI physics path.

## Decisions

### D1 — Create one pure strategic-target planner before the shared intent boundary

Introduce a simulation-only planner (name to be selected during implementation) that receives the current `HoleModel`, `BallState`, remaining distance/lie, player attributes, current conditions, legal club/family availability, and an AI disposition where applicable. It returns zero or more immutable `StrategicTargetOption` facts. An option contains at least:

- role (`SAFE`, `PRIMARY`, or `AGGRESSIVE`);
- literal canonical landing aim point;
- a legal suggested club/family pair and its normal reachable landing range;
- a server-authored, qualitative exposure/route explanation; and
- a deterministic next-shot/remaining-distance consequence used to establish material distinction.

The planner reads current V4 `HoleSpatialPlan`, route progress, zone cores, canonical surfaces/hazards, and the existing club/family rules. It is pure and does not retain a ball, alter a seed, or write save data. AI turns its chosen option into the same `BallStrikeIntent` a human can submit; human guidance is advisory only.

### D2 — Screen candidates with deterministic current facts, not future shot results

For each candidate role, the planner SHALL:

1. select the relevant future zone/route target from current route progress, or reject it when no future zone is appropriate;
2. find at least one legal club/family that can reach its landing envelope from the current ball under existing reach and lie rules;
3. require canonical target terrain to be a valid intended landing surface and reject a target that is already in WATER, OUT_OF_BOUNDS, BUNKER, TREES, or recovery terrain;
4. derive a fixed, bounded two-sigma *contact exposure envelope* from the same pre-shot lateral-dispersion inputs used by the current shot model, then classify canonical surfaces inside that envelope; and
5. apply the existing bounded ground-response policy only as a deterministic nominal landing/release screen. It must not create a new traversal rule, a roll-created penalty, or an unmodelled hazard prediction.

The exposure description is not a probability or a promise. It is a relative current-course fact such as a guarded/higher-exposure landing area, based on canonical terrain inside a deterministic pre-shot envelope. It must not claim that a water carry, tree clearance, or future mishit has been simulated.

Implementation may extract a shared pure forecast/helper from existing shot inputs if needed; it must not duplicate or retune execution randomness. The fixed envelope is preferred over Monte Carlo so candidate selection consumes no random values and remains reproducible.

### D3 — Truthfulness and fallback rule

An option is publishable only when it is viable and materially distinct from the ordinary/PRIMARY alternative. Distinction requires both a different useful landing/route point and a legible consequence: for example, a materially shorter next approach paired with higher canonical exposure, or a safer landing/route paired with a longer next approach. The implementation uses a 12-yard material next-shot-distance delta and a two-sigma lateral exposure envelope; both are fixed planner constants selected from the deterministic V4 fixtures, not UI rules.

AGGRESSIVE is valid only if it improves the route/next approach and has greater supported exposure than the safer comparison. SAFE is valid only if it reduces supported exposure or preserves a more reliable route at a real positional cost. The planner SHALL not assign a strategic label merely because V4 metadata happens to contain a zone.

If the necessary distinction does not exist—because a target is unreachable, unsafe, redundant, the ball is already in short-game range, or the hole lacks an eligible V4 plan—the planner returns the established ordinary target as one PRIMARY option and explains no fabricated trade-off. The UI does not render duplicate strategic choices. V1/V2 behaviour is therefore unchanged, and straight V4 holes can gain choices only when their actual zones establish one.

### D4 — Human guidance is structured and non-binding; no new control category

Replace the internal/API meaning of three unconditional `ShotGuidance` points with an additive ordered collection of backend-authored strategic options. Keep a compatible ordinary PRIMARY aim point for existing consumers during the migration, then remove/retire the redundant fields only in a separately approved compatibility cleanup if appropriate.

The existing play dock continues to provide compact target buttons, but renders only distinct available options. Each button/tip uses backend-provided role, suggested club/family, and short explanation (for example, longer approach / lower exposure). Choosing it changes the literal target marker only. It MUST NOT silently change the player's club, family, shape, or final outcome, and free pointer/touch/keyboard aiming remains available.

The browser only displays the supplied facts. It must not decide whether targets are distinct, recompute terrain exposure, simulate rollout, or infer a fallback from SVG geometry.

### D5 — AI policy chooses a precomputed option deterministically

For an eligible non-putting V4 situation, `StrategyPolicy` requests the same planner facts and selects in this order:

- retain existing recovery/putt and short-game protections;
- CONSERVATIVE prefers viable SAFE, otherwise PRIMARY/ordinary;
- BALANCED prefers viable PRIMARY/ordinary;
- AGGRESSIVE prefers viable AGGRESSIVE only when the planner established the real route advantage/exposure trade-off, otherwise PRIMARY/ordinary;
- retain existing par-5 lay-up/go-for-it logic as a constraint when no approved strategic option supersedes it.

The chosen option supplies the suggested club/family and literal aim point. No strategy reads an upcoming shot's random contact, roll, hazard, or score. Existing strategy remains the policy's disposition and is not persisted or migrated.

### D6 — Compatibility and persistence

No generated course data changes. Existing V4 spatial plans and canonical geometry are sufficient. V1/V2 models, V4 holes without eligible strategic options, completed holes/events, historical scorecards, and saved careers remain untouched. Current saves have no pending ball migration requirement; future unresolved V4 strokes may use the planner under the loaded course, player, and conditions.

As with the accepted ground-response policy, cross-build re-resolution of a future stroke can differ because planning policy changes. Historical completed results are never re-resolved. No new world version, strategy adoption control, or data conversion is introduced.

### D7 — Scope boundary and later roadmap

This feature is intentionally the smallest strategic activation. It leaves obstacle clearance, richer archetype variety, hazard-aware approach play, and elevation as later work. Those later features may extend the same pure planner, but none is implied or enabled by this change. Numerical rollout calibration—especially driver/long-club release across landing conditions and its scoring/hazard consequences—remains the explicitly deferred calibration task from the completed ground-response milestone.

### D8 — The availability projection prevents invalid defaults; aiming remains click-first

The server already projects the authoritative legal club/family/shape availability for the current lie. The play dock SHALL initialize from that projection and reconcile its current selection when a new situation makes it invalid, while preserving a still-legal deliberate player choice. It does not infer new legality or loosen `ShotFamilyEligibility`; the server still validates every submitted intent.

Expected validation errors returned as `BAD_REQUEST` are useful player feedback (for example, an aim outside the published planning envelope). The client may display that safe server message, but network and unexpected failures retain a generic retry message.

Click/tap on the hole remains the normal literal aiming interaction. Remove the redundant directional nudge buttons. Keep numerical X/Y inputs under a compact Fine adjust disclosure for precision and accessibility. This is a small presentation correction, not a new shot-control category or a replacement for free aim.

## Acceptance design

The implementation must provide a small deterministic fixture path without a development-only runtime feature. Prefer a named, fixed-seed V4 fixture/helper built directly from existing course planning/geometry factories. Add a narrowly scoped fixture only if the existing corpus cannot reliably identify the required hole in a readable test.

Required evidence:

1. A strategic V4 par 5 offers valid, distinct SAFE and AGGRESSIVE plans. The aggressive plan has a measurable better next-shot consequence and higher canonical exposure; the safe plan has the inverse trade-off. Both use legal club/family choices and existing resolver semantics.
2. A positional V4 par 4 chooses a route/landing target rather than automatically aiming at the green or pin when the green is not the approved strategic target. It may legitimately expose only PRIMARY when no honest second option exists.
3. AI CONSERVATIVE/BALANCED/AGGRESSIVE selection is deterministic, follows the same published candidate set, and falls back correctly.
4. A human receives only backend-authored distinct options, can adopt an option's literal target, and can still retain/select any legal club/family/shape and free target without client terrain authority.
5. V1/V2 and ineligible V4 compatibility fixtures retain their existing aim/policy outcome; shared human/AI resolver equivalence, seed stability, and current calibrated baseline checks remain intact.
6. A bunker/recovery situation initializes only a server-legal strike selection; an invalid out-of-envelope aim remains rejected by the server and produces the specific expected validation feedback. Hole click/tap and disclosed X/Y fine adjustment remain available without directional aim buttons.

### Implementation refinement — deterministic fixture scope

The existing generated corpus deliberately permits an honest one-PRIMARY fallback, so it does not guarantee one
compact, readable three-option par-5 in every fixed seed. Implementation therefore adds one narrow test-only
`HoleModel` fixture assembled from the existing `HoleSpatialPlan`, `CourseGeometry`, and canonical terrain records.
It has no runtime route, API, generator, save, or debugging-framework surface. It proves the required 230/270/315
yard safe/primary/aggressive decision separately from the broad generated-course corpora that continue to validate
V4 feasibility and calibration.

## Risks / Trade-offs

- **Candidate evaluator drifts from resolver facts** — keep it pure, bounded, and based on extracted shared pre-shot/ground-response facts; assert that it cannot author final settlement.
- **A label implies unsupported physics** — use qualitative exposure language and reject unsupported water/tree/flight claims; no percentage, carry, clearance, or final-rest promise.
- **Options crowd normal play** — require material distinction and publish only a primary ordinary target when no genuine decision exists.
- **AI scores shift unexpectedly** — run focused fixed fixtures plus existing strategy/scoring/hazard calibration before integration; do not compensate by retuning rollout or clubs in this feature.
- **GraphQL/client migration breaks current play** — make structured options additive first, regenerate client types, and test no-guidance/ordinary fallback state.

## Approval decisions

No additional product-scope decision blocks implementation. The design adopts an **additive structured strategic-option projection** rather than asking the browser to compare three bare coordinates. This is the smallest way to state why a choice exists without browser terrain authority. Reviewers should explicitly confirm that compatibility approach before implementation; if rejected, the safe fallback is to keep the legacy fields and defer activation rather than teach the browser strategic interpretation.

## Migration plan

1. Add pure option/forecast records and candidate planner behind existing V4 model seams; preserve existing legacy aim path.
2. Make AI use the planner only where it returns viable V4 options; compare deterministic outcomes with current policy fixtures.
3. Add additive GraphQL/DTO projection, regenerate typed frontend operations, and render server-authored distinct options in the existing dock.
4. Run targeted fixture, compatibility, architecture-purity, and calibrated regression checks; manually play only scenarios actually exercised.
5. No data migration or archive action occurs until implementation, evidence, and explicit approval are complete.
