# TECH_STACK.md

# Professional Golf World Simulation

## Technology Stack

---

# Purpose

This document defines the authoritative technology stack for the project.

It describes **how** the application will be implemented.

Gameplay behaviour and product requirements remain defined exclusively within `EXPLORE.md`.

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

# Frontend

| Technology    | Version |
| ------------- | ------- |
| React         | 19      |
| Vite          | 7       |
| TypeScript    | Latest  |
| Apollo Client | Latest  |
| React Router  | Latest  |

Responsibilities:

* User Interface
* GraphQL client
* Routing
* State management
* Visualisation

---

# Database

| Technology | Version |
| ---------- | ------- |
| PostgreSQL | 17      |

Database responsibilities:

* Persistent world state
* User accounts
* Career saves
* Historical data

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

Frontend:

* Vitest
* React Testing Library

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
