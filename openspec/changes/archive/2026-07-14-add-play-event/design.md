## Context

`add-career-hub` gave a read-only view of a loaded session. This slice adds the play loop. Backend contract (verified in `WorldGraphQlMutationsTest`):

- **Loop `advanceWeek(id): WorldStatus!`** until `world(id).hasPendingEvent` is true — the world pauses at the player's tournament.
- **`currentSituation(id): ShotSituation`** — `{ holeNumber, par, shotNumber, strokesThisHole, distanceToPin, lie, pinLateral, minReach, maxReach }`; null when no shot is pending.
- **`eventLeaderboard(id): [LeaderboardRow!]!`** — `{ position, golfer{id,name}, score, roundsPlayed }`.
- **`playShot(id, decision: ShotDecisionInput!): ShotOutcome!`** — decision is `{ club, targetDistance, targetLateral?, strategy }` (club/strategy are enum names); outcome is `{ finalSurface, carry, lateral, distanceRemaining, hazardEntered, penaltyStrokes, strokes }`.
- **`simShot`/`simRound`/`simEvent`** and **`completeEvent(id): WorldStatus!`** (resumes the week; `hasPendingEvent` returns to false).
- Enums: **Club** {DRIVER, FAIRWAY_WOOD, HYBRID, IRON, WEDGE, PUTTER}; **Strategy** {CONSERVATIVE, BALANCED, AGGRESSIVE}.

Governing docs: the four `docs/frontend/*`.

## Goals / Non-Goals

**Goals:**
- Advance the calendar from the hub and, on reaching a tournament, play it shot-by-shot with club/target/strategy, or sim any part.
- A clear situation + leaderboard + decision surface; finish the event and return to the hub.

**Non-Goals:**
- Per-event pre-round tactics/config; a spatial hole map or shot visualisation (numeric situation only); lateral aiming (aim at the pin by default — `targetLateral` omitted); advancing multiple weeks at once; management between events. Each is a later slice.

## Decisions

### D1 — Play is a distinct route driven by the read state, not stored client state

**Decision:** `/career/[id]/play` reads `currentSituation` + `eventLeaderboard` + `world` (for `hasPendingEvent`). The screen is a function of those reads: a situation present → the decision surface; `hasPendingEvent` true but no situation → the event's play is done → offer **Finish event**; `hasPendingEvent` false → no event → redirect to the hub. After each shot/sim mutation, invalidate these queries so the next situation/standing render.

**Why:** the backend is the state machine; the UI mirrors it from reads rather than duplicating a client-side notion of "which shot/round." This keeps it correct across holes and rounds without the frontend modelling event structure.

### D2 — Target distance constrained to the shot's reach; aim at the pin

**Decision:** the target-distance control is bounded to `[minReach, maxReach]` from the situation (default to `distanceToPin` clamped into range). `targetLateral` is omitted in this slice (aim at the pin). Club and strategy are selects over the two enums.

**Why:** the reach range is per-shot and backend-enforced; constraining the control avoids invalid decisions. Lateral aiming is a refinement, not needed for a first playable loop.

### D3 — Sim shortcuts + explicit completion

**Decision:** expose **sim shot / sim rest of round / sim rest of event**; when play is finished, an explicit **Finish event** calls `completeEvent` and routes back to `/career/[id]`. Reuses `networkMode: "always"` (per the career-hub finding) so mutations/queries never pause.

**Why:** the game is about decades — playing every shot is optional (Pillar 2). Sim shortcuts make the round skippable; explicit completion matches the backend's `completeEvent` boundary.

## Risks / Trade-offs

- **Ephemeral session lost mid-event** (server restart) → the play reads return NOT_FOUND; reuse the hub's not-found handling to guide back to saves. Unsaved event progress is lost (acceptable; save boundaries are between events anyway).
- **Many small round-trips playing shot-by-shot** → acceptable; each is a same-origin BFF call, and sim shortcuts collapse the rest.
- **Between-rounds nulls** — if `currentSituation` is null while `hasPendingEvent` is true, treat play as finished and offer Finish event (sim any remainder first if needed). Verified against the real flow during implementation.

## Open Questions

- Visual treatment of the situation/leaderboard/decision surface is a design task (shaped with `impeccable`), not decided here.
