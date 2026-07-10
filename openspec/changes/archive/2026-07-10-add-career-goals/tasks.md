## 1. Control: goal model

- [x] 1.1 Add `com.progolf.sim.control.GoalType` { REACH_TOP_TOUR, WIN_A_MAJOR, WORLD_NUMBER_ONE, CAREER_WINS, CAREER_EARNINGS, HALL_OF_FAME }.
- [x] 1.2 Add `com.progolf.sim.control.CareerGoal(GoalType type, long target)` with static factories `of(type)` (target 1) and `of(type, target)`.
- [x] 1.3 `PlayerControl` holds `List<CareerGoal> careerGoals` with `setCareerGoals(List)` and a read accessor (core-only, framework-free).

## 2. Media: goal-achieved news

- [x] 2.1 Add `NewsType.GOAL_ACHIEVED`, `MediaConstants.PROMINENCE_GOAL_ACHIEVED` (≥ significance threshold), and `NewsFactory.goalAchieved(season, golferId, name, description)`.

## 3. World: evaluate, surface, announce

- [x] 3.1 Add `CareerGoalProgress(CareerGoal goal, long current, long target, boolean achieved)` record and a private `evaluate(playerId, goal)` mapping each `GoalType` to the live metric (tour tier / ranking position / wins / majors / earnings / HoF eligibility).
- [x] 3.2 `World.setCareerGoals(List<CareerGoal>)` (require player) and `careerGoals()` → the progress view.
- [x] 3.3 `checkCareerGoals()` — announce newly-achieved goals once via a `achievedGoals` set + `NewsFactory.goalAchieved`; call at the end of `feedConsumers` and `seasonalTransition`. Clear `achievedGoals` on `assignPlayer` (createPlayer routes through it).
- [x] 3.4 `WorldService`: expose `setCareerGoals` / `careerGoals`.

## 4. Verification

- [x] 4.1 Set/read: goals set are retrievable as progress entries; no player → `careerGoals` requires a player.
- [x] 4.2 Progress: a fresh created player shows unachieved goals; after achieving one (e.g., WIN_A_MAJOR once a major is won, or CAREER_WINS target met), it reads achieved with the right current/target.
- [x] 4.3 Never-gate: two same-seed worlds, one with goals set (that get achieved) and one without, have identical rankings, careers, and records; only the goals world has extra `GOAL_ACHIEVED` news.
- [x] 4.4 Announce-once: an achieved goal produces exactly one `GOAL_ACHIEVED` news item across further advances.
- [x] 4.5 Full suite + `ArchitecturePurityTest` pass.
- [x] 4.6 `openspec validate add-career-goals --type change --strict`.
