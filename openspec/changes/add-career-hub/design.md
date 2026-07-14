## Context

`add-onboarding-create-golfer` lets a player create a career that lands in the saves list, but nothing consumes a save. This slice adds the resume/load action and a read-only career hub.

Backend facts (verified):

- **`load(saveId): WorldStatus!`** — `WorldService.load(ownerId, saveId)` restores the `SaveGame` into a fresh `World`, creates a **new in-memory `WorldSession`** with a random UUID owned by the caller, stores it in the session map, and returns its status. Each load makes a new session; loading the same save twice yields two independent sessions. Sessions are **not persisted** — they live for the server's lifetime and are keyed only in memory.
- **`world(id: ID!): WorldStatus`** — fields `season`, `week`, `activePopulation`, `hasPendingEvent`. **Verified in testing:** an unknown or not-owned session id does **not** return a null `world` — every field errors with `extensions.classification = NOT_FOUND` ("No world session with id: …") and top-level `data` is null. So the expired-session case is an **error to handle**, not a null to check.
- **`careerGoals(id): [CareerGoal!]!`** — `{ type (GoalType name), target (Long), current (Long), achieved }`. Boolean-style goals (`REACH_TOP_TOUR`, `WORLD_NUMBER_ONE`, `HALL_OF_FAME`) use target 1; targeted goals (`WIN_A_MAJOR`, `CAREER_WINS`, `CAREER_EARNINGS`) track current toward target. Empty when no player/goals.
- **`playerSchedule(id): [ScheduleEntry!]!`** — `{ tournamentId (Long), week, tier (TourTier name), prestige (EventPrestige name), entered }`. Empty when no player. TourTier ∈ {DEVELOPMENT, SECONDARY, PRIMARY, ELITE}; EventPrestige ∈ {REGULAR, SIGNATURE, MAJOR}.
- **No query returns the player golfer's identity or attributes** outside a live event (only the pending-event leaderboard exposes `Golfer{id,name}`). So the hub cannot show the golfer by name or profile.

The four `docs/frontend/*` docs remain the constitution.

## Goals / Non-Goals

**Goals:**
- A "Resume" action on each save that loads it and opens `/career/[sessionId]`.
- A read-only hub reading world status + goals (with progress) + upcoming schedule in one request, with friendly labels for enum names.
- Robust handling of loading, error, empty-goals, and expired-session states.

**Non-Goals:**
- Advancing the week/season, playing/simming events, or any management action (development, staff, equipment, sponsorship, scheduling edits, editing goals). Read-only.
- Showing the golfer's name/profile/stats/ranking — no query exposes them; a player-profile query is a separate future backend slice.
- Persisting the "current session" or resuming the same session across reloads/restarts (sessions are ephemeral; re-open from the save).
- Deleting saves.

## Decisions

### D1 — Ephemeral session model; hub keyed by session id, save is the durable handle

**Decision:** The hub route is `/career/[sessionId]`. "Resume" calls `load(saveId)` and navigates to the returned session's id. The **save** is the durable thing; the **session** is a disposable in-memory view of it. If the overview query fails with a **NOT_FOUND** classification (restart, or a stale link), the hub shows a "this session is no longer available" state linking back to `/saves` to reopen the save. (`gqlRequest` surfaces the GraphQL error's `extensions.classification`, and `isNotFound(error)` distinguishes it from a generic failure.)

**Query-client gotcha (found in testing):** TanStack Query's default `networkMode: "online"` **pauses a retrying query** when the browser reports `navigator.onLine === false` — which some embedded/preview browsers do — leaving a *failing* query stuck in `pending`/`paused` forever (successful queries are unaffected). The overview query therefore sets `networkMode: "always"` and `retry: false` (a not-found needn't retry); `networkMode: "always"` is also the QueryClient default, which is correct for a same-origin BFF app with no offline-first behaviour.

**Why:** matches the backend exactly — `load` mints a new session each time and sessions aren't persisted. Keying the hub on the session id (not the save id) is honest about what's being viewed; falling back to the saves list on session loss avoids a broken screen without pretending sessions survive restarts. Loading the same save twice creating two sessions is harmless here (both read-only, nothing writes back).

**Trade-off:** repeated resumes accumulate in-memory sessions until the server restarts. Acceptable for now (read-only, server-lifetime, no persistence); a future session-reuse or cleanup policy is out of scope.

### D2 — One combined `CareerOverview` query, not three round-trips

**Decision:** Fetch `world`, `careerGoals`, and `playerSchedule` for the session in a **single GraphQL document** (three root fields, one `$id`), driven by one TanStack Query. `useLoadCareer` is a separate `useMutation` wrapping `load`.

**Why:** the three reads are always shown together on one screen; one request is fewer round-trips and one coherent loading/error state. Both go through the existing typed `/api/graphql` BFF path (codegen-typed).

### D3 — Label maps for enum names; boolean vs targeted goal rendering

**Decision:** A small frontend module maps `GoalType`, `TourTier`, and `EventPrestige` names to display labels (mirroring the engine enums, as onboarding does). Goals render by shape: **boolean-style** goals (target 1) show achieved / not-yet; **targeted** goals show `current / target` progress (with `CAREER_EARNINGS` formatted as money). `achieved` drives a done treatment regardless.

**Why:** raw enum names (`WIN_A_MAJOR`) aren't player-facing copy; the current/target split from the API supports real progress display. Mirroring enums locally is the established monorepo choice (backend stays the validator).

### D4 — Read-only by construction

**Decision:** The hub renders only reads and navigation. No mutation is wired except `load` (the resume action that *reaches* the hub). No advance/play/edit controls exist in this slice.

**Why:** keeps the slice small and the spec's read-only requirement true by construction; the event loop and management surfaces are deliberately later slices that this shell will host.

## Risks / Trade-offs

- **Thin hub without the golfer's identity** → Mitigation: none available via the API; flagged prominently as a known gap and a future backend slice. The career *state* shown is still meaningful.
- **Stale `/career/[id]` links after a restart** → Mitigation: the expired-session state (D1) turns a dead link into a clear path back to saves.
- **In-memory session accumulation on repeated resumes** → Mitigation: accepted for a read-only, non-persisted view; revisit with a session policy when the play/advance loop lands.

## Open Questions

- The exact visual treatment of goals (progress bars vs text), the schedule list, and the world-status header is a design task (shaped with `impeccable`), not decided here.
- Whether a later slice adds a backend player-profile query (to give the hub a real golfer header) or reuses the leaderboard shape — deferred.
