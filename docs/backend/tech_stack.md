# TECH_STACK.md

# Professional Golf World Simulation

## Technology Stack

---

# Purpose

This document defines the authoritative technology stack for the project.

It describes **how** the application will be implemented.

Gameplay behaviour and product requirements remain defined exclusively within `EXPLORE.md`.

> **As-built note (updated 2026-08-20).** This document was originally written as an intent. Two decisions
> changed during the build and the tables below have been corrected to reflect what actually shipped:
> - **Frontend:** built with **Next.js 16 (App Router) + TanStack Query + graphql-codegen**, *not* Vite +
>   React Router + Apollo.
> - **Database:** persistence is **filesystem JSON** today (users + saves as files). **PostgreSQL is planned
>   but not yet wired in** — it would arrive as a store adapter behind the existing `UserStore` /
>   `SaveGameStore` interfaces.
>
> Sections still marked as intent (containerisation prod file, standard repo files, `.env.production`, etc.)
> are directional and may not all exist yet.

---

# Core Principles

* Prefer mature, well-supported technologies.
* Minimise unnecessary complexity.
* Prefer convention over configuration.
* Favour long-term maintainability over novelty.
* Container-first development.
* Environment-variable driven configuration.

---

# Backend

| Technology      | Version           |
| --------------- | ----------------- |
| Java            | 21 LTS            |
| Spring Boot     | 3.x               |
| Spring Security | Latest compatible |
| Spring GraphQL  | Latest compatible |
| Maven           | Latest            |

Responsibilities:

* Business logic
* Simulation engine
* GraphQL API
* Authentication
* Database access

---

# Frontend (as built)

| Technology                   | Version        |
| ---------------------------- | -------------- |
| Next.js (App Router)         | 16             |
| React                        | 19             |
| TypeScript                   | 5.x            |
| pnpm                         | (workspace)    |
| Tailwind CSS                 | 4              |
| shadcn/ui + Radix UI         | Latest         |
| TanStack Query               | 5.x            |
| GraphQL Code Generator       | 7.x (client preset) |
| React Hook Form + Zod        | Latest         |
| Motion                       | 12.x           |

Responsibilities:

* User Interface
* GraphQL client (typed operations generated from the backend schema)
* Routing (App Router) + a BFF layer (Next route handlers proxy the backend, hold the session)
* State management (TanStack Query)
* Visualisation

> Routing/state are handled by Next.js + TanStack Query rather than React Router + Apollo (the original intent).
> The full frontend design system is documented in [`../frontend/`](../frontend/).

---

# Database / Persistence

**Current (as built):** filesystem JSON, no database.

* User accounts → JSON files under `PROGOLF_USERS_DIR` (`FilesystemUserStore`).
* Career saves → JSON files under `PROGOLF_SAVES_DIR` (`FilesystemSaveGameStore`); a full `WorldSession` is
  snapshotted/rehydrated via Jackson (`SimSnapshotModule`).

**Planned:** PostgreSQL 17 as a store adapter behind the existing `UserStore` / `SaveGameStore` interfaces —
for persistent world state, accounts, saves, and historical data. Not yet wired in.

---

# API

GraphQL shall be the primary API between frontend and backend.

REST endpoints should only exist where required (health checks, authentication helpers, etc.).

---

# Authentication

Authentication shall use:

* Spring Security
* JWT Access Tokens
* Refresh Tokens
* BCrypt password hashing

Future enhancements may include:

* Email verification
* Password reset
* OAuth providers

Keycloak is intentionally not used in Version 1.

---

# Containerisation

Docker shall be used for all environments.

Repository shall include:

* Dockerfile (backend)
* Dockerfile (frontend)
* docker-compose.yml (local development)
* docker-compose.prod.yml (production)

---

# Environment Configuration

Configuration shall be environment-variable driven.

Repository shall include:

* `.env.example`
* `.env.local`
* `.env.production`

Secrets shall never be committed to Git.

---

# Standard Repository Files

The repository should include:

* README.md
* LICENSE
* .gitignore
* .editorconfig
* .gitattributes
* .env.example
* docker-compose.yml
* docker-compose.prod.yml

---

# Suggested Repository Structure

```text
/backend
/frontend
/infrastructure
/docs
```

---

# Development Tools

Recommended tooling:

* Docker Desktop
* IntelliJ IDEA (Backend)
* VS Code (Frontend)
* Postman / Insomnia (optional)
* pgAdmin (optional)

---

# Git

* Git for version control
* GitHub for repository hosting
* Feature branch workflow
* Pull Requests for all changes

---

# Testing

Backend:

* JUnit
* Spring Boot Test

Frontend (as built): no unit-test runner is wired in yet. Quality gates are `pnpm typecheck` (tsc), `pnpm lint`
(ESLint), and `pnpm format:check` (Prettier). Vitest + React Testing Library remain the intended choice when a
runner is added.

---

# Non-Negotiable Technology Decisions

The project SHALL use:

* Spring Boot
* React + Vite
* TypeScript
* PostgreSQL
* GraphQL
* Spring Security + JWT
* Docker
* Docker Compose
* Environment variables for configuration

Alternative technologies should not be introduced without a documented architectural decision.


