## Why

A golfer exists and can compete, and the world ranks them — but nothing yet ties a golfer's events together into a *career*: an arc from a first professional start to mandatory retirement, with a cumulative record and a legacy. The `ProfessionalGolfer` already references a Career by id, but no such entity exists. Building it now gives every golfer a timeline and a record that tournaments feed into, and it is the container the whole game is about ("living the career of a professional golfer over decades").

## What Changes

- Define the **Career** entity: the complete playable history of one golfer, owned by a `Player`, beginning on activation (REQ-025/026).
- Track **Career Age** as whole years that advance once per completed season and never decrease (REQ-027), across the **16–65 timeline** with **mandatory retirement at 65** that cannot be bypassed (REQ-028).
- Provide a **seasonal-advance mechanism** that advances age and archives the completed season (REQ-029). Calendar *generation* is deferred to the World; the Career exposes the advance, the World drives cadence.
- Coordinate with the Player's **`CareerStatus`** and support **runtime states** (Active/Saved/Loaded/Paused) that affect execution only, never progression (REQ-034).
- **Complete** the Career on retirement, making it read-only (REQ-035), and guarantee **integrity** — historical records are append-only and a completed career is reconstructible from them (REQ-037).
- Maintain **cumulative career statistics** from tournament results — events played, cuts made, wins, runner-ups, top-10s, average finish, total earnings (REQ-032).
- Auto-record **career milestones** — first event, first cut, first top-10, first win, etc. — objectively, with duplicate prevention (REQ-031).
- Maintain a **chronological, immutable career history** of participation and significant events (REQ-033).
- Perform a **Hall-of-Fame eligibility evaluation** automatically on retirement and record the result permanently (REQ-036); the detailed criteria are a placeholder seam refined later by the Legacy Systems spec.

Explicitly out of scope: progression, aging, and regression (REQ-151–165) — the Career advances age but does not evolve attributes; World calendar/tournament-schedule generation (REQ-030 / REQ-101–112) — the Career exposes a seasonal-advance hook the World will drive; and the full Hall-of-Fame criteria — evaluated via a placeholder now. The Career is analytical: it never mutates player attributes or tournament results.

## Capabilities

### New Capabilities
- `career-lifecycle`: The Career entity, its start, whole-year Age and 16–65 timeline with mandatory retirement at 65, the seasonal-advance mechanism, runtime states, completion/read-only on retirement, and append-only integrity (REQ-025/026/027/028/029/034/035/037).
- `career-record`: Cumulative career statistics, auto-recorded objective milestones with duplicate prevention, and a chronological immutable career history — all updated from tournament results (REQ-031/032/033).
- `career-legacy`: An automatic Hall-of-Fame eligibility evaluation on retirement, permanently recorded, with a placeholder criteria seam that never alters history (REQ-036).

### Modified Capabilities
<!-- None. This change DEPENDS ON existing capabilities — player-entity (Player, CareerStatus),
     professional-golfer (career reference), tournament-completion (TournamentResult) — but changes
     none of their requirements. It realises the Career that ProfessionalGolfer already references. -->

## Impact

- **Codebase**: New framework-free `com.progolf.sim.career` package in the existing `backend/` module. The Career belongs to a `Player`, consumes `tournament.TournamentResult`, and coordinates the Player's `CareerStatus` for retirement. No changes to existing packages.
- **Determinism**: Statistics, milestones, and history are pure functions of the sequence of recorded results and season advances; the same inputs reproduce the same career (REQ-037).
- **Downstream consumers (future changes)**: The World/season loop will drive `advanceSeason()` and calendar generation; progression/aging will hook into the seasonal transition; the Legacy Systems spec will refine Hall-of-Fame criteria; media/statistics domains will read career history. The seasonal-advance and record contracts defined here are the seams those build on.
- **Deferred seams**: the seasonal-advance hook (World drives cadence), and the Hall-of-Fame criteria (placeholder). Each is documented, not a hidden shortcut.
