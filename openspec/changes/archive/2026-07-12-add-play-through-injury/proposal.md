## Why

Today an injury is binary: a golfer who is Injured or Recovering simply cannot compete until healed. That is realistic for a serious injury but misses a real career decision — a pro nursing a minor knock (or in the tail of a rehab) can choose to **grind through** an event at reduced performance instead of sitting out. Adding that choice gives the controlled golfer's career meaningful variance and a genuine risk/reward: play now, hurt and below your best, and stay hurt longer — or rest, heal cleanly, and miss the event. The idea was seeded by the (now-removed) dead injury scaffold's `performancePenalty`; this gives it a real home in the live `sim.health` domain.

## What Changes

- A golfer in the **Recovering** stage of an injury (rehab in its final weeks — which every MINOR injury is from the start, plus the tail of MODERATE/SEVERE) becomes eligible to **play through** the injury rather than being benched. The early **Injured** stage of a serious injury stays benched.
- Playing through carries an **impairment penalty** scaled by injury severity (MINOR small, MODERATE larger, SEVERE largest): a new shot input that widens dispersion, shortens carry, and lowers putt make-rate — physical, so the psychologist does NOT relieve it (unlike fatigue).
- Playing through **freezes rehabilitation**: rehab only advances in a week the golfer did NOT compete. Grinding keeps you hurt longer; resting is what heals you. Deterministic — no new randomness.
- The play-through option is the **controlled golfer's** strategic choice, expressed through the existing schedule (enter = grind, skip = rest). AI-controlled golfers keep resting recovering injuries and heal exactly as before, so world scoring, longevity, and reproducibility are unchanged.
- The impairment is synced into the shot engine from Physical State exactly as accumulated **fatigue** already is (a primitive crossing the boundary), so the Health domain never owns shot resolution.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `injury-recovery`: an injury no longer unconditionally prevents competition — a recovering golfer MAY play through at a severity-scaled impairment, and rehabilitation advances only during weeks the golfer does not compete.
- `availability`: the Recovering availability MAY permit competition for a controlled golfer who chooses to play through (still benched otherwise); the early Injured stage never permits it.
- `shot-resolution`: a new injury-impairment condition input degrades shot outcomes (dispersion, carry, putting), independent of and not relieved by staff mental support.
- `world-progression`: the field gate admits a controlled golfer who opts to play through a recovering injury; weekly rehabilitation is frozen for any golfer who competed that week.

## Impact

- `com.progolf.sim.health`: `InjurySeverity` (impairment per severity), `PhysicalState` (`canPlayThroughInjury`, `injuryImpairment`), `HealthSystem.recoverWeek` (competed-this-week freezes rehab), `HealthConstants`.
- `com.progolf.sim.shot`: `GolferState` (append `injuryImpairment` input), `ShotResolver` (apply it to full shots + putts), `SimConstants` (injury weights).
- `com.progolf.sim.player`: `PlayerState` (transient `injuryImpairment` shot input, synced before play like fatigue), `Player.toGolferState`.
- `com.progolf.sim.world.World`: field-entry helper allowing the controlled golfer to play through; `recoverHealth` passes the competed-this-week flag (reusing `committedThisWeek`); impairment synced in `buildEvent`.
- No change to AI field behaviour, world calibration, or serialization.
