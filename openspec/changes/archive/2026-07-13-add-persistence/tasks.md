## 1. Engine seam

- [x] 1.1 Add a pure `World.config()` accessor (framework-free getter) so a save can store its restore config.

## 2. Save model (com.progolf.app.persistence)

- [x] 2.1 `SaveMetadata` (record): saveId, savedAt (Instant), season, week, playerGolferId (nullable).
- [x] 2.2 `SaveGame` (record): seed, `WorldConfig` config, `WorldSnapshot` snapshot, `SaveMetadata` metadata.
- [x] 2.3 `SaveEnvelope` (record): formatVersion (int), checksum (String), payload (String). Listing reuses `SaveMetadata`.

## 3. Store port + filesystem adapter

- [x] 3.1 `SaveGameStore` interface: `save(id, SaveGame)`, `load(id) -> SaveGame`, `list() -> List<SaveMetadata>`, `delete(id)`, `exists(id)`.
- [x] 3.2 Typed exceptions: `SaveException` (base), `SaveNotFoundException`, `SaveCorruptedException`, `SaveVersionMismatchException`.
- [x] 3.3 `FilesystemSaveGameStore` (`@Component`): one `<id>.json` per save under `progolf.saves.dir` (default `./saves`, created on demand). Save serializes the `SaveGame`, SHA-256 hashes the payload, wraps in a `SaveEnvelope`, writes atomically (temp + move). Load reads the envelope, rejects an unsupported version and a checksum mismatch, then deserializes the payload. `list` scans the directory; `delete` removes the file. Uses a dedicated persistence `ObjectMapper` (isolated from HTTP; `SimSnapshotModule` + JSR-310/JDK8 + ignore-unknown for records' derived getters).

## 4. WorldService integration

- [x] 4.1 `save(sessionId, saveId)`: build a `SaveGame` from the session (seed, `world.config()`, `world.snapshot()`, metadata) and store it.
- [x] 4.2 `load(saveId)`: load the `SaveGame`, `World.restore(seed, config, snapshot)`, register a new `WorldSession`, return it.
- [x] 4.3 `listSaves()` / `deleteSave(saveId)`.
- [x] 4.4 Autosave to a reserved `AUTOSAVE` id after `advanceSeason` and after `completeEvent`.

## 5. Tests

- [x] 5.1 Store round-trip: save a `SaveGame` to a `@TempDir`, load it, and assert the payload deserializes to an equal `SaveGame` (snapshot equality).
- [x] 5.2 End-to-end determinism through disk: create → advance → `WorldService.save` → `load` → advance both loaded and original by N → identical (via `World.snapshot()` equality).
- [x] 5.3 Corruption: tamper with a saved file's payload → load throws `SaveCorruptedException`. Wrong format version → `SaveVersionMismatchException`. Missing id → `SaveNotFoundException`.
- [x] 5.4 Multiple isolated saves: save two worlds under two ids, list reports both, each loads to its own world; delete removes one.
- [x] 5.5 Autosave: advancing a season writes the reserved autosave slot; it loads back to the advanced world.
- [x] 5.6 A player-world save round-trips (designated golfer + control state survives disk).

## 6. Verify

- [x] 6.1 Full backend suite green, including the engine-purity test (no serialization imports leaked into `sim.*`).
