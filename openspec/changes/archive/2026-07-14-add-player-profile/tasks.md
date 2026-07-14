## 1. Backend — the query

- [x] 1.1 Add `PlayerProfileDto` and `AttributeValueDto` (app.api.dto); add `PlayerProfile` + `AttributeValue` types and `playerProfile(id): PlayerProfile` to the schema
- [x] 1.2 Add `WorldService.playerProfile(ownerId, sessionId)` aggregating identity/attributes/world-ranking/earnings/tour/career-stats into the DTO (attributes in `Attribute` enum order; ranking nullable)
- [x] 1.3 Add the guarded resolver in `WorldQueryController` (`hasPlayer` → null); keep sim types out of the signature (ApiBoundaryTest)
- [x] 1.4 Add a backend test: create a player, query `playerProfile`, assert identity/attributes/tour/earnings and null when no player; run the full suite green

## 2. Frontend — hub header + leaderboard highlight

- [x] 2.1 Author the `PlayerProfile` typed operation; run codegen; add a `usePlayerProfile(id)` query hook
- [x] 2.2 Design the golfer header with the `impeccable` skill; build it on the career hub (name, nationality, age, archetype, world ranking, career earnings, attribute spread)
- [x] 2.3 Highlight the player's row on the play leaderboard by matching `playerProfile.golferId` against `golfer.id`

## 3. Verify

- [x] 3.1 Typecheck, lint, codegen clean; backend suite green
- [x] 3.2 End to end against the running backend: create/resume a career, see the golfer header on the hub, advance to an event, and see the player highlighted on the leaderboard
- [x] 3.3 Confirm the impeccable hook is clean; keyboard/focus/reduced-motion hold; run `openspec validate add-player-profile`
