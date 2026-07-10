# Design — add-player-scheduling

## Context

Player entry is gated in three places, all keyed on the `resting` blanket toggle: `isPlayerEntered(event)` (whether the World yields the event to the player) and the two field-draw filters in `buildEvent` (the tour-standings draw and the cross-tour `majorField`). This change turns the single blanket flag into real per-event entry while keeping resting as a convenience.

## Decisions

### D1 — Default-enter with strategic skip (the chosen model)
The player is entered in every eligible event by default; they mark specific events to **skip**. Playing is the baseline because not playing is career-ending (no events → no ranking → relegation), unlike not hiring staff. This preserves current behavior (an unconfigured player still competes), matches standard career-mode scheduling, and keeps the "decades, not micromanagement" pillar — the player intervenes only where a rest is strategically worth it.

- **Alternative — default-skip / opt-in:** consistent with the now-opt-in staff/equipment, but an idle player would play nothing and their career would stall entirely. Rejected as too harsh for the one decision that gates all competitive progress.

### D2 — Keep `resting` as a blanket "skip all"; add a per-event skip set
`PlayerControl` keeps `resting` and adds `Set<Long> skippedEvents` (event ids). The player sits out an event when `resting || skippedEvents.contains(id)`. Resting subsumes per-event skip (skip everything); per-event skip is the granular season-planning tool. This is additive — the existing resting requirement, API, and test are unchanged — and the two compose cleanly. Skips are keyed by the monotonic tournament id, so a past season's skips never collide with a future event; stale ids simply never match again.

### D3 — Split hard eligibility from the player's choice
`isPlayerEntered(event)` becomes `isPlayerEligible(event) && !playerSitsOut(event)`:
- `isPlayerEligible` = assigned + active + able to compete (health) + (the event is a major, i.e. cross-tour, **or** the player is a member of the event's tour). These are hard gates the player cannot override.
- `playerSitsOut(event)` = `resting || skipped(event id)` — the player's choice.
The two field filters use `!(isPlayer(id) && playerSitsOut(event))`, so a sat-out player is excluded from the field and the event auto-resolves; whether they *make* a major's field is still decided by the ranking cut in `majorField` (unchanged). An entered, eligible event the player qualifies for is yielded and played through the existing `PlayableEvent`.

### D4 — A season-planner view surfaces the trade-off
`World.playerSchedule()` returns, for each upcoming event the player is eligible for (their tour's events plus all majors), a `PlayerScheduleEntry(tournamentId, week, tier, prestige, entered)`. `entered` reflects the player's *choice* (not resting and not skipped), independent of transient health, so it reads as a plan. The reward side of the trade-off is the event's `prestige` (and tier); the cost side is read from the existing `physicalStateOf` (fatigue) and the flat travel/entry costs. The World computes no projections — it exposes the plan and the existing state, and the app presents the weigh-up.

## Risks

- **Stale skip ids accumulating** — harmless (never re-match a future id); not cleared, to avoid coupling the skip set to schedule regeneration. Noted rather than engineered away.
- **`entered` vs transient health** — the planner shows the player's choice, but an injury at event time still excludes them (the hard eligibility gate). Documented on the view; acceptable for V1 (a forward plan, not a guarantee).
- **Regression surface** — the resting behavior must be preserved bit-for-bit; `playerSitsOut` returns true whenever resting, so the existing resting test is untouched, and unassigned worlds are unaffected (no player, no skip set).
