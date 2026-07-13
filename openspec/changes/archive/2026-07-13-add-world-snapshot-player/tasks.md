## 1. PlayerControl capture (sim.control)

- [x] 1.1 Add a `PlayerControl.Snapshot` record (golferId, developmentFocus, resting, skippedEvents, careerGoals) + `snapshot()`.
- [x] 1.2 Add `PlayerControl.restore(Snapshot)` rebuilding via the existing setters; expose the skipped-event ids if not already readable (add a read accessor and re-apply `skipEvent` on restore).

## 2. WorldSnapshot fields

- [x] 2.1 Extend `WorldSnapshot` with `PlayerControl.Snapshot playerControl` (nullable), `List<SponsorshipOffer> playerPendingOffers`, `List<StaffMember> playerPendingStaff`, `List<EquipmentItem> playerPendingEquipment`, `Set<CareerGoal> achievedGoals`.

## 3. World snapshot / restore

- [x] 3.1 In `World.snapshot()`, remove the `playerControl != null` rejection (keep the pending-event guard); capture the player fields null-safely.
- [x] 3.2 In `World.restore()`, rebuild `playerControl` (if present) and refill the pending offers/staff/equipment lists and achieved goals.

## 4. Tests

- [x] 4.1 Extend `WorldSnapshotRoundTripTest`: a player world (designated golfer, non-default development focus, resting/skip choices, career goals, pending offers) round-trips — `snapshot → restore → advanceSeason N` equals `advanceSeason N`.
- [x] 4.2 Autonomous round-trip still green (player fields null/empty); an autonomous snapshot restores to `playerControl == null`.
- [x] 4.3 The pending-event guard still rejects a snapshot mid-player-event.

## 5. Verify

- [x] 5.1 Full backend suite green.
