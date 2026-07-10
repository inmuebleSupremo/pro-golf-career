## Why

The player's only scheduling lever today is a crude blanket **rest** toggle — sit out everything, or play everything. But the vision commits to real season planning: the player **chooses which events to enter** from the calendar, weighing entry eligibility and the fatigue/travel cost of playing against the prize, ranking points, and prestige on offer (player-experience §8, a confirmed V1 decision). This is the lever the majors just made meaningful — a differentiated calendar of regular, signature, and major events is only a real decision if the player can pick their spots. This change adds **event-by-event entry**: the player is entered in every event they're eligible for by default, and strategically **skips** specific events to manage fatigue and travel, while the blanket rest toggle remains as a "skip everything" convenience.

This is the second half of management breadth (the weekly scheduling lever, after the seasonal staff/equipment levers).

## What Changes

- **`PlayerControl` gains per-event entry.** The player marks specific eligible events to **skip** (a set of event ids); everything else they are eligible for is entered by default. The existing `resting` flag stays as a blanket "skip all upcoming events." `isPlayerEntered` becomes: eligible (assigned, active, able to compete, and a member of the event's tour — or a major, which is cross-tour) **and** the player has not chosen to sit it out (not resting and not skipped).
- **A season-planner view.** `World.playerSchedule()` returns the upcoming events the player is eligible for — each with its week, tour tier, event prestige, and whether the player is currently entered — so the human can weigh which to play. New actions `skipEvent(id)` / `enterEvent(id)`; the existing `setResting` and the health/finances reads (`physicalStateOf`, `financialAccountOf`) surface the fatigue/travel trade-off.
- **The field draw honors the choice.** The two player-specific field filters (tour events and the cross-tour major draw) exclude the player from an event they have chosen to sit out, exactly as resting already does — so a skipped event auto-resolves without the player (and the player recovers that week instead), and an entered event they qualify for is played (interactively, via the existing `PlayableEvent`).
- Expose the new actions and the planner view on `WorldService`.

Explicitly out of scope: no new entry-requirement / qualification / exemption model (eligibility stays tour membership or the major's cross-tour ranking cut — unchanged); no travel-distance or venue geography model (travel is the existing flat cost); no change to how fatigue accrues or recovers (the trade-off uses the existing health and economy mechanics); no auto-planner/recommendation.

## Capabilities

### Modified Capabilities
- `player-control`: the player's scheduling is now event-by-event — entered in eligible events by default and choosing to skip specific ones (with the blanket rest toggle retained) — and can review their eligible upcoming schedule with each event's prestige and entry status.
- `world-progression`: when a world has a designated player, the World additionally applies the player's per-event entry choices (excluding skipped events from the player's field, resolving them automatically) alongside the existing resting behavior.

## Impact

- **Codebase**: `PlayerControl` (skip set + entry API), `World` (eligibility/sit-out split in `isPlayerEntered`, per-event checks in the two field filters, `skipEvent`/`enterEvent`/`playerSchedule`, a small `PlayerScheduleEntry` record), `WorldService` (expose the actions + view). No changes to health, economy, tournament, or the calendar.
- **Determinism**: entry is a player choice; the World's resolution of entered/skipped events uses the existing deterministic paths. A world with no player is unchanged (the AI has no skip set); resting behavior is preserved.
- **Boundary**: `World` stays a pure coordinator — it reads the player's entry choices and routes each event to the existing interactive/automatic resolution; it invents no new scheduling mechanics.
