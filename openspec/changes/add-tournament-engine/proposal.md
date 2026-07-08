## Why

Three domains now exist in isolation — the shot engine resolves rounds, courses generate venues, and the population supplies golfers — but nothing makes them compete. The tournament engine is the convergence point: the first change that actually *plays golf competitively*, drawing a field, assigning a course, and playing four rounds to a single winner. It is also the concrete producer that the deferred consumer domains (Career, Rankings, Economy, Tours) will attach to, so building it now gives those domains a real result contract instead of a guessed one.

## What Changes

- Define a **Tournament** (REQ-086): name, Course, tier, entry requirements, prize structure, competition format, scheduled date — immutable once play begins.
- Define **Tournament Entry** (REQ-088): one per Player, no duplicates; and **Registration** (REQ-089) that checks eligibility and returns a clear reason on failure.
- Define the confirmed **Field** (REQ-090): the accepted entries, fixed once Round 1 begins.
- Implement the **lifecycle state machine** (REQ-087): Scheduled → Registration Open → Field Confirmed → R1 → R2 → Cut → R3 → R4 → Playoff → Completed, with no skipped states.
- Implement **four-round play** (REQ-091/092): every competitor's round is resolved through the existing shared `RoundResolver` (no separate human/AI path); scores carry forward; completed rounds are immutable; scoring is strokes relative to Course par, lower is better.
- Maintain a **live leaderboard** (REQ-094) recomputed on score change, with consistent tie handling.
- Implement **cut evaluation** exactly once after Round 2 (REQ-093): top N plus ties advance, identical criteria for all; formats without a cut may omit it.
- Implement **playoff resolution** (REQ-095): when competitors tie for first after Round 4, play sudden-death holes until exactly one winner remains.
- Implement **completion** (REQ-096): finalize only after scores, winner, prize distribution, and recorded statistics; the Tournament becomes read-only.
- Handle **withdrawal** (REQ-097) before/during play without invalidating the event, and guarantee **competitive integrity** (REQ-098): identical rules for all, outcomes reproducible from seed.
- Preserve **permanent tournament history** (REQ-099): course, competitors, scores, winner, prize distribution.

Explicitly out of scope: the Tour hierarchy — tiers, membership, promotion/relegation (REQ-126–140); Rankings updates (REQ-141–150) — results are produced here and consumed by Rankings later; the Economy — prize distribution records payout *amounts* from the prize structure but no financial ledger; Weather generation — rounds are resolved under a calm or per-tournament-seeded `Environment`; and the human's interactive per-shot path — every competitor is resolved headlessly via `resolveRound` in this change (the shot-by-shot UI is a later presentation concern). The engine is not responsible for shot calculations, progression, finance, course generation, or weather (REQ-100).

## Capabilities

### New Capabilities
- `tournament-definition`: The Tournament entity, Tournament Entry, Registration/eligibility, and the confirmed Field (REQ-086/088/089/090). Tour is an inline tier/eligibility reference only.
- `tournament-play`: The lifecycle state machine, four-round play resolved via the shared engine, cumulative relative-to-par scoring, and the live leaderboard with tie handling (REQ-087/091/092/094).
- `tournament-cut-playoff`: One-time cut evaluation after Round 2 and sudden-death playoff resolution to exactly one winner (REQ-093/095).
- `tournament-completion`: Completion, withdrawal handling, competitive integrity/reproducibility, and permanent history including prize-amount distribution (REQ-096/097/098/099/100).

### Modified Capabilities
<!-- None. This change DEPENDS ON existing capabilities — golfer-population, course-generation
     (and course-*), shot-resolution, hole-spatial-model, deterministic-rng — but changes none of
     their requirements. It composes them; it does not modify them. -->

## Impact

- **Codebase**: New framework-free `com.progolf.sim.tournament` package in the existing `backend/` module. It composes `population` (field), `course` (venue + per-round `HoleModel`), `shot` (`RoundResolver`), and `core` (seed hierarchy). No changes to those packages.
- **Determinism**: Tournament resolution maps directly onto the `SeedCoordinate` (world→season→tournament→round→golfer→hole→shot), so a full event is reproducible bit-for-bit from its seed (REQ-098/265/299).
- **Downstream consumers (future changes)**: Career attaches tournament results to golfer history; Rankings recompute from results; Economy converts prize amounts into a financial ledger; Tours decide which tournaments exist and drive promotion/relegation. The result/history types defined here become the contract those domains consume.
- **Stubs to revisit**: prize distribution records amounts but no ledger; playing conditions are calm/seeded pending the Weather domain; eligibility is a simple inline tier check pending Tours. Each is a documented seam, not a hidden shortcut.
