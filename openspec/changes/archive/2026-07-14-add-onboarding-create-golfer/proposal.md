## Why

The frontend foundation (`add-frontend-foundation`) lets a player sign in and view their saved careers, but there is **no way to start one** — the saves list is always empty because the UI can't create a golfer. Onboarding is the entry to the whole game: per `docs/backend/player-experience.md`, the player **creates and owns a custom golfer** as the career they guide across decades. This slice delivers that create-your-golfer moment and closes the loop back to the existing saves list.

## What Changes

- Add an **onboarding / create-your-golfer flow** reachable from the saves screen (a "Start a new career" call-to-action, surfaced prominently in the empty state and as an action on the populated list).
- Add an onboarding route where the player composes a golfer:
  - **Identity** — first name, last name.
  - **Nationality** — one of the 16 engine `Nationality` values.
  - **Archetype** — one of the **five playing-style archetypes** (`POWER_HITTER`, `PRECISION_PLAYER`, `SHORT_GAME_ARTIST`, `ALL_ROUNDER`, `MENTAL_FORTRESS`), each presented with its play-style strengths/weaknesses. The three background archetypes are AI-only and are **not** offered.
  - **Starting age** — within the backend-enforced range **16–22** (the intersection of the golfer-factory 16–30 check and the Career-constructor 16–22 check).
- On submit, **orchestrate three typed GraphQL mutations in sequence** through the existing same-origin BFF: `createWorld(random seed)` → `createPlayer(id, …)` → `save(id, generated saveId)`, so the new career persists.
- Show a **confirmation of the created golfer** ("your golfer is ready", with their identity, archetype, nationality, and age), then return to the saves list where the new career now appears (`playerGolferId` set → "Career in progress").
- Handle validation, and backend failures gracefully — including the "a player has already been assigned" (BAD_REQUEST) case; an in-memory world created without a completed save is harmless (nothing persists until `save`).

## Capabilities

### New Capabilities
- `web-onboarding`: The create-your-golfer flow — the entry point from the saves screen, the golfer-composition form (identity, nationality, archetype, starting age) with its validation and archetype presentation, the three-mutation career-creation orchestration through the BFF, and the post-creation confirmation that lands the player back on their saves.

### Modified Capabilities
<!-- None. The saves-screen CTA is additive and reachable-from context, not a change to web-data-access requirements. No backend/engine spec requirements change. -->

## Impact

- **New frontend code**: an onboarding route + form components, archetype/nationality option data (mirroring the engine enums), the career-creation orchestration (three typed operations + generated saveId + random seed), and the confirmation view. A "Start a new career" CTA added to the existing saves screen/empty state.
- **New typed GraphQL operations** (codegen from the existing backend schema): `createWorld`, `createPlayer`, `save`. No new dependencies — reuses the foundation's stack (React Hook Form, Zod, TanStack Query, shadcn/Radix primitives, the BFF `/api/graphql` proxy).
- **Backend**: none. Consumes existing mutations `createWorld` / `createPlayer` / `save` (all owner-scoped via the token, already wired through the BFF).
- **Docs**: governed by the four `docs/frontend/*` files; realises them without modification.
