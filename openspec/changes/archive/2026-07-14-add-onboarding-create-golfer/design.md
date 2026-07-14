## Context

`add-frontend-foundation` established the app shell, token-driven design system, shadcn/Radix primitives, TanStack Query + typed GraphQL (codegen), the BFF (`/api/graphql` + auth cookies), and the authenticated saves screen. This slice adds the one thing missing to make that screen non-empty: creating a golfer.

Backend facts that shape the design (verified in the engine):

- **Mutations** (all owner-scoped via the token, already reachable through the BFF): `createWorld(seed: Long!, config: WorldConfigInput): WorldStatus!` → returns `{ id }`; `createPlayer(id, firstName, lastName, nationality, startAge, archetype): ID!` (enum args are engine **names**); `save(id, saveId): Boolean!`.
- **`Nationality`** — 16 values: USA, GBR, IRL, ESP, ZAF, AUS, JPN, KOR, SWE, GER, FRA, CAN, ARG, NZL, ITA, MEX.
- **`Archetype`** — 8 total; the **five playing-style** ones are the create-your-golfer choices (POWER_HITTER, PRECISION_PLAYER, SHORT_GAME_ARTIST, ALL_ROUNDER, MENTAL_FORTRESS), each carrying an attribute emphasis (strengths/weaknesses). The other three (GRASS_ROOTS_TALENT, TOP_COLLEGE_GRADUATE, FUTURE_PRODIGY) are neutral AI backgrounds — not offered.
- **Starting age** — `createPlayer` stacks two checks: the golfer factory validates `CREATION_MIN_AGE=16` … `CREATION_MAX_AGE=30`, and the `Career` constructor validates `CareerConstants.MIN_START_AGE=16` … `MAX_START_AGE=22`. The effective range is the intersection, **16–22** (an over-permissive 16–30 form was caught rejecting age 24 in live testing). The per-archetype ranges in `Archetype` are AI-routing metadata and are *not* enforced on human creation.
- **`createPlayer`** admits the golfer at the Development tour, assigns them as the session's player, and throws `IllegalStateException` ("a player has already been assigned") → surfaced by the API as **BAD_REQUEST** if a world already has a player.
- The `Save` DTO carries `saveId, savedAt, season, week, playerGolferId` — **not** the golfer's name; the saves list cannot show the name (no golfer-by-id query exists), which is why the confirmation step matters.

The four `docs/frontend/*` docs remain the constitution.

## Goals / Non-Goals

**Goals:**
- A reachable, token-consistent onboarding flow that composes a valid golfer and creates + persists the career, landing it in the existing saves list.
- Inputs constrained to backend-accepted values (nationality set, five archetypes, age 16–30), validated with React Hook Form + Zod before submission.
- A confirmation moment that gives the created golfer weight (the career the player owns) — the flow's designed highlight, shaped with the `impeccable` skill.
- Graceful handling of validation and backend failures, including "player already assigned".

**Non-Goals:**
- Career hub, shot/play surface, and any between-events management (development, staff, equipment, sponsorship, scheduling).
- Loading/resuming an existing save into a playable session (a later slice; saves remain read-only here).
- Attribute-level build customisation or previewing generated attributes — the archetype is the build choice; the engine derives attributes.
- Backend changes.

## Decisions

### D1 — Client-side orchestration of the three mutations (no new BFF endpoint)

**Decision:** Run `createWorld` → `createPlayer` → `save` as three sequential typed operations from a single TanStack Query `useMutation`, each going through the existing generic `/api/graphql` BFF proxy. No dedicated onboarding BFF route.

**Why:** The foundation already exposes a generic authenticated GraphQL path; chaining three codegen-typed operations there keeps the BFF surface minimal and every call type-checked against the schema. Partial failure is safe by construction: a world created without a completed `save` never persists (sessions are in-memory; only `save` writes to disk), so an aborted flow leaves no leftover career and the player can retry. The dependency (`createWorld.id` feeds the next two calls) is naturally expressed as an async sequence.

**Alternative considered:** a bespoke `POST /api/career` BFF route orchestrating server-side — rejected for this slice: it adds a second, non-GraphQL server surface for no correctness gain (the partial-failure case is already harmless), and loses the end-to-end typing the codegen path gives.

### D2 — Seed and saveId generation

**Decision:** Generate a **random seed** for `createWorld` client-side (a positive integer rendered as a string, since the `Long` scalar maps to `string` in codegen) — the player does not choose a seed. Generate a **unique `saveId`** client-side (e.g. `career-${crypto.randomUUID()}`) so repeated creations never collide.

**Why:** A career's world seed is an implementation detail, not an onboarding decision (surfacing it would be scope creep). `crypto.randomUUID()` is browser-native (no dependency) and guarantees uniqueness for the save slot.

### D3 — Archetype presentation as a selectable set, not a bare dropdown

**Decision:** Present the five archetypes as selectable cards/options, each with a one-line play-style description derived from its strengths/weaknesses (e.g. POWER_HITTER — "length off the tee and attacking irons, at the cost of touch around the greens"). The archetype and nationality option lists live in a small frontend constants module that mirrors the engine enums.

**Why:** The archetype is the most meaningful choice (it shapes the whole career), so it deserves more than a `<select>`; showing the trade-offs helps the player decide (Pillar 1 — decide better). Mirroring the enum names locally is the pragmatic monorepo choice; the names are validated for real by the backend on `createPlayer` (a bad name → BAD_REQUEST), so the local list is a UX convenience, not a trust boundary.

### D4 — Flow shape and routing

**Decision:** A public-to-authenticated route under the protected `(app)/` group (e.g. `/new`), reached from a "Start a new career" CTA on `/saves` (primary action in the empty state, secondary action on the populated list). On success, transition to an in-flow **confirmation** state showing the created golfer, with a "View your saves" action that navigates to `/saves` and refreshes it (invalidating the `saves` query so the new career shows).

**Why:** Keeping onboarding inside the authenticated shell reuses the existing layout, header, and route protection. The confirmation is a distinct state (not a toast) because the saves list can't display the golfer's name — this is the player's one clear look at the golfer they just created.

## Risks / Trade-offs

- **Orphan in-memory worlds on abandoned/failed flows** → Mitigation: none needed for correctness — unsaved sessions never persist and are the backend's to reclaim; the user-visible outcome is clean (no save). Documented so it isn't mistaken for a leak.
- **Local enum lists drifting from the engine** (new nationality/archetype added backend-side) → Mitigation: the backend is the real validator (bad name → BAD_REQUEST); a drifted list only under-offers options, never breaks creation. If the engine later exposes these as a query, the lists can be sourced from it.
- **Three sequential round-trips add latency to submit** → Mitigation: acceptable for a one-time onboarding action; show a pending state on the submit button; the calls are same-origin via the BFF.
- **Second `createPlayer` on the same world → BAD_REQUEST** → Mitigation: each submission creates a *fresh* world first, so this can't arise from normal use; the error is still handled and surfaced clearly as a guard.

## Open Questions

- The concrete play-style descriptions and the visual treatment of the archetype selection and confirmation moment are a design task (shaped with `impeccable`), not decided here.
