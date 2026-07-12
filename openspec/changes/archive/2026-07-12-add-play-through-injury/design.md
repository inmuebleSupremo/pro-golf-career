## Context

The live health model (`sim.health`) benches any injured golfer: `PhysicalState.availability()` returns `INJURED` (early) or `RECOVERING` (final rehab weeks, `rehabWeeksRemaining <= RECOVERING_WEEKS_THRESHOLD`), and `canCompete()` is `availability() == AVAILABLE`. The World gates every field by `canCompete()` (regular field, `majorField`, and `isPlayerEligible`), and `recoverHealth()` advances each active golfer's rehab by one week every week via `HealthSystem.recoverWeek`. Shots read a `GolferState` whose condition inputs (fatigue, pressure, equipment, support) the World syncs from the authoritative domains just before play — notably `state.setFatigue(physicalStates.get(id).fatigue())` in `buildEvent`. Because `MINOR` rehab is 2 weeks and the Recovering threshold is 2, every MINOR injury is in the Recovering stage from the moment it occurs; the early Injured stage only exists for MODERATE/SEVERE.

## Goals / Non-Goals

**Goals:**
- Let the controlled golfer choose to compete through a Recovering injury at a severity-scaled shot impairment.
- Make grinding cost something real and deterministic: competing freezes rehab (only rest heals).
- Keep AI field behaviour, world scoring/longevity, and reproducibility byte-identical.
- Route the impairment through the shot engine as a primitive condition input, exactly like fatigue, so the Health domain never owns shot resolution (availability boundary REQ).

**Non-Goals:**
- No AI play-through policy — AI golfers still rest recovering injuries and heal as today.
- No aggravation/RNG worsening (the chosen cost model is the deterministic rehab freeze, not a roll).
- No new human decision surface — the existing schedule (enter = grind, skip = rest) IS the choice.
- The early Injured stage remains fully benched for everyone.

## Decisions

**Decision: play-through eligibility == the Recovering stage.** `PhysicalState.canPlayThroughInjury()` is true exactly when `availability() == RECOVERING`. This covers every MINOR injury and the tail of MODERATE/SEVERE, and excludes the early Injured stage — matching the user's "MINOR + recovering stage" scope with one rule. `canCompete()` stays `AVAILABLE`-only and is unchanged, so AI gating is untouched.

**Decision: only the controlled golfer may play through.** The two field gates (`buildEvent` regular filter and `majorField`) and `isPlayerEligible` admit a Recovering golfer only when `isPlayer(id)`. A shared helper `canEnterField(id)` = `canCompete() || (isPlayer(id) && canPlayThroughInjury())` keeps the three sites consistent. AI Recovering golfers remain excluded → they rest → heal exactly as before, so all world tests and calibration hold. The player's existing skip/enter scheduling (`playerSitsOut`) becomes the grind-vs-rest decision with no new surface.

**Decision: impairment is a severity-scaled scalar, synced like fatigue.** `InjurySeverity` gains an `impairment` (MINOR ~0.05, MODERATE ~0.15, SEVERE ~0.30). `PhysicalState.injuryImpairment()` returns that value when `canPlayThroughInjury()`, else 0. `PlayerState` gains a transient `injuryImpairment` shot input (a scalar, NOT an `Injury` — mirrors how it already carries synced `fatigue`, equipment, and support); `buildEvent` syncs it for the whole field (0 for everyone except a grinding player). `GolferState` appends an `injuryImpairment` component (back-compat: existing shorter constructors stay valid prefixes, new field defaults 0). `Player.toGolferState` passes it through.

**Decision: apply the impairment in `ShotResolver` like fatigue, but psychologist-independent.** A physical injury is not soothed by a sports psychologist, so unlike `effectiveFatigue`/`effectivePressure` (which are cut by `mentalSupport`), the impairment is applied raw: it widens `sigmaLateral`/`sigmaDistance` (`INJURY_SIGMA_WEIGHT`), trims mean carry (`INJURY_MEAN_WEIGHT`), and lowers putt make-rate (`PUTT_INJURY_PENALTY`). At impairment 0 the arithmetic is identical to today (all terms are `1 + 0·w` or `−0`), so every existing shot/world test stays byte-identical.

**Decision: freeze rehab via the existing `committedThisWeek` set.** `HealthSystem.recoverWeek` gains a `competed` flag: fatigue still recovers, but rehab advances one week only when `!competed`. A back-compat 2-arg overload delegates with `competed = false` (a rested week), preserving current callers/tests. `recoverHealth()` runs in `finishWeek()`, after the week's fields are built and before the next week clears `committedThisWeek`, so `committedThisWeek.contains(id)` is precisely "competed this week." No new bookkeeping.

## Risks / Trade-offs

- **A grinding player never heals** → by design (that is the cost); rehab resumes the first week they rest. Bounded because the impairment discourages indefinite grinding and the player can always rest.
- **Reproducibility/fidelity** → the impairment is deterministic and synced identically down the auto and playable paths, so simmed == played for an impaired player, and same-seed worlds stay identical (AI impairment is always 0). Verified by leaving all world/shot tests unchanged and green.
- **Boundary creep** → the Health domain only computes a number (`injuryImpairment()`); shot resolution applies it, exactly as it already applies health-owned fatigue. No health type crosses into `sim.shot`.
- **Calibrating the penalty** → tuned empirically so a MINOR grind is a mild handicap and a Recovering SEVERE is a heavy one, without making play-through strictly dominated or strictly free; guarded by a `ShotResolver` test asserting monotonic degradation.
