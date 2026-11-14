# Project status and observation backlog

**Snapshot: 10 November 2026.** This is the current handover and observation backlog, not a replacement for the [product requirements](backend/explore.md), [player decision surface](backend/player-experience.md), or applicable [OpenSpec specifications](../openspec/specs/). Update this page after each accepted workstream. Git and code remain the evidence for implementation; observations below are not all approved changes.

## Current phase and next task

**Observation-led Gameplay and Interface Completion.** The end-to-end career is playable. Improve existing gameplay and interface behavior through bounded corrections based on observed issues. No new major feature or immediate production-deployment phase has been approved.

**Recommended next workstream: career navigation hierarchy and grouping.** Review the current sidebar destinations, their frequency and importance, and the hub cards. Produce a design recommendation for user review first; implementation is not yet authorized. A larger career calendar and time-progression redesign is a separate future task.

## Integration and verification snapshot

At this snapshot, `docs/project-continuity`, local `develop`, and `origin/develop` all point to `30501fb`; the working tree was clean before this documentation task. The merge commits below are on `develop`'s first-parent history. “Merged” means integrated in Git, not that a fresh Docker/browser acceptance run was performed for this handover. The cited implementation commits include focused tests; those tests were not rerun for this documentation-only task.

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

Successful recovery of a **Season 5 career** is a user-reported Docker/browser acceptance result associated with the large-save fix. The personal save is not in Git, so this specific recovery cannot be independently verified from repository contents. No personal saves, account identifiers, secrets, or backups belong in this handover.

Some OpenSpec change directories remain unarchived with checked tasks, including presentation and career-inbox work. Their presence is not evidence that they are the current priority or that every manual check is finished; read their own `tasks.md` before resuming them. The [player-experience roadmap](backend/player-experience.md) describes longer-term possibilities, not the current task order.

## Working principles

- Preserve existing gameplay functionality, save compatibility, saved careers, and progression. Favor narrow fixes with an observed failure or clear decision benefit.
- Keep human and AI golfers consistent where rules require parity; retain backend authority, deterministic simulation, and the pure `sim.*` / application boundary.
- Avoid broad redesign from vague instructions. Review an observation, agree on scope, then implement a focused change. Avoid new major features until the existing experience has been reviewed and improved.
- Codex investigates and implements focused repository work. The user performs Docker/browser acceptance and manual SourceTree version control. Leave Codex changes uncommitted unless separately instructed.

## Observation-led backlog

**Priority:** P1 = gameplay/functionality; P2 = usability/consistency; P3 = presentation/refinement. Visual quality still matters for a game. **State:** confirmed limitation/defect = evidenced current behavior to investigate or correct; candidate = observation requiring review and scope approval; design decision = unresolved choice; accepted deferral = intentionally later work. Priority does not authorize implementation.

| Priority / state | Area | Observation or decision to review |
| --- | --- | --- |
| P2 · candidate | Career navigation | Review sidebar destination hierarchy and grouping; separate frequent career actions from secondary records and information. Review the usefulness and order of hub bento cards. Mobile navigation and account access are already implemented. |
| P2 · design decision | Career time | Consider moving career-time simulation controls from the top bar into Schedule/Calendar. Explore a deliberate FIFA-style week/month/season flow with checkpoints for training, development, equipment, entries, and other meaningful decisions. Preserve existing in-round simulation controls. This is separate from the next navigation review. |
| P2 · candidate | Schedule | Review labels, filters, and information that may be unnecessary to players, including “Corrected flag placement.” |
| P2 · candidate | Golfer creation | Improve the long scrolling setup; consider responsive steps while retaining every current choice and gameplay effect. |
| P1 · candidate | Play information | Make wind speed and direction easier to see while deciding a shot. |
| P3 · candidate | Course presentation | Review Full Hole lighting, fade, and course presentation; keep the stronger Putting and Around the Green treatments where appropriate. Review Free Practice background and lighting consistency. |
| P1 · candidate | Practice parity | Consider greenside wedge parity in Free Practice after reviewing current play and training behavior. This is not an approved implementation slice. |
| P2 · candidate | Saved careers | Make careers easier to identify by golfer name where useful. Review exit availability in immersive Play and Practice. |
| P2 · design decision | Career count | Revisit whether a maximum number of named careers is desirable. No limit has been approved. |
| P2 · candidate | Season Review | Review duplicated Continue actions and the bottom panel covering review content; confirm exact behavior in the browser before scoping a correction. |
| P1 · confirmed limitation | Season simulation | Advancing a season can exceed the GraphQL proxy's response-header timeout. Duplicate-operation protection and safe reconciliation are merged; performance or transport optimization is still open. |
| P2 · known limitation | Finish Event response | The Finish Event UI handles a received `COMPLETED_UNSAVED` error, but an entirely lost completion response has no event-operation query for reconciliation. The outcome must be checked from current career state before any retry; this is a recovery/clarity gap, not evidence that an event was recorded twice. |
| P2 · confirmed limitation | Save growth | Save loading uses a bounded 256 MiB JSON payload-string limit. Long-term snapshot growth remains a scaling concern. Save listing currently reads full save envelopes and parses snapshot payloads to extract metadata; metadata access could be made cheaper. |
| P3 · accepted deferral | Greens | Broader procedural green-shape and contour variety remains later work, as recorded in [player experience](backend/player-experience.md#physical-putting-v1-first-playable-milestone). |

Promote a candidate to an implementation task only after an observed example, intended player outcome, scope, and acceptance checks have been agreed. Recheck Git and the relevant OpenSpec change before marking any item complete.

## Starting a new conversation

1. Ask the new ChatGPT or Codex conversation to read `AGENTS.md`, this page, `README.md`, and the relevant directory `AGENTS.md`. For product questions, read `docs/backend/explore.md` and `docs/backend/player-experience.md`; for UI work, read the relevant `docs/frontend/` guides and OpenSpec specifications/active change.
2. Run `git status --short --branch` and `git log --oneline --decorate --first-parent -15` to verify the actual branch, working tree, and integration state. Do not infer a merge from a prior conversation.
3. Use **this page's Observation-led backlog** for active observations and the **Current phase and next task** section for the current recommendation. Treat the integration table and code/tests as verified repository evidence; treat candidates and design decisions as proposals, and the Season 5 recovery as user-reported acceptance.
4. Start with a **design review of career navigation hierarchy and grouping**. Keep the larger calendar/time-simulation redesign separate. Leave changes uncommitted for the user's SourceTree review unless explicitly instructed otherwise.
