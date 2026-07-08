## Context

This is the greenfield foundation of the Pro Golf Career Simulation. `docs/explore.md` (REQ-001–316) defines *what* the game does but defers the shot mathematics to a non-existent "Shot Resolution Specification"; `docs/tech_stack.md` pins Java 21 / Spring Boot 3 / PostgreSQL 17 / GraphQL. Before this change there is no code.

Eight foundational engine decisions were agreed with the product owner and recorded in project memory. The three that bind this change directly:

- **Shared model, abstracted AI** — one engine, two entry points (`resolveShot` for the player's per-shot loop, `resolveRound` for AI). Identical rules for human and AI are guaranteed *structurally* (REQ-104/110/123), not by convention.
- **Hybrid spatial model** — a hole is drawn in 2D but resolved in 1D (distance-to-pin + lateral offset against weighted surface/hazard **zone bands**). The zone band is the explicit seam between (future) course generation and shot resolution.
- **Deterministic RNG** (mandated by REQ-047/265/299) — a per-world master seed derives seeds per season → tournament → round → shot.

This change delivers the pure mathematical core only. Everything else (course generation, tournaments, rankings, economy, progression, weather, persistence, UI) is a downstream consumer and a separate change.

## Goals / Non-Goals

**Goals:**
- Define the numerical value model and the fixed 7-step calculation pipeline as executable domain code.
- Deliver one shared, deterministic shot-resolution function exposed as `resolveShot` and `resolveRound` that provably sample identical distributions.
- Establish the zone-band abstraction as the stable contract course generation will later produce and the shot engine consumes.
- Establish the seed hierarchy so all downstream randomness has exactly one governed source.
- Keep the core framework-free and unit-testable in isolation (REQ-301), independent of Spring, persistence, and I/O.

**Non-Goals:**
- Course *generation* algorithms (this change only defines the zone-band contract they must emit).
- Tournament flow, cut/playoff logic, scoring aggregation beyond a single shot/round.
- Rankings, economy, progression, aging, and weather *generation* math.
- Persistence wiring, GraphQL schema, and any UI. The master seed's *storage* is downstream; this change defines its *derivation*.
- Final numeric tuning of realism constants — structure now, calibration later.

## Decisions

### D1. Pure domain module, no framework in the core
The engine lives in plain Java packages (e.g., `sim.core` for the numerical model + RNG, `sim.spatial` for zone bands, `sim.shot` for resolution) with **no Spring annotations, no JPA, no I/O**. Spring wiring happens in downstream changes via thin adapters. *Why:* testability and determinism (REQ-301/299); the core must be exercisable millions of times in tests without a container. *Alternative rejected:* Spring `@Service` beans throughout — couples pure math to the framework and slows tests.

### D2. Immutable inputs/outputs, functions not stateful objects
Resolution is expressed as pure functions over immutable value objects (Java `record`s): `resolveShot(ShotContext, Rng) -> ShotOutcome`. No hidden mutable state. *Why:* order-independence and reproducibility fall out for free; a `ShotContext` fully determines its distribution (REQ-047). *Alternative rejected:* a mutable `Shot` object accumulating state — reintroduces evaluation-order coupling.

### D3. Seed hierarchy via a splittable, coordinate-addressed generator
Use a documented splittable generator (JDK `SplittableRandom` / `RandomGenerator.SplittableGenerator`, or an explicit SplitMix64 we control). A child seed is a **pure hash of (parent seed, stable child id)** — e.g., `mix(parentSeed, hash(scopeId))` — rather than sequential splitting, so a seed depends only on its coordinate, never on traversal order (spec: Order Independence, Sibling Isolation). Coordinate = `(worldSeed, seasonId, tournamentId, roundNo, golferId, holeNo, shotNo)`. *Why:* guarantees save/load reproducibility and lets any shot be resolved in isolation. *Alternative rejected:* a single sequentially-advanced PRNG per round — result depends on how many prior draws occurred, breaking isolation and lazy/parallel evaluation. *Alternative rejected:* `java.util.Random` — weak statistical quality and no clean split. We will pin the algorithm and document it (spec: Documented Generator Algorithm) so it is stable across restarts and deploys.

### D4. Distribution family: Normal for continuous error, with strategy/attribute-driven variance
Carry distance and lateral offset are sampled from **Normal distributions** whose mean comes from steps 1–4 and whose standard deviation shrinks with the relevant accuracy/control attributes and grows with adverse modifiers and Aggressive strategy. Rare extreme events (mishits, hero recoveries) come from an explicit low-probability tail/mixture rather than fattening the whole curve, keeping "average is most common, extremes rare" (REQ-046). *Why:* Normal is the natural continuous-error model, cheap, and analytically inspectable for tests (we can assert mean/variance monotonicity). *Alternatives considered:* triangular (simpler but hard shoulders look artificial); heavy-tailed t-distribution everywhere (extremes too frequent). Exact constants are deferred to calibration.

### D5. Zone bands as an ordered 1D partition with lateral sub-regions
A `ZoneBand` declares `[distanceStart, distanceEnd)`, and for that interval a set of lateral sub-regions (`centralExtent -> surface`, then flanking regions outward) each with a surface + weight. Bands for a shot context must **completely partition the reachable range** (spec: Bands fully partition). Resolution: sample carry → pick the distance band; sample lateral offset → pick the sub-region → read surface. *Why:* makes "draw in 2D, compute in 1D" concrete and gives course generation a precise output contract. *Alternative rejected:* querying 2D polygons at resolution time — the whole point of the hybrid decision is to avoid that cost and nondeterminism.

### D6. `resolveRound` literally composes `resolveShot`
Abstracted AI is not a parallel implementation — `resolveRound` loops the identical per-shot core, deriving each shot's decision from a simple strategy policy and seeding it at the same shot coordinate a `resolveShot` call would use. *Why:* the specs demand distribution equivalence across entry points; the cheapest way to guarantee it is to share the exact code path. *Alternative rejected:* a closed-form "round score" sampler for AI — faster, but would drift from the per-shot distributions and violate REQ-104.

### D7. Explainability via a returned factor breakdown
`ShotOutcome` carries a small structured breakdown of dominant contributing factors (attribute contribution, environment, strategy, luck delta) alongside the physical result. *Why:* REQ-065 requires outcomes be explainable; capturing factors at resolution time is far cheaper than reconstructing them later. *Trade-off:* a little extra allocation per shot — acceptable; can be made opt-in for bulk AI resolution if profiling demands.

## Risks / Trade-offs

- **[Distribution realism unproven until calibrated]** → Structure the code so all realism constants live in one configuration surface; add statistical tests (mean/variance/percentile assertions over large N) now, tune values later without touching logic.
- **[Abstracted AI could still diverge from player distributions if a shortcut sneaks in]** → Enforce by construction (D6) and add a property test that resolves the same seeded hole via both entry points and asserts identical outcomes (spec: Distribution equivalence).
- **[Seed-derivation algorithm change would silently break reproducibility of existing saves]** → Pin and version the algorithm; treat any change as a breaking migration; cover with a cross-process golden-value test.
- **[Bulk `resolveRound` performance at Standard scale (~360 golfers × ~30 events)]** → Core is allocation-light and O(shots); a full field-week is a few thousand shot samples. Keep the factor breakdown suppressible for AI if profiling shows pressure.
- **[Zone-band contract might be under-specified for future course generation needs]** → Ship it as a versioned interface; it is additive-friendly (new surfaces/fields) per REQ-282, so generation can extend without breaking resolution.

## Migration Plan

Greenfield — no rollback surface. Sequencing: (1) numerical model + value categories; (2) seed hierarchy + generator; (3) zone-band types + surface catalogue; (4) shot-resolution core composing the above; (5) `resolveShot`/`resolveRound` entry points; (6) statistical + reproducibility test suites. Each layer is independently testable before the next builds on it.

## Open Questions

- Exact realism constants (base carry per club, σ-vs-attribute curves, tail probabilities) — deferred to a dedicated calibration pass with target scoring distributions.
- Whether the factor breakdown (D7) is always-on or opt-in for `resolveRound` — decide after first performance profiling.
- Precise `ShotContext` field set for lie/elevation/wind — enough is defined here for resolution; the full environmental schema is finalized alongside the Weather change that produces it.
- Where the per-world master seed is persisted (world row vs. save metadata) — belongs to the persistence change; this change only consumes it.
