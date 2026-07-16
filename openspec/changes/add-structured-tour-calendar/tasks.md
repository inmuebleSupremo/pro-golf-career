## 1. Tour Championship prestige level

- [x] 1.1 Add `TOUR_CHAMPIONSHIP` to `EventPrestige`, ordered between `SIGNATURE` and `MAJOR`; update any `isMajor()`-style helpers and exhaustive switches over the enum.
- [x] 1.2 Extend the prestige reward weighting (ranking points + prize multipliers) so the order Regular < Signature < Tour Championship < Major holds monotonically. (Ranking 2.25, purse 3.0 via `purseWeight()`.)
- [x] 1.3 Extend situational closing-round pressure scaling for the new level (PRESSURE 0.9, between Signature 0.8 and Major 1.0).
- [x] 1.4 Extend course-setup difficulty scaling for the new level (setup bump 0.25, between Signature 0.14 and Major 0.36).
- [ ] 1.5 Frontend label for `TOUR_CHAMPIONSHIP` — DEFERRED to when branches converge on develop (this branch has no frontend; prestige surfaces as the enum name with a raw-value fallback meanwhile).

## 2. Cadence definition

- [ ] 2.1 Add cadence constants/params to `WorldConstants` / `WorldConfig`: `STRUCTURED_MIN_WEEKS` (~20), target events per tier for the structured path (~14), and the anchor weeks (major weeks, championship weeks per tier, signature spotlight weeks incl. the collision week).
- [ ] 2.2 Define the season cadence as a declarative template: a set of `{week, tier, prestige}` anchors plus a fill rule that lays Regular events on remaining "on" weeks up to the target density. Keep it seed-independent (courses stay seed-assigned).
- [ ] 2.3 Encode per-tier rhythm: majors on Elite chapter weeks (still `tier = ELITE`, field cross-tour); Development/Secondary/Primary signature spotlights offset from Elite major weeks; each tour's championship at its final week, Development one week before Elite.

## 3. Schedule generation

- [ ] 3.1 Rewrite `World.generateSchedule` to build the structured cadence (Section 2) when `weeksPerSeason >= STRUCTURED_MIN_WEEKS`.
- [ ] 3.2 Keep the existing even-spread logic as the `< STRUCTURED_MIN_WEEKS` proportional fallback (preserves the base schedule contract for small/test configs).
- [ ] 3.3 Confirm majors still route through `majorField` (cross-tour strongest field) and the one-event-per-week / `committedThisWeek` rule holds with the denser calendar.

## 4. Reward normalization (balance)

- [ ] 4.1 Scale per-event Regular reward magnitudes so a *season's* total ranking points and prize stay ~constant vs. the pre-change 6-events/tier baseline (Signature/Championship/Major keep their relative premiums). (Pending the reward-normalization open question.)
- [ ] 4.2 Re-run the balance/behavior suites that pin scoring, Hall-of-Fame election timing, career length, and world-scoring recalibration; adjust constants if the density shift moved them.

## 5. Tests

- [ ] 5.1 Structured-cadence test (standard scale): majors on the fixed chapter weeks; exactly one championship per tour at its final week; Development championship before Elite; signatures spotlighted (not all in opening weeks); the mid-season Elite+Development signature collision; target density per tier.
- [ ] 5.2 Determinism test: same-seed standard-scale schedules are identical (weeks + prestige).
- [ ] 5.3 Degradation test: a 6-week config still produces a valid, reproducible schedule via the fallback (existing small-config schedule/progression tests stay green).
- [ ] 5.4 Prestige-weighting tests: Tour Championship rewards/pressure/setup sit strictly between Signature and Major.
- [ ] 5.5 Performance check: advancing several standard-scale seasons unattended stays responsive (record a rough per-season time; flag if a full season exceeds a couple of seconds).

## 6. Wrap-up

- [ ] 6.1 Run the full backend suite; fix any fallout from the enum/weighting changes.
- [ ] 6.2 Verify the frontend still renders the schedule and the new prestige label (no schema break; regenerate types if the enum surfaces in a typed field).
