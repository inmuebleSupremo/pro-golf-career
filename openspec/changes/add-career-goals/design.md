# Design — add-career-goals

## Context

Career goals are the player's self-defined success criteria. They must be lightweight (a handful of ambitions), self-chosen, and — critically — **never gate play** (player-experience §1.6): they frame and motivate, they do not unlock or restrict anything. They are surfaced by the narrative/stats layer: progress is readable, and achieving one is a story beat.

## Decisions

### D1 — Goals are observational; they never affect outcomes
A `CareerGoal` is a target the World *reads* the current world state against. Nothing about a goal feeds back into resolution, ranking, entry, or any competitive computation. The guarantee is testable: a world's ranking, careers, and records are identical whether or not goals are set — only the narrative gains the achievement news (media is already a pure observer). This keeps goals purely a framing layer.

### D2 — A small, mapped `GoalType` set over existing metrics
`GoalType` maps each ambition to a metric the World already exposes:
- `REACH_TOP_TOUR` → tour membership is the top (Elite) tier.
- `WIN_A_MAJOR` → career majors won ≥ target (default 1).
- `WORLD_NUMBER_ONE` → current ranking position is 1.
- `CAREER_WINS` → career wins ≥ target.
- `CAREER_EARNINGS` → career earnings ≥ target.
- `HALL_OF_FAME` → `HallOfFame.evaluate(careerStatistics).eligible()`.
Each evaluates to a `(current, target)` pair; achieved is `current >= target`. Boolean-style goals use target 1 with current 0/1; targeted goals (`WIN_A_MAJOR`, `CAREER_WINS`, `CAREER_EARNINGS`) use the player's number. `GoalType`/`CareerGoal` stay in `sim.control` (core-only labels); the evaluation — which needs ranking/career/tour — lives in `World`.

### D3 — Progress evaluated on read; achievement detected at advance seams
`careerGoals()` computes progress on demand from current state (no stored progress to drift). Achievement *announcement* needs edge-detection, so the World keeps an `achievedGoals` set and, in `checkCareerGoals()`, announces any goal that is newly achieved (publishing `goalAchieved` once and adding it to the set). The check runs at the two seams where the underlying metrics change: after each event (`feedConsumers` — wins, majors, ranking) and at the season transition (`seasonalTransition` — tour tier, year-end ranking, longevity). The set dedupes, so an already-announced goal is never repeated. `achievedGoals` is cleared when a player is (re)assigned or created.

### D4 — A dedicated `GOAL_ACHIEVED` news type
Achieving a self-chosen goal is a distinct, personal narrative beat, so it gets its own `NewsType.GOAL_ACHIEVED` and a high prominence (at/above the significance threshold, so it stays discoverable) rather than reusing the generic milestone. `NewsFactory.goalAchieved(season, id, name, description)` renders a headline from a per-type description ("reached the Elite tour", "won a major", "became world number one", …).

## Risks

- **Accidentally gating** — avoided by construction (goals are read-only) and asserted by a byte-identity test on competitive outcomes (goals-set vs no-goals worlds) that tolerates only the extra narrative news.
- **Re-announcement spam** — the `achievedGoals` set (value-keyed on the immutable `CareerGoal`) guarantees one announcement per goal; changing the goal list does not resurface an already-achieved goal.
- **Evaluation cost** — `checkCareerGoals` runs per event but evaluates only the player's handful of goals against O(1) lookups; negligible.
