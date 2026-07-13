## 1. Dependencies & config

- [x] 1.1 Add `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server` to `backend/pom.xml`.
- [x] 1.2 Add config to `application.properties`: `progolf.users.dir` (default `./users`), `progolf.auth.jwt.secret` (dev default), access/refresh TTLs. Gitignore `users/`.
- [x] 1.3 Point the test `application.properties` users dir at `target/test-users` and set a test secret.

## 2. Accounts & storage

- [x] 2.1 `com.progolf.app.auth.UserAccount` (id, username, passwordHash, createdAt).
- [x] 2.2 `UserStore` port: `save`, `findByUsername`, `findById`, `existsByUsername`.
- [x] 2.3 `FilesystemUserStore` adapter (one JSON per user id, atomic writes, username index) mirroring `FilesystemSaveGameStore`.

## 3. Credentials & tokens

- [x] 3.1 `PasswordEncoder` (BCrypt) bean in `SecurityConfig`.
- [x] 3.2 `JwtService`: `NimbusJwtEncoder`/`NimbusJwtDecoder` over the symmetric secret; issue access (`type=access`, short TTL, subject=user id) and refresh (`type=refresh`, long TTL) tokens; decode/validate.
- [x] 3.3 `AuthService`: register (reject duplicate, BCrypt-hash), login (verify, issue pair), refresh (validate `type=refresh`, mint new access).

## 4. REST endpoints

- [x] 4.1 `AuthController` (`/auth`): `POST /register`, `POST /login`, `POST /refresh` with request/response DTOs (never echo the password).
- [x] 4.2 Map failures: duplicate username → 409; bad credentials / invalid refresh → 401.

## 5. Security filter chain

- [x] 5.1 `SecurityConfig` `SecurityFilterChain`: stateless, CSRF off, `permitAll` `/auth/**` + `/actuator/health` + GraphiQL assets, `authenticated()` for `/graphql` and the rest.
- [x] 5.2 `oauth2ResourceServer().jwt()` with the decoder; require `type=access` on the resource-server path (reject refresh tokens at `/graphql`).

## 6. Tests

- [x] 6.1 `FilesystemUserStore` unit test: save/find/exists round-trip + persistence across a fresh store instance.
- [x] 6.2 `JwtService` test: access/refresh issue + decode, `type` claim, expiry rejection.
- [x] 6.3 Auth flow (`@SpringBootTest` + `MockMvc`): register → login → call `/graphql` with the access token succeeds; no/invalid token → 401; refresh → new access token; duplicate register → 409; bad login → 401; refresh token at `/graphql` → 401.
- [x] 6.4 Confirm existing GraphQL resolver tests (via `GraphQlTester`) still pass and `ArchitecturePurityTest` is green.

## 7. Verify

- [x] 7.1 `mvn test` from `backend/` — full suite green.
- [x] 7.2 Manual smoke: register + login over HTTP, call `/graphql` with and without the token, refresh the access token.
