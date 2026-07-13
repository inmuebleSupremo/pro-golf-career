## Context

The app layer wraps a pure engine; all Spring/web/persistence lives in `com.progolf.app` and `sim.*` stays framework-free (guarded by `ArchitecturePurityTest`, scoped to `com/progolf/sim`). Persistence already follows a port/adapter pattern (`SaveGameStore` + `FilesystemSaveGameStore`) with Postgres deferred. This change adds authentication in the same spirit: a `UserStore` port with a filesystem adapter, JWT issuance, and a Spring Security filter chain that gates the GraphQL API. The GraphQL surface itself is unchanged — it is now reached only by authenticated requests.

## Goals / Non-Goals

**Goals:**
- Accounts with securely hashed credentials, behind a store port (filesystem adapter now, Postgres later).
- JWT access + refresh tokens, environment-configured, with register/login/refresh REST endpoints.
- A stateless security filter chain gating `/graphql`; `/auth/**` and health stay public.
- Keep `sim.*` pure and `WorldService` API-agnostic (no security imports leak into the engine seam).

**Non-Goals:**
- Ownership/authorization of saves and sessions (the next slice, `add-user-ownership`).
- Email verification, password reset, OAuth, Keycloak (V1 exclusions).
- Postgres/JPA (deferred to the Docker step, behind the same port).
- Roles/authorities beyond "authenticated user" (no admin tiering in V1).

## Decisions

**D1 — `UserStore` port + `FilesystemUserStore` adapter, mirroring `SaveGameStore`.** One JSON file per user under `progolf.users.dir` (default `./users`, gitignored; tests use a temp/`target` dir). Atomic writes (temp + move). Lookups by username and by id. *Alternative:* introduce Postgres/JPA now — rejected per the project's "defer infra" pattern and the scoping decision; the Postgres adapter slots behind this port at the Docker step. *Alternative:* in-memory store — rejected: accounts must survive restarts.

**D2 — Passwords hashed with BCrypt via Spring Security's `PasswordEncoder`.** Registration stores only the hash; login verifies with `matches`. A `BCryptPasswordEncoder` bean is the single hashing authority. Plain passwords never persist or log.

**D3 — JWT with Spring Security's Nimbus encoder/decoder over a symmetric secret.** `spring-boot-starter-oauth2-resource-server` provides the `oauth2ResourceServer().jwt()` validation DSL and (via `spring-security-oauth2-jose`) `NimbusJwtEncoder`/`NimbusJwtDecoder`. A single HMAC secret (`progolf.auth.jwt.secret`, env-driven) signs and verifies both tokens — no third-party JWT dependency. Access tokens carry the subject (user id), a short TTL, and a `type=access` claim; refresh tokens carry `type=refresh` and a longer TTL. *Alternative:* `jjwt` — rejected to stay within the Spring Security ecosystem already pulled in. *Alternative:* asymmetric RSA keys — unnecessary for a single-service V1; symmetric is simpler and env-configurable, and can be swapped later.

**D4 — Access + refresh split; `/auth/refresh` re-issues only the access token.** Login returns both tokens. The refresh endpoint validates a token, requires its `type=refresh` claim, and mints a fresh access token. V1 refresh is **stateless** (no server-side refresh store or rotation/blacklist) — a refresh token is valid until it expires. *Trade-off:* no immediate revocation; acceptable for V1 and documented. Rotation/revocation can be added later (it would need a store, which the `UserStore`/a token store could back). The `type` claim prevents using an access token to refresh or vice-versa.

**D5 — Stateless `SecurityFilterChain`; permit auth + health + GraphiQL, authenticate the rest.** `SessionCreationPolicy.STATELESS`, CSRF disabled (token-based, no cookies), `permitAll` on `/auth/**`, `/actuator/health`, and the GraphiQL page/assets; `authenticated()` on `/graphql` and everything else; `oauth2ResourceServer().jwt()` validates the Bearer access token and sets the principal (subject = user id). A missing/invalid token yields 401. *Note:* the resource-server decoder accepts any validly-signed unexpired JWT; to reject a *refresh* token at `/graphql`, a lightweight validator requires `type=access` on the resource-server path.

**D6 — Auth stays out of the engine seam.** All security/JWT/user code is in `com.progolf.app.auth`; `WorldService` and `sim.*` gain no security imports. The authenticated principal will be threaded into `WorldService` in the ownership slice, as method parameters — not via a security dependency inside the engine wrapper. This keeps `ArchitecturePurityTest` green and `WorldService` reusable.

## Risks / Trade-offs

- **Securing `/graphql` could break existing tests** → the GraphQL resolver tests use `@AutoConfigureGraphQlTester` over the `ExecutionGraphQlService`, which does not pass through the servlet security filter, so they remain green. Only HTTP-level tests must authenticate — verified by running the full suite. → Mitigation: no method-level security in this slice.
- **Stateless refresh tokens can't be revoked before expiry** (D4) → accepted V1 trade-off; keep refresh TTL moderate and document that rotation/revocation is a later enhancement.
- **Symmetric secret in config** → must come from the environment in real deployments (never committed); a dev default is provided for local runs, and secrets are gitignored. Flagged for the Docker/`.env` step.
- **GraphiQL now needs a token** → GraphiQL supports an Authorization header; developers paste a Bearer access token. The page/assets are permitted; only the `/graphql` POST requires the token. Documented.
- **Filesystem user store isn't concurrency-hardened for scale** → fine for V1/single-node dev, same posture as saves; Postgres adapter addresses scale later.
