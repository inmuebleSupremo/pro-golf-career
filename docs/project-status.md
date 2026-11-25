# Project status and observation backlog

**Status updated: 25 November 2026.** This is the current handover and observation backlog, not a replacement for the [product requirements](backend/explore.md), [player decision surface](backend/player-experience.md), or applicable [OpenSpec specifications](../openspec/specs/). Update this page after each accepted workstream. Git and code remain the evidence for implementation; observations below are not all approved changes.

## Current phase and next task

**Gradual production maturation.** The end-to-end career is playable. Improve the existing application through targeted reliability, security, performance, user experience, maintainability, deployment, and operational-readiness work. Preserve gameplay, save compatibility, and deterministic simulation. Previously discussed new gameplay systems and broad redesigns are deferred, not active roadmap commitments.

**Backend career-session lifecycle remains the next identified backend reliability priority, with release implementation deferred.** Source inspection and a focused synthetic measurement confirmed retention of loaded worlds, but a safe Save & Return release requires broader per-session operation coordination than the approved narrow slice. No release change was implemented; see the deferred finding below. The owner has closed the career-navigation investigation without a redesign and accepts the current Season Review for now.

## Integration and verification snapshot

At the 14 November handover, local `develop` and `origin/develop` both pointed to `4e05a66`. The owner subsequently verified a clean, synchronized `9f30130` baseline on `develop`; this documentation-only closeout did not perform Git operations to recheck it. The merge commits below are historical integration evidence on `develop`'s first-parent history. “Merged” means integrated in Git; browser acceptance is identified separately where the owner reported it. The cited implementation commits include focused tests, which were not rerun for this documentation-only task.

| Completed work | Integration evidence on `develop` | Verification boundary |
| --- | --- | --- |
| Physical putting as the default and only mode; fringe putting, Around the Green view, slope-aware chip/pitch rollout | `8b9d1f5`, `d2674a1`, `427d8ba`, `0895823` | Merged implementation; consult the commits and focused coverage for each behavior. |
| Physical wedge training and putting/shot-control parity | `410a42b` (`1ac2b9a`) | Merged with backend and frontend test changes. |
| Mobile career navigation | `96122d5` (`d998b16`) | Merged with sidebar tests. |
| Session-to-save provenance and conflict-safe career saving | `e54453c` (`47265b3`), `0ac83e2` (`999fcbd`) | Merged with provenance, conflict, and safe-save tests. |
| My Careers, Sign Out, and safe exit confirmations | `8463dcf` (`98b5a10`) | Merged with account-menu and logout tests. |
| Account/saved-career usability and mobile account-button visibility | `7608321` (`5d16618`) | Merged with header, menu, and navigation tests. |
| Season simulation duplicate-operation protection and timeout reconciliation | `d1afcc4` (`9a32a48`) | Merged with backend operation, proxy, and frontend reconciliation tests. The transport timeout remains an open limitation below. |
| Season Review loading/transition correction | `b3cf476` (`8f765b7`) | Merged with transition and season-control tests. |
| Large-career save loading | `30501fb` (`4ec44d2`) | Merged with save-store tests. The Jackson payload-string limit is now 256 MiB; this is a bounded fix, not a long-term scaling solution. |
| Advance Week guard during same-session Sim Season | `dbbda88` | Merged with deterministic concurrency tests and owner-run same-session two-tab browser acceptance. |
| Cross-session autosave protection and concurrent Finish Event guard | `4e05a66` | Merged with lineage, autosave-generation, conflict, outcome and Finish Event concurrency tests. Owner-run isolated Docker/browser acceptance covered normal progression, Sim Season and Season Review, named save/reload, independent same-career sessions, stale-autosave rejection, preservation and recovery of the newer checkpoint, and reopening the recovered career. |

Successful recovery of a **Season 5 career** is a user-reported Docker/browser acceptance result associated with the large-save fix. The personal save is not in Git, so this specific recovery cannot be independently verified from repository contents. No personal saves, account identifiers, secrets, or backups belong in this handover.

The merged autosave safeguard keeps one slot per owner and prevents a stale session of the same career lineage from replacing a newer checkpoint. A different career may take the slot, except when it contains ambiguous legacy recovery data without lineage provenance. That legacy autosave remains loadable; the owner must review and explicitly delete it before a different career can use the slot. Deletion also resets the slot's stored lineage-generation history. Named-save revision checks remain separate.

The owner reports that the concurrent-registration correction is merged. A deterministic regression test reproduced the original check-then-save race: two simultaneous requests for one unused username both succeeded and created two account files. Registration now uses an atomic check-and-create operation in the filesystem user store, coordinated per username within one backend instance. The service's earlier availability check remains a fast rejection; existing username matching, account JSON, authentication and HTTP contracts are unchanged. The focused concurrency test then verified one success, one duplicate rejection and one persisted account; a second test verified concurrent registration of different usernames. Thirteen focused registration, filesystem-store, JWT, authentication-flow and HTTP error-status tests passed during implementation, and the OpenSpec change was validated and [archived](../openspec/changes/archive/2026-11-25-fix-concurrent-registration/proposal.md). These tests were not rerun for this documentation closeout. The in-memory coordination does not guarantee uniqueness across multiple backend processes sharing a user directory.

### Deferred backend session retention

[WorldService](../backend/src/main/java/com/progolf/app/world/WorldService.java) retains each created or loaded live session and its simulation world for the backend process lifetime; Save & Return persists and navigates away but does not release the session. In a short-career synthetic diagnostic, loading the same named save six additional times increased retained `WorldSession` and `World` counts from one to seven, with 84 `Course` objects at the end. Post-GC used heap rose from 30,689 KiB to 85,621 KiB (54,932 KiB); this is not a per-session production memory estimate, and JVM committed or process working-set memory is not live heap. The diagnostic used isolated synthetic account/save data and did not cover long careers or active events.

A narrow save-then-remove implementation was stopped before editing: although `saveCareer` holds the world monitor, many other world mutations do not. A request can acquire a world reference before closure and mutate it after the saved snapshot; autosave can also write a session entry back, and season-operation status must remain reconcilable. Safe release requires a per-session operation boundary that coordinates all relevant service calls, in-flight saves and season operations, and auxiliary state cleanup. That broader lifecycle change is deferred for owner review; no automatic expiration, capacity limit or separate observability workstream is approved.

The owner and ChatGPT completed and accepted an isolated filesystem backup-and-restoration rehearsal with synthetic data. Both account authentication and named-career loading succeeded after restoration. The owner advanced the restored career from Season 1 Week 2 to Week 3, used Save & Return, and reopened the progressed career; named and recovery saves were present. The archive was verified before restoration, and the restored files matched it. See the [filesystem backup and recovery procedure](operations/filesystem-backup-and-recovery.md) for the evidence boundary, consistency requirements, and limitations. This acceptance does not establish an automated backup schedule or certify recovery of every historical save.

The career-navigation source investigation and owner-led browser review are closed without implementation. The owner identified no need for a navigation redesign. Season Review is acceptable for now. These decisions supersede the earlier navigation and Season Review recommendations below without erasing their historical observations.

Some OpenSpec change directories remain unarchived with checked tasks, including presentation and career-inbox work. Their presence is not evidence that they are the current priority or that every manual check is finished; read their own `tasks.md` before resuming them. The [player-experience roadmap](backend/player-experience.md) describes longer-term possibilities, not the current task order.

## Working principles

- Preserve existing gameplay functionality, save compatibility, saved careers, and progression. Favor narrow fixes with an observed failure or clear decision benefit. Defer new gameplay systems and foundational redesigns.
- Keep human and AI golfers consistent where rules require parity; retain backend authority, deterministic simulation, and the pure `sim.*` / application boundary.
- Avoid broad redesign from vague instructions. Review an observation, agree on scope, then implement a focused change. Avoid new major features until the existing experience has been reviewed and improved.
- Codex investigates and implements focused repository work. The user performs Docker/browser acceptance and manual SourceTree version control. Leave Codex changes uncommitted unless separately instructed.

## Observation-led backlog

**Priority:** P1 = gameplay/functionality; P2 = usability/consistency; P3 = presentation/refinement. Visual quality still matters for a game. **State:** confirmed limitation/defect = evidenced current behavior to investigate or correct; candidate = observation requiring review and scope approval; design decision = unresolved choice; accepted deferral = intentionally later work. Priority does not authorize implementation.

| Priority / state | Area | Observation or decision to review |
| --- | --- | --- |
| P2 · closed without implementation | Career navigation | The earlier investigation considered sidebar destination hierarchy, grouping, and hub cards; mobile navigation and account access were already implemented. The owner reviewed the running application and found no need for a navigation redesign. |
| P2 · accepted deferral | Career time | The earlier observation considered moving career-time simulation controls into Schedule/Calendar and a week/month/season flow with decision checkpoints while preserving in-round controls. Time-control redesign is deferred and is not an active roadmap commitment. |
| P2 · candidate | Week 1 event navigation | Owner-observed potentially inconsistent navigation into a Week 1 event. Capture the exact career state, route, and action sequence before treating this as a confirmed defect or scoping a correction. |
| P2 · candidate | Post-event career summary | Owner observed bankroll and last-result information that appeared stale immediately after event completion and corrected after reopening the career. Verify the affected surfaces and refresh timing before scoping a correction. |
| P2 · candidate | Schedule | Review labels, filters, and information that may be unnecessary to players, including “Corrected flag placement.” |
| P2 · candidate | Golfer creation | Improve the long scrolling setup; consider responsive steps while retaining every current choice and gameplay effect. |
| P1 · candidate | Play information | Make wind speed and direction easier to see while deciding a shot. |
| P3 · candidate | Course presentation | Review Full Hole lighting, fade, and course presentation; keep the stronger Putting and Around the Green treatments where appropriate. Review Free Practice background and lighting consistency. |
| P1 · accepted deferral | Practice parity | The earlier candidate considered greenside wedge parity in Free Practice after reviewing current play and training behavior. This would expand gameplay and is deferred; it is not an approved implementation slice. |
| P2 · candidate | Saved careers | Make careers easier to identify by golfer name where useful. Review exit availability in immersive Play and Practice. |
| P2 · accepted deferral | Career count | The earlier design question asked whether a maximum number of named careers is desirable. No limit has been approved; this is deferred. |
| P2 · accepted for now | Season Review | The earlier observation concerned duplicated Continue actions and the bottom panel covering review content. The owner reviewed Season Review and accepts it for now; no correction is currently scoped. |
| P1 · confirmed limitation | Season simulation | Advancing a season can exceed the GraphQL proxy's response-header timeout. Duplicate-operation protection and safe reconciliation are merged; performance or transport optimization is still open. |
| P2 · known limitation | Finish Event response | The Finish Event UI handles a received `COMPLETED_UNSAVED` error, but an entirely lost completion response has no event-operation query for reconciliation. The outcome must be checked from current career state before any retry; this is a recovery/clarity gap, not evidence that an event was recorded twice. |
| P2 · known limitation | Legacy autosave | An ambiguous legacy recovery autosave remains readable and protected; replacing it with another career requires explicit owner deletion after reviewing the recovery data. Deletion resets the slot's stored lineage-generation history. |
| P2 · confirmed limitation | Save growth | Save loading uses a bounded 256 MiB JSON payload-string limit. Long-term snapshot growth remains a scaling concern. Save listing currently reads full save envelopes and parses snapshot payloads to extract metadata; metadata access could be made cheaper. |
| P3 · accepted deferral | Greens | Broader procedural green-shape and contour variety remains later work, as recorded in [player experience](backend/player-experience.md#physical-putting-v1-first-playable-milestone). |

Promote a candidate to an implementation task only after an observed example, intended player outcome, scope, and acceptance checks have been agreed. Recheck Git and the relevant OpenSpec change before marking any item complete.

## Starting a new conversation

1. Ask the new ChatGPT or Codex conversation to read `AGENTS.md`, this page, `README.md`, and the relevant directory `AGENTS.md`. For product questions, read `docs/backend/explore.md` and `docs/backend/player-experience.md`; for UI work, read the relevant `docs/frontend/` guides and OpenSpec specifications/active change.
2. Run `git status --short --branch` and `git log --oneline --decorate --first-parent -15` to verify the actual branch, working tree, and integration state. Do not infer a merge from a prior conversation.
3. Use **this page's Observation-led backlog** for active observations and the **Current phase and next task** section for the current recommendation. Treat merge commits and code/tests as repository evidence, and the cited browser checks and Season 5 recovery as owner-reported acceptance. Cross-session autosave protection is merged at `4e05a66`; retain the legacy-autosave deletion policy and the lost-response Finish Event limitation when discussing recovery.
4. **Backend career-session lifecycle and resource management** remains the next identified backend reliability priority, but a safe release implementation is deferred pending a broader, explicitly scoped operation boundary. Concurrent registration integrity is merged and its OpenSpec change is archived. The navigation investigation is closed without implementation, Season Review is accepted for now, and new gameplay features and time-control redesign remain deferred. Leave changes uncommitted for the user's SourceTree review unless explicitly instructed otherwise.
