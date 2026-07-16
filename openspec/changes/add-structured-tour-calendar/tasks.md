## 1. Tour Championship prestige level

- [x] 1.1 Add `TOUR_CHAMPIONSHIP` to `EventPrestige`, ordered between `SIGNATURE` and `MAJOR`; update any `isMajor()`-style helpers and exhaustive switches over the enum.
- [x] 1.2 Extend the prestige reward weighting (ranking points + prize multipliers) so the order Regular < Signature < Tour Championship < Major holds monotonically. (Ranking 2.25, purse 3.0 via `purseWeight()`.)
- [x] 1.3 Extend situational closing-round pressure scaling for the new level (PRESSURE 0.9, between Signature 0.8 and Major 1.0).
- [x] 1.4 Extend course-setup difficulty scaling for the new level (setup bump 0.25, between Signature 0.14 and Major 0.36).
- [ ] 1.5 Frontend label for `TOUR_CHAMPIONSHIP` — DEFERRED to when branches converge on develop (this branch has no frontend; prestige surfaces as the enum name with a raw-value fallback meanwhile).

## 2. Cadence definition

- [x] 2.1 Cadence constants in `SeasonCadence`: `TARGET_EVENTS_PER_TIER`=14, `MAJOR_WEEKS`={7,14,21,27}, championship weeks (Elite 30 / non-Elite 29), signature spotlight weeks per tier (incl. the week-17 collision), Elite rest weeks. `appliesTo(config)` gates on the standard 30-week / 4-major profile.
- [x] 2.2 Declarative template: `SeasonCadence.forSeason` places anchors (majors/championship/signatures) then fills Regulars evenly on remaining eligible weeks (excluding anchors + rest weeks) to the target density. Seed-independent; World assigns courses/ids.
- [x] 2.3 Per-tier rhythm: majors on Elite chapter weeks (tier=ELITE, cross-tour field); Development/mid signatures offset from major weeks; each tour's championship at its final week, non-Elite one week before Elite; non-Elite tours rest during majors, Elite rests the week before each.

## 3. Schedule generation

- [x] 3.1 `World.generateSchedule` → `generateStructuredSchedule()` (via `SeasonCadence.forSeason`) at the standard profile.
- [x] 3.2 Old even-spread logic kept verbatim as `generateProportionalSchedule()` fallback for non-standard configs (small-config tests unchanged).
- [x] 3.3 Majors stay Elite-tagged / cross-tour via `majorField`; one-event-per-week holds (a tier never plays twice in a week; `committedThisWeek` handles cross-tour overlap on major weeks — asserted by `SeasonCadenceTest`).

## 4. Reward normalization (balance)

- [ ] 4.1 Per-season reward normalization — **PAUSED for user decision.** The ~2× density did NOT break any balance/behavior suite (see 4.2), so the a-priori rationale (protect tuned balance) may not require action; a global reward down-scale would corrupt small-config balance, and a structured-only scale is speculative. Deciding whether to implement vs. accept density-natural growth.
- [x] 4.2 Balance/behavior suites (HoF election timing `WorldHallOfFameTest`, longevity `WorldLongevityTest`, economy `WorldEconomyTest`/`WorldEconomyStakesTest`, scale `WorldScaleTest`) all pass at the new density — 523 tests green.

## 5. Tests

- [x] 5.1 `SeasonCadenceTest`: majors on {7,14,21,27}; one championship per tour at its final week; Dev championship (29) before Elite (30); signatures spotlighted (a signature past week 15); the Elite+Development week-17 collision; target density (14) per tier; no tier plays twice a week.
- [x] 5.2 Determinism: `SeasonCadence.forSeason` is pure/identical across calls; same-seed World schedules are already pinned byte-identical by `WorldSnapshotRoundTripTest` (archives are in the snapshot).
- [x] 5.3 Degradation: `appliesTo` is false for the 6-week config; existing small-config schedule/progression tests stay green via the proportional fallback.
- [x] 5.4 `EventPrestigeTest`: Tour Championship ranking/purse/pressure sit strictly between Signature and Major.
- [x] 5.5 Performance: a standard-scale season roughly doubled in sim time (`WorldScaleTest` 1.3s→2.5s for 3 seasons ≈ 0.8s/season; `WorldHallOfFameTest` many seasons in 4.6s) — well within responsive; no action needed.

## 6. Wrap-up

- [ ] 6.1 Run the full backend suite; fix any fallout from the enum/weighting changes.
- [ ] 6.2 Verify the frontend still renders the schedule and the new prestige label (no schema break; regenerate types if the enum surfaces in a typed field).
