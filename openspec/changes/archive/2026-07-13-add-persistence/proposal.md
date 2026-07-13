## Why

The engine can capture a career as an immutable `WorldSnapshot`, but nothing writes it anywhere — close the process and the world is still gone. This change makes saves durable: it serializes a snapshot to a JSON file on disk, with a format version and a checksum so a corrupt or incompatible file is detected rather than silently loaded, supports multiple independent saves, and autosaves at natural checkpoints. That turns the in-memory session into a real, resumable game. All serialization and filesystem code lives in the Spring Boot app layer; the simulation stays pure and framework-free.

## What Changes

- Add a `SaveGameStore` port and a filesystem adapter in `com.progolf.app.persistence`. A save bundles the restore inputs and the snapshot — master seed, world config, `WorldSnapshot`, and display metadata (save id, timestamp, season/week, player golfer) — written as one JSON file per save in a configurable saves directory.
- Wrap each save in an envelope carrying a **format version** and a **SHA-256 checksum** of the payload. On load, a version mismatch or a checksum mismatch (a tampered/corrupt file) is rejected with a typed exception instead of a broken world.
- Serialize with Jackson (already on the classpath via Spring Boot: databind + JSR-310 for dates + JDK8 for `Optional`). The pure `sim.*` snapshot records carry **no Jackson annotations** — the app-layer mapper reflects over their record components.
- Extend `WorldService` with `save` / `load` / `listSaves` / `deleteSave`; loading rebuilds the world via `World.restore(seed, config, snapshot)` into a session. Add a tiny pure `World.config()` accessor so a save can store its restore config.
- **Autosave** to a reserved slot after each season advance and after a player event completes.
- Prove it end-to-end: create → advance → save to disk → load from disk → advancing the loaded world matches advancing the original.

## Capabilities

### New Capabilities
- `save-persistence`: a world can be saved to and loaded from durable JSON storage, with format versioning, checksum-based corruption detection, multiple isolated saves, and autosave; a loaded world continues identically to the saved one.

### Modified Capabilities
<!-- none -->

## Impact

- New package `com.progolf.app.persistence`: `SaveGame`, `SaveMetadata`, `SaveEnvelope`, `SaveSummary`, `SaveGameStore` (port), `FilesystemSaveGameStore` (adapter), and typed exceptions (`SaveNotFoundException`, `SaveCorruptedException`, `SaveVersionMismatchException`).
- `com.progolf.app.world.WorldService`: `save`/`load`/`listSaves`/`deleteSave` + autosave hooks in `advanceSeason` and `completeEvent`.
- `com.progolf.sim.world.World`: a pure `config()` getter (no framework dependency).
- No new Maven dependency (Jackson is transitive via `spring-boot-starter-web`). The `sim.*` purity boundary is unchanged — no serialization code or annotations enter the engine.
