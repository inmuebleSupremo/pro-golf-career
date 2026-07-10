## Why

The player now creates and guides their own golfer, but the career has no *self-defined* point — no "what am I chasing." The vision (player-experience §8, a confirmed V1 decision; §1.6) calls for **lightweight, self-chosen career goals** — reach the top tour, win a major, become world #1, win N tournaments, make the Hall of Fame — that **frame** progress and legacy but **never gate** play. They are the player's own answer to "what would make this career a success," surfaced by the narrative/stats layer: progress is trackable, and achieving one is a story moment. This closes out onboarding (after create-your-golfer) and gives every career phase something to aim at (§1.5).

## What Changes

- Add **`com.progolf.sim.control.CareerGoal`** (a `GoalType` + an optional numeric target) and **`GoalType`**: `REACH_TOP_TOUR`, `WIN_A_MAJOR`, `WORLD_NUMBER_ONE`, `CAREER_WINS`, `CAREER_EARNINGS`, `HALL_OF_FAME`. The player sets a handful of these as standing ambitions; `PlayerControl` holds them (framework-free, like the other player decisions).
- **Progress is surfaced, never gating.** `World.careerGoals()` returns a `CareerGoalProgress` view for each goal — its current value, its target, and whether it is achieved — evaluated on read against the player's real world state (tour tier, ranking position, career wins, majors won, earnings, Hall-of-Fame eligibility). Goals are purely observational: they change no competitive outcome and gate no play.
- **Achieving a goal is a narrative moment.** When a goal first becomes achieved during an advance, the World publishes a one-time `NewsFactory.goalAchieved` (a new high-prominence `GOAL_ACHIEVED` news type) into the world narrative. Already-announced goals are not re-announced.
- New actions `World.setCareerGoals(...)` / `careerGoals()`; exposed on `WorldService`. Assigning or creating a player clears any prior goal state.

Explicitly out of scope: goals that unlock or restrict anything (they never gate — a hard guarantee); rewards for achieving goals (no prize/attribute effect); AI golfers having goals (goals are the human's framing only); goal *suggestions* or difficulty scoring.

## Capabilities

### New Capabilities
- `career-goals`: the player sets lightweight, self-chosen career ambitions that never gate play; the World tracks progress toward each against real career state and surfaces it, and achieving a goal is announced once in the world narrative.

## Impact

- **Codebase**: new `CareerGoal` + `GoalType` in `sim.control`; `PlayerControl` holds the goals; `World` evaluates them (`careerGoals`, achievement check at the event and season seams) with a small `CareerGoalProgress` record; `NewsFactory`/`NewsType`/`MediaConstants` gain the goal-achieved item; `WorldService` exposes the actions. No change to resolution, ranking, careers, or the world loop's outcomes.
- **Determinism / never-gate**: goals are read-only observers of existing state, so a world's competitive outcomes (ranking, careers, records) are identical whether or not the player sets goals — only the world narrative gains the achievement news. Evaluation is deterministic.
- **DAG**: `GoalType`/`CareerGoal` live in `control` (core-only); `World` already composes `control`, `career`, `ranking`, `tour`, and `media`. No inversion.
- **Boundary**: `World` reads goal progress from the domains it already coordinates and publishes achievement news through `media`; it invents no new career metrics.
