## Context

`WorldSnapshot` is a pure, self-contained record graph (all leaves are records/enums/primitives/collections; the one golfer-referencing type is captured by id). `World.restore(masterSeed, config, snapshot)` rebuilds a world. The app already depends on `spring-boot-starter-web`, so Jackson `databind` + `jsr310` (LocalDate) + `jdk8` (`Optional<Injury>` in `PhysicalState`) are on the classpath. `WorldService` holds in-memory `WorldSession(id, seed, World)` and is the designated seam for durable saves. The engine's `ArchitecturePurityTest` forbids framework imports in `com/progolf/sim`, so all Jackson/filesystem code must live in `com.progolf.app`.

## Goals / Non-Goals

**Goals:**
- Write a snapshot to a JSON file and read it back into an identical world.
- Detect a corrupt or version-incompatible file on load (fail loudly, never a broken world).
- Support multiple independent saves + list/delete; autosave at checkpoints.
- Keep `sim.*` serialization-free.

**Non-Goals:**
- No format migration across versions (a mismatch is rejected; migration is later).
- No database adapter (Postgres is a later adapter behind the same port).
- No concurrent-write coordination beyond atomic file replace (single-user local saves).

## Decisions

**Decision: a `SaveGameStore` port with a `FilesystemSaveGameStore` adapter.** The port exposes `save(id, SaveGame)`, `load(id) -> SaveGame`, `list() -> List<SaveSummary>`, `delete(id)`. The filesystem adapter stores one `<id>.json` per save under a configurable directory (`progolf.saves.dir`, default `./saves`), created on demand. Postgres later implements the same port.

**Decision: a `SaveGame` bundles the restore inputs, not just the snapshot.** `SaveGame(long seed, WorldConfig config, WorldSnapshot snapshot, SaveMetadata metadata)`. `metadata` = save id, `savedAt`, season, week, player golfer id — for listing without deserializing the whole snapshot (though V1 may read the full file). A pure `World.config()` getter is added so the app can capture the config to save.

**Decision: an envelope carries the version and checksum over the exact payload bytes.** `SaveEnvelope(int formatVersion, String checksum, String payload)`, where `payload` is the JSON of the `SaveGame` and `checksum` is its SHA-256. Save: serialize the game to a JSON string, hash it, wrap, write the envelope JSON. Load: read the envelope, reject an unsupported `formatVersion` (`SaveVersionMismatchException`), recompute the hash of `payload` and reject a mismatch (`SaveCorruptedException`), then deserialize `payload` to `SaveGame`. Hashing the stored string (not a re-serialization) means corruption of any byte is caught without needing canonical output. Writes are atomic (temp file + move) so a crash mid-write never leaves a half file.

**Decision: reuse Spring's configured `ObjectMapper`.** The adapter takes an `ObjectMapper` by constructor; Spring injects the one already registered with the JSR-310 and JDK8 modules. The pure records deserialize via their canonical constructors (Jackson record support) — no annotations in `sim.*`. Map types round-trip by value (`Map.equals` is type-independent), so an `EnumMap` captured in a live snapshot equals a `LinkedHashMap` read from JSON.

**Decision: autosave is an ordinary save under a reserved id.** `WorldService.advanceSeason` and `completeEvent` call `save(sessionId, AUTOSAVE_ID)` after advancing. It uses the same store/format/guarantees and is loadable like any save. Manual `save(sessionId, saveId)` uses any caller-chosen id.

**Decision: typed failures.** `SaveNotFoundException`, `SaveCorruptedException`, `SaveVersionMismatchException` (extending a common `SaveException`), mapped later to HTTP status by the controller layer.

## Risks / Trade-offs

- **A snapshot type Jackson can't round-trip** → the end-to-end disk determinism test (save → load → advance == advance) plus a direct snapshot serialize/deserialize-equals test catch any un-serializable field early; all snapshot leaves are records/enums/collections, and the needed modules are present.
- **Checksum over an escaped-JSON payload string** makes the file a JSON envelope wrapping a JSON string — slightly less human-friendly, but bulletproof for integrity and simple to verify. Acceptable for a save file.
- **Saves directory location** → configurable; defaults under the working directory. Tests use a JUnit `@TempDir` so nothing leaks.
- **Boundary regression** → the engine-purity test already guards `com/progolf/sim`; adding a `save-persistence` scenario asserting no serialization imports in the engine keeps it explicit.
