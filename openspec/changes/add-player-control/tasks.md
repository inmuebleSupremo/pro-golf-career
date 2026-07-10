## 1. Player-control state

- [x] 1.1 Create framework-free package `com.progolf.sim.control` (covered by the architecture-purity test).
- [x] 1.2 Add `PlayerControl` (designated `golferId`, mutable ordered `developmentFocus` `List<Attribute>`, mutable `resting` flag) with getters/setters; imports only `core`.

## 2. Development focus (modified: player-development)

- [x] 2.1 Add `ProgressionEngine.develop(current, age, supportFactor, List<Attribute> focus)` — awards the (coach-scaled) Development Points and, when focus is non-empty, greedily raises the focus attributes in priority order under the per-season cap and cost curve; otherwise delegates to the automatic allocation.

## 3. World wiring (modified: world-progression)

- [x] 3.1 In `World`, hold an optional `PlayerControl` and a `List<SponsorshipOffer>` of the player's pending offers; add `assignPlayer(golferId)` (validates the golfer exists) and read accessors (`playerGolferId`, `pendingSponsorships`).
- [x] 3.2 Player actions: `setDevelopmentFocus(List<Attribute>)`, `setResting(boolean)`, `acceptSponsorship(index)` (signs to the account within the max-concurrent cap and removes from pending).
- [x] 3.3 In `evolveGolfer`, if the golfer is the player's and a focus is set, develop with focus; else the existing automatic develop.
- [x] 3.4 In `resolveEvent` field selection, additionally exclude the player's golfer when resting.
- [x] 3.5 In `runFinancialSeason`, for the player's golfer, generate offers into the pending queue (replacing prior pending) instead of AI choose+sign; keep paying/evaluating existing agreements unchanged.
- [x] 3.6 Ensure every player branch is a strict override — an unassigned world takes only the existing paths.

## 4. Service surface (app)

- [x] 4.1 In `WorldService`, add `assignPlayer`, `setDevelopmentFocus`, `setResting`, `pendingSponsorships`, `acceptSponsorship`, delegating to the session's `World`.

## 5. Verification

- [x] 5.1 Development tests: `develop(..., focus)` raises the focused attributes and differs from the automatic allocation; empty focus equals the automatic path.
- [x] 5.2 Control tests: `PlayerControl` holds/updates decisions; resting toggles.
- [x] 5.3 World tests: with a designated player — development follows the focus; a resting player is excluded from fields and recovers; offers go to pending and `acceptSponsorship` signs one (respecting the cap) while unaccepted offers lapse next season.
- [x] 5.4 Compatibility test: two worlds with the same seed, one with no player assigned, produce identical outcomes; a non-player golfer in a player world is still AI-driven (auto-signs sponsors).
- [x] 5.5 Service test: a `@SpringBootTest` "plays a turn" — assign a player, set focus/rest, advance, accept a pending offer — and asserts the effects through `WorldService`.
- [x] 5.6 Confirm the full engine suite (290) and `ArchitecturePurityTest` still pass.
- [x] 5.7 Run `openspec validate add-player-control --type change --strict` and resolve findings.
