## Why

The career hub is read-only — you can see a career but not play it. This slice delivers the game's core: **advance the calendar to your next tournament and play the round shot-by-shot** (club / target / strategy), with the simulation resolving each outcome. It turns the hub from a viewer into somewhere you actually play.

## What Changes

- Add an **"Advance week"** action to the career hub (`advanceWeek`). When advancing lands on the player's tournament, the hub surfaces a **"Play your event"** entry; otherwise it shows the new week.
- Add a **play screen** (`/career/[id]/play`) for a pending event:
  - The current **shot situation** (hole, par, shot number, distance to pin, lie) and the live **leaderboard** (the player's position and the top of the field).
  - A **shot decision**: club (of 6), target distance (within the shot's reach range), and strategy (conservative / balanced / aggressive) → **Play shot**, showing the resolved outcome.
  - **Sim shortcuts**: sim the current shot, the rest of the round, or the rest of the event.
  - When play is done, **Finish event** (`completeEvent`) resumes the week and returns to the hub.
- Handle the states: no pending event (return to hub), loading/error, and event completion.

## Capabilities

### New Capabilities
- `web-play`: Advancing the calendar to a tournament and playing it — the advance action, the shot-by-shot decision surface (situation + leaderboard + club/target/strategy), the sim shortcuts, and event completion back to the hub.

### Modified Capabilities
<!-- None. The advance action and play CTA are additive to the career hub; the web-career read requirements are unchanged. -->

## Impact

- **New frontend code**: a `/career/[id]/play` route + play components (situation, leaderboard, shot-decision form, sim controls, outcome feedback), typed operations, and mutation/query hooks. The career hub gains an advance action + play CTA.
- **New typed GraphQL operations** (codegen from the existing schema): `advanceWeek`, `currentSituation`, `eventLeaderboard`, `playerMadeCut`, `playShot`, `simShot`, `simRound`, `simEvent`, `completeEvent`. No new dependencies.
- **Backend**: none. Consumes existing owner-scoped play mutations/queries.
- **Docs**: governed by `docs/frontend/*`; realised without modification.
