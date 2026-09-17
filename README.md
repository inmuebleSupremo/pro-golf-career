# Pro Golf Career

An interactive professional-golf **career-simulation game**. You create one golfer and guide their multi-decade
career — making the strategic decisions (shot selection on the course; development, schedule, staff, equipment,
sponsorship, and finances off it) while a simulation resolves the outcomes. No reflexes, no swing meter: you win
by deciding better.

## Repository

| Path | What |
|---|---|
| [`backend/`](backend) | Spring Boot (Java 21) — pure simulation engine + GraphQL API + JWT auth + save/load |
| [`frontend/`](frontend) | Next.js 16 (App Router, TypeScript, pnpm) — the player-facing UI |
| [`docs/`](docs) | Product vision & requirements, tech stack, frontend design system, course-hole art |

## Run it locally

Everything together with Docker:

```bash
docker compose up --build
```

Frontend → http://localhost:3000 · Backend → http://localhost:8080 (GraphiQL at `/graphiql`). Override
`PROGOLF_JWT_SECRET` via a root `.env` for anything beyond local dev.

Or run each side on its own — see [`backend/AGENTS.md`](backend/AGENTS.md) and
[`frontend/README.md`](frontend/README.md).

## Documentation

- **Working in this repo (any AI agent):** start with [`AGENTS.md`](AGENTS.md), then the directory-level
  `AGENTS.md` for the area you're touching.
- **Product vision & requirements:** [`docs/backend/explore.md`](docs/backend/explore.md) (the north star) and
  [`docs/backend/player-experience.md`](docs/backend/player-experience.md).
- **As-built technology:** [`docs/backend/tech_stack.md`](docs/backend/tech_stack.md).
- **Frontend design system:** [`docs/frontend/`](docs/frontend/).

## Architecture in one line

The game logic is a **pure, framework-free simulation** (`com.progolf.sim.*`, deterministic from a seed); a thin
Spring layer (`com.progolf.app.*`) wraps it with a GraphQL API, auth, and filesystem persistence; a Next.js BFF
+ UI consumes that API. The `sim`/`app` boundary is enforced by a test. See [`AGENTS.md`](AGENTS.md).
