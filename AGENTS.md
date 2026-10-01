# AGENTS.md — Pro Golf Career

Orientation for any AI agent (Codex, Claude, etc.) working in this repository. Read this first, then the
directory-level `AGENTS.md` for the part you're touching (`backend/AGENTS.md`, `frontend/AGENTS.md`).

---

## What this project is

**Interactive Pro Golf Career Simulation** — a long-term, strategic single-player sports-management game. You
create one professional golfer and guide their multi-decade career: you make the decisions (shot selection on
the course, and management off it — development, schedule, staff, equipment, sponsorship, finances) and a
simulation resolves the outcomes. No reflexes, no swing meter; success comes from decisions, attributes,
conditions, and controlled variance.

The authoritative product vision and numbered requirements (REQ-xxx) live in
[`docs/backend/explore.md`](docs/backend/explore.md) — the north star. The player-facing decision surface is
distilled in [`docs/backend/player-experience.md`](docs/backend/player-experience.md).

Current state: **the full game is built and playable end-to-end** — Spring Boot simulation + GraphQL API +
JWT auth + a complete Next.js frontend (create-golfer → play/sim events including majors → full career
management → season review/off-season → save/load). Persistence is filesystem-based today (Postgres was the
original V1 intent but is not yet wired in — see "Known deviations" below).

---

## Repository layout

```
/backend        Spring Boot (Java 21, Maven) — simulation engine + GraphQL API + auth + persistence
/frontend       Next.js 16 (App Router, TypeScript, pnpm) — the player-facing UI (BFF + client)
/docs           Design & product docs (vision, tech, frontend design system, course-hole SVGs)
/img-assets     Source images for course/event scenery (parkland, links, desert, tropical, mountain, majors)
/openspec       OpenSpec scaffolding (spec-driven change workflow; config template only)
docker-compose.yml   Runs backend (:8080) + frontend (:3000) together for local E2E
```

Two independent build systems — **Maven** for `backend/`, **pnpm** for `frontend/`. There is no root package
manager; treat the two as sibling projects that share a repo.

---

## The core architectural principle: sim-pure / app-wraps

This is the single most important rule in the codebase.

- **`com.progolf.sim.*` is a pure, framework-free simulation engine.** No Spring, no annotations, no I/O, no
  web types. It is deterministic given a seed. All game logic lives here, split into ~20 domains
  (`player`, `shot`, `play`, `tournament`, `tour`, `ranking`, `economy`, `equipment`, `staff`, `progression`,
  `career`, `achievement`, `world`, `course`, `weather`, `health`, `media`, `population`, `statistics`, …).
- **`com.progolf.app.*` is the thin application layer** that wraps the engine: GraphQL resolvers, auth,
  persistence, and the `WorldService`/`WorldSession` seam a human acts through. Spring lives only here.
- The boundary is **enforced by a test** — `backend/src/test/java/com/progolf/sim/ArchitecturePurityTest.java`
  fails the build if `sim.*` ever imports Spring or the app layer. Do not break it.

The same engine seams serve the human player and the AI field — the human occupies the same interfaces the AI
does, with no separate rules and no hidden advantage.

**The GraphQL resolvers attach at the `WorldService` boundary.** When adding a player-facing capability: build
it in the relevant `sim.*` domain, expose it through `WorldService`, then add a resolver + schema field, then
the frontend operation.

---

## Frontend ↔ backend contract

- The backend exposes **GraphQL** at `POST /graphql` (schema-first; single source of truth is
  [`backend/src/main/resources/graphql/schema.graphqls`](backend/src/main/resources/graphql/schema.graphqls)).
  GraphiQL explorer at `/graphiql` in dev.
- The frontend is a **BFF (backend-for-frontend)**: browser code never calls the Spring backend directly.
  Next route handlers under `frontend/src/app/api/` (`/api/graphql`, `/api/auth/*`) call the backend
  server-side over `BACKEND_INTERNAL_URL`, attaching the session from httpOnly cookies. `/api/graphql` is the
  authoritative auth boundary.
- **Types are generated, not hand-written.** `graphql-codegen` reads the checked-in `schema.graphqls` and emits
  typed operations into `frontend/src/lib/graphql/generated/` (gitignored). It runs automatically before
  `dev`/`build`/`typecheck`; re-run manually with `pnpm codegen`. **After changing the GraphQL schema, re-run
  codegen so the frontend types don't drift.**

---

## Build, test, run

### Backend (`backend/`, Java 21 + Maven — no wrapper, use a system `mvn`)
```bash
cd backend
mvn test              # full JUnit suite (includes ArchitecturePurityTest)
mvn spring-boot:run   # start API on http://localhost:8080  (GraphiQL at /graphiql)
mvn package           # build the runnable jar
```

### Frontend (`frontend/`, Next.js + pnpm)
```bash
cd frontend
pnpm install
pnpm dev              # http://localhost:3000  (runs codegen first)
pnpm typecheck        # tsc --noEmit (runs codegen first)
pnpm lint             # eslint
pnpm build            # production build
```
Set `BACKEND_INTERNAL_URL` (see `frontend/.env.example`; defaults to `http://localhost:8080`). Run the backend
first so the frontend has an API to call. Before starting the backend, copy the root `.env.example` to `.env` and
set `PROGOLF_JWT_SECRET` to a unique value of at least 32 characters.

### Both together (Docker)
```bash
docker compose up --build   # frontend :3000, backend :8080, shared data volume
```
Docker Compose requires `PROGOLF_JWT_SECRET` in a root `.env`; copy `.env.example` and set a unique value of at
least 32 characters.

---

## Persistence & auth (as built)

- **Saves** and **user accounts** are stored as JSON files on disk, not in a database.
  - Users → `backend/users/` (`PROGOLF_USERS_DIR`), Saves → `backend/saves/` (`PROGOLF_SAVES_DIR`).
  - Implemented by `FilesystemUserStore` and `FilesystemSaveGameStore`; a whole `WorldSession` is snapshotted
    via Jackson (`SimSnapshotModule`) and re-hydrated on load.
- **Auth**: Spring Security + JWT (HS256, Nimbus) — short-lived access token (15 min) + long-lived refresh
  token (14 days), BCrypt password hashing. The frontend keeps tokens in httpOnly cookies.

---

## Conventions

- **Branch + PR per unit of work.** `develop` is the main branch. Feature branches are `feat/<slug>`; commit
  messages are conventional (`feat(scope): …`, `docs(scope): …`). Do not commit or push unless asked.
- **Backend**: keep `sim.*` pure; put Spring/I/O in `app.*`. Prefer records + immutability in the engine. Tests
  are first-class — there is heavy coverage and calibration/regression harnesses for realism (scoring, attribute
  balance, world success). If you touch the engine, run `mvn test`.
- **Frontend**: this is **not stock Next.js** — it pins Next 16 with renamed APIs (e.g. middleware → "proxy").
  Follow the design system in [`docs/frontend/`](docs/frontend/) (technology, boundaries, design-tokens,
  references) before any UI work — it is a real, enforced house style (tokens only, no default shadcn styling,
  Server Components first). Read the relevant guide in `node_modules/next/dist/docs/` before using an unfamiliar
  Next API.

---

## Where to look for things

| You want to… | Look at |
|---|---|
| Understand the product/requirements | [`docs/backend/explore.md`](docs/backend/explore.md) (REQ-xxx), [`docs/backend/player-experience.md`](docs/backend/player-experience.md) |
| Understand the as-built stack | [`docs/backend/tech_stack.md`](docs/backend/tech_stack.md) |
| Change game logic | `backend/src/main/java/com/progolf/sim/<domain>/` |
| Change the API | `schema.graphqls` + `backend/.../app/api/*Controller.java` + `WorldService` |
| Change the UI | `frontend/src/app/` (routes), `frontend/src/components/`, `frontend/src/lib/` |
| Know the frontend design rules | [`docs/frontend/`](docs/frontend/) |

---

## Known deviations from the original plan (do not treat the old plan as current truth)

The original [`docs/backend/tech_stack.md`](docs/backend/tech_stack.md) named a **Vite + React Router + Apollo**
frontend and **PostgreSQL**. The project diverged during the build:

- Frontend is **Next.js 16 (App Router) + TanStack Query + graphql-codegen**, not Vite/Apollo/React Router.
- Persistence is **filesystem JSON**, not PostgreSQL (Postgres is a possible future adapter, not present today).

`tech_stack.md` has been updated to reflect what was actually built; this note is a pointer in case you find
older references elsewhere.
