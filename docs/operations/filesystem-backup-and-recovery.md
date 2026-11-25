# Filesystem backup and recovery

This procedure covers the current single-instance filesystem stores. It is an operational procedure, not a new save mechanism or a deployment topology. Protect the backup as account and career data. The [save-persistence specification](../../openspec/specs/save-persistence/spec.md) remains authoritative for application behavior.

## Data and configuration to retain

- Account files are under the configured PROGOLF_USERS_DIR (default ./users), one user-ID JSON file per account. [FilesystemUserStore](../../backend/src/main/java/com/progolf/app/auth/FilesystemUserStore.java) stores the stable account ID and BCrypt password hash.
- Named saves and the one recovery autosave per owner are under PROGOLF_SAVES_DIR/<user-id>/<save-id>.json (default ./saves). Preserve the user-ID directory and filenames exactly. The save includes the seed, world configuration and snapshot, metadata, review acknowledgements, and career lineage. [FilesystemSaveGameStore](../../backend/src/main/java/com/progolf/app/persistence/FilesystemSaveGameStore.java) and [SaveGame](../../backend/src/main/java/com/progolf/app/persistence/SaveGame.java) define this layout and content.
- The supplied [Compose configuration](../../docker-compose.yml) mounts its backend-data volume at /data and sets the two directories to /data/users and /data/saves. The actual volume name depends on the Compose project; inspect the running backend mount rather than inferring it from the YAML.
- Retain the application image identity or version with the backup. The current writer uses save envelope format 4 and reads formats 2–4; format 1 is rejected. Restoring with the same application images is the simplest compatibility check. A later application version needs its own compatibility verification, and an older image must not be assumed to load a newer save. Saved course-generator provenance also participates in deterministic restoration.
- A JWT secret is required to start the backend. A distinct secret permits password login and issues new tokens, but existing browser tokens then cease to work. Keep secrets outside the archive and never print the backend environment wholesale.

## Consistency and recovery boundary

The stores write temporary files and replace individual JSON files; career saves also use slot locks, revision checks, and a payload checksum. These safeguards do not create one transaction across account and save files. A normal copy while the backend is writing can capture different moments. For a simple, consistent file-level backup, finish active operations, use Save & Return for the relevant named career, stop the backend, and archive both directory trees while it is stopped. Always restart the source backend after the archive attempt, including on failure. A storage-level point-in-time snapshot could substitute for this pause if a future deployment supplies and verifies it.

Copy the users and saves trees as one archive. Temporary .tmp files and save .lock files are not recovery inputs; a stopped-backend archive may contain them without making them authoritative. An archive hash detects subsequent archive changes. On load, the save checksum rejects a damaged payload, but neither hash proves that every expected account or save was captured; compare the archive inventory and perform an isolated restore.

Only successfully persisted state can be recovered. A pending playable event cannot be snapshotted, and an unsaved live session, in-progress shots, transient practice state, and session ID are not in the backup. The owner's autosave may be older than a named save or belong to another career. Check each slot against its own expected state. [WorldService](../../backend/src/main/java/com/progolf/app/world/WorldService.java) and [World.snapshot](../../backend/src/main/java/com/progolf/sim/world/World.java) establish these boundaries.

## Procedure on Windows with Docker

Replace the uppercase identifiers below with inspected values. These commands are examples for Command Prompt. Do not run them against an unverified project or volume. Use a newly created, empty backup directory outside the repository and restrict its access. Docker can create a new empty volume from a mistyped name, so inspect the exact source or destination volume before every helper mount.

### 1. Preflight

Record the source Compose project, backend container, /data mount, volume, and backend/frontend image IDs. Inspect only the relevant storage-path variables; inspecting all container environment variables can expose the JWT secret.

    docker compose ls
    docker inspect SOURCE_BACKEND_CONTAINER --format "{{json .Mounts}}"
    docker exec SOURCE_BACKEND_CONTAINER printenv PROGOLF_USERS_DIR PROGOLF_SAVES_DIR
    docker volume inspect SOURCE_VOLUME --format "{{.Name}}"
    docker image inspect BACKEND_IMAGE FRONTEND_IMAGE --format "{{.RepoTags}}|{{.Id}}"

Confirm that /data/users and /data/saves are inside SOURCE_VOLUME and that the backup target does not already exist. Record private, representative pre-backup career values for later comparison. Use synthetic data for an initial procedure rehearsal; use real career data only when specifically authorized.

### 2. Create the backup

Finish any event or season operation and save the named career at a clean boundary. Stop only the verified backend, confirm it is exited, then archive the two trees through a read-only source-volume mount. The locally available helper image must include tar. The backup-directory bind mount is writable only to receive the archive.

    docker stop --timeout 120 SOURCE_BACKEND_CONTAINER
    docker inspect SOURCE_BACKEND_CONTAINER --format "{{.State.Status}}"
    docker ps --filter volume=SOURCE_VOLUME --format "{{.Names}}"
    docker run --rm --pull=never --mount "type=volume,source=SOURCE_VOLUME,target=/data,readonly" --mount "type=bind,source=BACKUP_DIR,target=/backup" --entrypoint sh BACKEND_IMAGE -c "tar -cf /backup/data.tar -C /data users saves"
    docker start SOURCE_BACKEND_CONTAINER

The inspected backend status must be exited and the running-container volume query empty before archiving. Run the final start command even if tar fails; investigate any failed stop, archive, or restart before using the backup.

### 3. Verify the backup

Check that the archive is nonempty, contains the expected account and owner-scoped named/autosave JSON paths, and can be listed. Keep the listing private because paths contain account IDs. Record the digest and image IDs alongside the backup.

    tar -tf "BACKUP_DIR\data.tar"
    certutil -hashfile "BACKUP_DIR\data.tar" SHA256

### 4. Restore in isolation

Use a separately reviewed Compose configuration with the same application images, a different project name, a new explicit destination volume, a different loopback frontend port, and a separate JWT secret. Keep its environment file outside the repository and restrict access. Create the project without starting the backend, verify that its mounted destination is new, empty, and different from SOURCE_VOLUME, then extract. Mount the archive read-only. Start without rebuilding.

    docker compose --env-file RESTORE_ENV -p RESTORE_PROJECT -f RESTORE_COMPOSE_FILE create
    docker volume inspect DESTINATION_VOLUME --format "{{.Name}}"
    docker inspect RESTORE_BACKEND_CONTAINER --format "{{json .Mounts}}"
    docker run --rm --pull=never --mount "type=volume,source=DESTINATION_VOLUME,target=/data" --mount "type=bind,source=BACKUP_DIR,target=/backup,readonly" --entrypoint sh BACKEND_IMAGE -c "tar -xf /backup/data.tar -C /data"
    docker run --rm --pull=never --mount "type=volume,source=DESTINATION_VOLUME,target=/data,readonly" --mount "type=bind,source=BACKUP_DIR,target=/backup,readonly" --entrypoint sh BACKEND_IMAGE -c "tar -df /backup/data.tar -C /data"
    docker compose --env-file RESTORE_ENV -p RESTORE_PROJECT -f RESTORE_COMPOSE_FILE up -d --no-build --no-recreate

Confirm backend health and frontend HTTP availability before browser acceptance. In the browser, authenticate with the restored account, list and open the named career and autosave, compare representative values, progress the isolated career, save it, and reopen it. Never direct a restored backend at the source writable volume.

### 5. Cleanup of disposable rehearsals

After owner acceptance, verify project labels, exact container names, network attachments, and volume labels. Stop and remove only the named rehearsal containers, networks, and dedicated synthetic volumes. Do not use broad prune commands or remove the backup. An actual source environment is not disposable.

## Accepted isolated rehearsal — 25 November 2026

The owner reported a clean 9f30130 baseline before the rehearsal. A synthetic source project, progolf-recovery-source, and a separate restore project, progolf-recovery-restore, used the same application images:

- Backend: sha256:f4d5a572b6498b7cc2941925f5d0c9be7e64aaed10145a1109c114db303bc0
- Frontend: sha256:92a870b18c635359705d7d801151d2f1836ce7ccf5d1e945493a6e72cd7e510b

The source backend was stopped, and a read-only mount of progolf-recovery-source_backend-data produced an archive containing one account, one named save, and one autosave. The 8,243,200-byte archive had SHA-256 44308DBE7D854396F32A7DF9EB80FB8707DB09853ACF1DB0C1703FF28328019A. The source backend was restarted. The destination volume, progolf-recovery-restore_backend-data, was checked empty and distinct before extraction. Extracted files matched the archive with tar comparison. The restore backend was healthy and its frontend returned HTTP 200 on localhost port 3102; the source remained healthy on port 3101. The restoration used a separate JWT secret.

The owner and ChatGPT then accepted browser authentication, restored career loading, progression from Season 1 Week 2 to Week 3, Save & Return, and reopening the progressed career. Both named and recovery saves were present. This browser result is owner-reported acceptance; the archive, file comparison, health, and availability checks were observed during the operational rehearsal.

After acceptance, the two exact rehearsal projects, their four containers, two networks, and two dedicated synthetic volumes were removed. The archive remains at C:\workspace\progolf-recovery-backups\rehearsal-20261125-141628Z\data.tar outside the repository with access restricted to the owner, SYSTEM, and Administrators. Its digest was unchanged after cleanup. No real user data or original development volume was used.

This synthetic drill proves the procedure and a representative saved career, not recovery of every historical or long-career save. It does not establish backup scheduling, retention, recovery-time targets, or recovery of unsaved in-memory play.
