# Design — add-staff-shot-effects

## Context

`SupportTeam.effects()` already aggregates a `mentalSupport` (psychologist) and `strategicSupport` (caddie) alongside the applied `developmentBonus` (coach) and `recoveryBonus` (fitness/physio). The two support values are computed and paid for but never read — `ShotResolver` has no support inputs. This change adds them through the same temporary-state seam equipment uses, and picks the shot-engine hooks that make each role meaningful in the world as it actually runs today.

## Decisions

### D1 — Support is temporary shot state, mirroring equipment
`GolferState` gains `mentalSupport` and `strategicSupport` (both [0,1], default 0), `PlayerState` carries them as transient pre-play values via `setSupport`, and `Player.toGolferState` surfaces them. This is the exact pattern equipment forgiveness/power already follow: neutral by default (so standard play is byte-identical), never written into permanent attributes, applied by the World before play and read by the resolver. It keeps `staff-influence`'s guarantee that staff never modify results directly — they shape the golfer's condition, and the shared engine resolves the shot.

### D2 — The hooks: caddie → mishits, psychologist → fatigue
The engine is inspected for hooks that are *live in the world*, not dormant:
- **Course Management already reduces mishit probability** (`mishitProbability *= (1 - MISHIT_MANAGEMENT_RELIEF * managementNorm)`). The **caddie** augments exactly this: `strategicSupport` multiplies the mishit probability by `(1 - strategicSupport)`. On-course strategy → fewer blow-ups. Neutral at 0.
- **Fatigue is the live condition** in world play; pressure is always 0 there (`toGolferState(0.0)`), so the composure/pressure term is dormant. The **psychologist** therefore reduces the *impact of fatigue*: an `effectiveFatigue = fatigue * (1 - mentalSupport)` is used wherever fatigue shapes the shot (the fatigue dispersion penalty and the fatigue carry reduction). Mental resilience keeps a tired golfer steady — and it matters *today*. At `mentalSupport` 0, `effectiveFatigue == fatigue`, so nothing changes. The pressure hook is deliberately left untouched for a future pressure model, where mental support would also apply.

This differentiates the two roles (execution vs resilience) and keeps both neutral-by-default.

### D3 — Reproducibility is scoped, not universal
Standard/neutral state is unchanged, so every shot- and tournament-level test (which builds `GolferState.fresh()` / neutral state) reproduces exactly, and two same-seed worlds stay byte-identical. But AI golfers hire caddies and psychologists over a career, so from the season they do, their shots tighten and multi-season *world* outcomes diverge from before this change. That is the intended effect (the roles now matter); any world test asserting a golden multi-season value is re-based, and reproducibility (same seed → same world) is re-asserted.

## Risks

- **Double-applying fatigue relief** — `effectiveFatigue` is computed once and substituted at both fatigue sites, so mental support scales fatigue consistently; a test asserts a tired golfer with mental support disperses no more than without.
- **Over/under-tuning** — magnitudes stay at the existing 0.20-per-quality on the tunables surface; tests assert direction (support never worsens the shot; caddie lowers mishit chance; psychologist softens fatigue) rather than exact values.
