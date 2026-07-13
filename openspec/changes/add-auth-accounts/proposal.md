## Why

The GraphQL API exposes the full game over `WorldService`, but every operation is open — anyone can create worlds, drive sessions, and read or delete any save. The tech stack mandates authentication (Spring Security, JWT access + refresh tokens, BCrypt) so the app can have user accounts that will own their saves and sessions. This change adds the authentication foundation: accounts, credential handling, token issuance, and securing the API. It is the first of two Auth slices — a follow-up (`add-user-ownership`) then scopes saves and sessions to the authenticated user.

## What Changes

- **User accounts**: a `UserAccount` (id, username, BCrypt password hash, created-at) and a `UserStore` port with a `FilesystemUserStore` adapter (one JSON file per user under a configurable directory), mirroring the existing `SaveGameStore` pattern. The PostgreSQL adapter is deferred to the Docker/infra step.
- **REST auth endpoints** (the tech stack keeps REST for auth): `POST /auth/register` (create an account, hashing the password with BCrypt), `POST /auth/login` (verify credentials, issue a token pair), and `POST /auth/refresh` (exchange a valid refresh token for a new access token).
- **JWT tokens**: a short-lived **access token** and a longer-lived **refresh token**, signed with a symmetric secret from configuration; access-token TTL, refresh-token TTL, and the secret are environment-driven.
- **Spring Security**: a stateless `SecurityFilterChain` that permits `/auth/**`, the health endpoint, and the GraphiQL assets, and requires a valid Bearer access token for `/graphql` (and everything else). Invalid/missing tokens yield 401.
- **Dependencies**: `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server` (Spring Security's Nimbus JWT encode/decode) — no third-party JWT library.
- **Out of scope (V1 per tech stack)**: email verification, password reset, OAuth providers, Keycloak. **Deferred to `add-user-ownership`**: scoping saves/sessions to the owner — this slice authenticates but does not yet restrict which saves/sessions a user can address.

## Capabilities

### New Capabilities
- `authentication`: user accounts, credential registration and verification (BCrypt), JWT access/refresh issuance and refresh, and securing the API so only authenticated requests reach the game.

### Modified Capabilities
<!-- none — the GraphQL surface is unchanged; it is now gated by the security filter, not altered. -->

## Impact

- **New app-layer code** (`com.progolf.app.auth`): `UserAccount`, `UserStore` + `FilesystemUserStore`, a registration/credential service (BCrypt), a JWT service (encode/decode, access + refresh), REST auth controllers + request/response DTOs, and `SecurityConfig` (the filter chain + `PasswordEncoder` + `JwtDecoder`/`JwtEncoder` beans).
- **New dependencies**: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`.
- **New config**: `progolf.users.dir` (accounts directory), `progolf.auth.jwt.secret`, access/refresh TTLs — environment-driven with dev defaults.
- **Unchanged**: the simulation core (`sim.*`), `WorldService`'s method surface, the GraphQL schema/resolvers, and the `ArchitecturePurityTest` boundary. Existing GraphQL resolver tests run through the `ExecutionGraphQlService` (not the servlet filter), so they are unaffected by the new security chain.
- **Tests**: register → login → authenticated `/graphql` succeeds; missing/invalid token → 401; refresh issues a new access token; duplicate registration and bad credentials are rejected.
