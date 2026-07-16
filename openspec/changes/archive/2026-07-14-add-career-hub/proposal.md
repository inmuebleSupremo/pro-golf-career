## Why

A player can now create a career (`add-onboarding-create-golfer`) and see it in their saves list, but the saves list is **read-only** — there is no way to open a career and look at its state. This slice makes saves actionable: **resume a save into a session and land on a read-only career hub** that shows where the career stands. It's the first screen that lets a player *re-enter* a career, and the shell that later slices (advancing time, playing events, management) will hang off.

## What Changes

- Add a **"Resume" action** to each save on the saves screen. It calls `load(saveId)`, which restores the save into a new session, then navigates to that session's career hub.
- Add a **career hub route** (`/career/[id]`) — a read-only overview of a loaded session, reading three queries in one request:
  - **World status** (`world`) — season, week, and the size of the active field.
  - **Career goals** (`careerGoals`) — each self-chosen goal with its progress (current vs target) and achieved state, using friendly labels for the `GoalType` names.
  - **Upcoming schedule** (`playerSchedule`) — the next eligible events with their week, tour tier, event prestige, and whether the player is entered.
- Handle an **expired/unknown session** gracefully: a loaded session is in-memory and ephemeral, so if `world(id)` returns null (server restarted, session gone), the hub explains this and links back to the saves list to reopen the save.
- **Known gap (flagged, not fixed here):** no GraphQL query exposes the player golfer's identity or stats outside a live event, so the hub shows career *state* (season/goals/schedule) but not the golfer's name or profile. A player-profile query is a separate future backend slice.

## Capabilities

### New Capabilities
- `web-career`: The read-only career hub and the resume/load action — the "Resume" entry point from the saves list, the `/career/[id]` overview (world status, career goals with progress, upcoming schedule), and the expired-session handling.

### Modified Capabilities
<!-- None. The "Resume" action is additive to the saves screen; the saves-list read requirement (web-data-access) is unchanged. No backend/engine spec requirements change. -->

## Impact

- **New frontend code**: a `/career/[id]` route + hub components (status, goals, schedule sections), a combined `CareerOverview` typed query, a `useLoadCareer` mutation, label maps for `GoalType` / `TourTier` / `EventPrestige`, and a "Resume" control on the saves rows.
- **New typed GraphQL operations** (codegen from the existing schema): `load` mutation, and a combined `CareerOverview` query (`world` + `careerGoals` + `playerSchedule`). No new dependencies — reuses the foundation stack (TanStack Query, the BFF proxy, shadcn/Radix primitives).
- **Backend**: none. Consumes existing `load` / `world` / `careerGoals` / `playerSchedule` (all owner-scoped via the token). The absent player-profile query is noted as a follow-up, not required here.
- **Docs**: governed by the four `docs/frontend/*` files; realises them without modification.
