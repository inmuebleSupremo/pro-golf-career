## 1. PlayerControl: per-event entry

- [x] 1.1 Add `Set<Long> skippedEvents` to `PlayerControl` with `skipEvent(long)`, `enterEvent(long)` (remove), `isSkipped(long)`, and a read accessor; keep `resting`.

## 2. World: honor entry choices

- [x] 2.1 Split `isPlayerEntered(event)` into `isPlayerEligible(event)` (assigned + active + canCompete + (major or tour membership)) and `playerSitsOut(event)` (`resting || isSkipped(tournamentId)`); `isPlayerEntered = eligible && !sitsOut`.
- [x] 2.2 Update the two field filters in `buildEvent` (tour draw at the standings stream, and `majorField`) from `!(isPlayer(id) && playerControl.isResting())` to `!(isPlayer(id) && playerSitsOut(event))` so a skipped/rested player is excluded and the event auto-resolves.
- [x] 2.3 Add `PlayerScheduleEntry(long tournamentId, int week, TourTier tier, EventPrestige prestige, boolean entered)` record and `World.playerSchedule()` returning it for each upcoming event the player is eligible for (their tour's events + all majors), `entered` = not resting and not skipped.
- [x] 2.4 Player actions `skipEvent(long)` / `enterEvent(long)` on `World` (require player), delegating to `PlayerControl`.

## 3. App seam

- [x] 3.1 `WorldService`: expose `skipEvent` / `enterEvent` / `playerSchedule` (keep `setResting`).

## 4. Verification

- [x] 4.1 Default-enter: an assigned player with no scheduling choice is entered in their eligible events (plays them), exactly as before.
- [x] 4.2 Skip: skipping a specific eligible event excludes the player from it (it auto-resolves, the player does not appear in its result / career), while other eligible events are still entered.
- [x] 4.3 Re-enter: `enterEvent` after `skipEvent` restores default entry.
- [x] 4.4 Resting still works (blanket skip) — the existing resting test is unaffected.
- [x] 4.5 Planner view: `playerSchedule()` lists the player's eligible upcoming events with prestige and entry status; a skipped event shows `entered=false`.
- [x] 4.6 Unassigned unchanged; full suite + `ArchitecturePurityTest` pass.
- [x] 4.7 `openspec validate add-player-scheduling --type change --strict`.
