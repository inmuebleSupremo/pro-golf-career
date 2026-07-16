## 1. Typed operations & hooks

- [x] 1.1 Author the play GraphQL operations (`advanceWeek`, `currentSituation`, `eventLeaderboard`, `playerMadeCut`, `playShot`, `simShot`, `simRound`, `simEvent`, `completeEvent`); run codegen and confirm type-check
- [x] 1.2 Add Club/Strategy option data + label maps; add `usePlayState(id)` (situation + leaderboard + hasPendingEvent) and mutation hooks (`useAdvanceWeek`, `usePlayShot`, `useSimShot`/`useSimRound`/`useSimEvent`, `useCompleteEvent`) that invalidate the play/career queries; use `networkMode: "always"`

## 2. Advance from the hub

- [x] 2.1 Add an "Advance week" action to the career hub (`advanceWeek`); on a pending event show/route to "Play your event", else refresh the hub with the new week

## 3. Play surface (impeccable)

- [x] 3.1 Design the play surface with the `impeccable` skill (situation + leaderboard + shot-decision) in the editorial/premium token direction
- [x] 3.2 Build `/career/[id]/play` (await async params) driven by `usePlayState`: situation present → decision surface; pending-but-no-situation → Finish event; no pending event → redirect to the hub
- [x] 3.3 Build the situation panel (hole · par · shot · distance · lie) and the leaderboard (player position + top of field)
- [x] 3.4 Build the shot-decision form: club select (6), target-distance control constrained to `[minReach, maxReach]`, strategy select (3) → Play shot; show the resolved outcome
- [x] 3.5 Build the sim controls (sim shot / rest of round / rest of event) and the Finish-event action (`completeEvent` → back to `/career/[id]`); handle loading/error/not-found

## 4. Verification

- [x] 4.1 Typecheck, lint, codegen clean
- [x] 4.2 Verify end to end against a running backend: create/resume a career, advance to an event, play a few shots (club/target/strategy) seeing outcomes, sim the rest, finish the event, and land back on the hub with the week advanced
- [x] 4.3 Verify the constrained target, the sim shortcuts, the no-pending-event redirect, and loading/error states; confirm the impeccable hook is clean and keyboard/focus/reduced-motion hold
- [x] 4.4 Run `openspec validate add-play-event`
