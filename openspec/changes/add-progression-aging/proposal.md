## Why

The world runs and golfers age toward retirement, but their **attributes never change** — a 40-year-old is as good as they were at 20, and a promising rookie never improves. That guts the core fantasy ("long-term player development") and the pillar that a decision at 18 is felt at 40. Progression & aging is the piece that makes careers *arc*: golfers earn Development Points and improve where they invest, then age along attribute-specific curves — physical gifts fade while judgment compounds. Wired into the World's seasonal transition, it turns the living world from a static cast into one with rising stars, primes, and graceful declines.

## What Changes

- Add **Development Points** (REQ-151/155): earned each season from competitive participation, and **allocated to raise chosen permanent attributes** — enabling specialisation while allowing balance (REQ-154). Growth is **gradual** — meaningful over multiple seasons, not single tournaments — avoiding numerical inflation (REQ-156).
- Add a **deterministic AI allocation policy** so simulation golfers develop without input, by the same rules as the player (REQ-163).
- Add **attribute-specific aging** (REQ-159): different attributes peak at different ages and decline at different rates — physical (driving distance/accuracy) peak in the late 20s and fade; skill (irons/wedges/putting) peak later and hold; mental (composure/course management) keep rising into the 40s. Regression is gradual and believable (REQ-160).
- Add **career stages** — Development, Prime, Late Career — that shape development opportunity without changing the rules (REQ-152); every golfer has a peak-performance period, older golfers stay viable, and trajectories are diverse (REQ-158/161/162).
- Make **permanent Attributes evolvable** (modifies `player-entity`): they increase through development and decrease through aging/regression, always clamped to 0–100, with every permanent change recorded (REQ-017/048/049/153). This adds a guarded attribute-evolution operation to `Player`.
- Wire progression into the **World seasonal transition** (modifies `world-progression`): each season, every active golfer is awarded and allocates Development Points and then ages, so the living world's golfers improve and decline over time.

Explicitly out of scope: any path by which **random tournament performance permanently changes attributes** (REQ-049) — only development and aging do; the player-facing UI for choosing allocations (the engine exposes an allocation input; a simple AI policy drives it for now, and the player UI is a later Presentation concern); and long-term-injury regression (the Health domain, deferred). Progression is analytical and deterministic — a pure function of attributes, age, and allocation, no RNG.

## Capabilities

### New Capabilities
- `player-development`: Development Points earned per season and allocated to raise chosen permanent attributes gradually (specialisation with balance), with a deterministic AI allocation policy and recorded changes (REQ-151/153/154/155/156/163).
- `player-aging`: Attribute-specific aging curves (different peaks and decline rates), gradual regression, career stages, peak performance, longevity, and diverse trajectories (REQ-152/158/159/160/161/162).

### Modified Capabilities
- `player-entity`: Permanent Attributes become evolvable — increasable through development, decreasable through aging/regression, clamped 0–100, and every permanent change recorded. Adds a guarded attribute-evolution operation to `Player` (attributes were fixed after creation).
- `world-progression`: The World's seasonal transition additionally applies development and aging to every active golfer each season.

## Impact

- **Codebase**: New framework-free `com.progolf.sim.progression` package. It computes attribute evolution as a pure function; `Player` gains a guarded `evolveAttributes` operation; `World.seasonalTransition` calls the progression engine for each active golfer.
- **Determinism**: Development awards, AI allocation, and aging are pure functions of attributes/age — no RNG — so a world with progression remains reproducible from its master seed.
- **Downstream consumers (future changes)**: the Presentation/UI layer will surface Development-Point allocation to the human player; the Economy/objectives may tie development opportunities to staff/finances; Career history already records milestones and will surface progression events.
- **Boundary/risk**: attribute evolution is the first sanctioned mutation of a permanent Player value — guarded to stay within 0–100, recorded, and driven only by development/aging (never by tournament randomness). The seasonal-transition wiring touches the World, whose full-run reproducibility test guards against regressions.
