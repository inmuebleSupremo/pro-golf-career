## 1. Identity & status

- [x] 1.1 Create framework-free package `com.progolf.sim.player` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Define `Nationality` (validated code set) and an immutable `Identity` record (first/last name 1–50 chars, nationality, date of birth, archetype); reject invalid/missing fields.
- [x] 1.3 Define `Archetype` enum (Grass Roots Talent, Top College Graduate, Future Prodigy) for identity (starting-attribute generation lives in population).
- [x] 1.4 Define `CareerStatus` enum (CREATED, ACTIVE, INJURED, RETIRED, DECEASED) with a `canTransitionTo` table and a guarded transition; RETIRED/DECEASED terminal.

## 2. Player state

- [x] 2.1 Define `Fatigue` handling as temporary state (0..1) with no path to mutate attributes.
- [x] 2.2 Define `LiveSkillRating` with baseline, `recordPerformance(delta)` (clamped), and `decayTowardBaseline(step)`; magnitudes in a `PlayerConstants` surface.
- [x] 2.3 Define `Injury` (type, severity, recoveryRemaining, effects) and `InjuryType`/severity; `advanceRecovery(step)` decrements and clears at zero.
- [x] 2.4 Define `PlayerState` aggregating Fatigue, Live Skill Rating, and `Optional<Injury>`; enforce zero-or-one active injury.

## 3. Player aggregate

- [x] 3.1 Implement `Player` = unique id + immutable `Identity` + reference to `core.Attributes` + mutable `PlayerState` + `CareerStatus`; identity/attributes never mutated in place.
- [x] 3.2 Implement derived-statistics access as recalculated-on-demand (one illustrative derived value; never stored).
- [x] 3.3 Implement a `toGolferState()` factory producing the shot engine's `shot.GolferState` from current state (no attribute/state duplication).
- [x] 3.4 Coordinate injury/status: applying an injury moves ACTIVE→INJURED; full recovery moves INJURED→ACTIVE, via the guarded transition.

## 4. Professional Golfer

- [x] 4.1 Define `ControlType` enum (HUMAN, SIMULATION), assigned at creation and immutable.
- [x] 4.2 Implement `ProfessionalGolfer` = `Player` + Career reference id + `ControlType`; expose read access without leaking mutability of identity.
- [x] 4.3 Define the `DecisionPolicy` seam interface for simulation decisions (interface only; no implementation); ensure no gameplay type branches on control type.
- [x] 4.4 Confirm (code/test) no gameplay path reads control type as a rule input (mirrors the shot engine's control-free design).

## 5. Population

- [x] 5.1 Add `PopulationConstants` (size defaults, attribute-profile spread) as the single tunables surface.
- [x] 5.2 Implement `PopulationGenerator.generate(seedCoordinate, size)` deriving a per-golfer seed and a diverse `Attributes` profile (overall skill + randomized strength/weakness shape); all via the seed hierarchy.
- [x] 5.3 Implement replenishment: introduce new golfers at fresh indices to keep the population sufficient, independent of the human player.
- [x] 5.4 Ensure rivalries are not authored at generation (no scripted-rival fields).

## 6. Verification

- [x] 6.1 Ownership/immutability tests: identity immutable after creation; Fatigue/Live Skill Rating/Injury changes never alter permanent Attributes; no duplicated attribute store.
- [x] 6.2 Status-machine tests: valid transitions accepted; invalid transitions (e.g. RETIRED→ACTIVE) rejected; terminal states enforced.
- [x] 6.3 Live Skill Rating tests: up on strong performance, down on poor, reverts toward baseline on inactivity, bounded.
- [x] 6.4 Injury tests: at most one active injury; recovery clock decrements and clears; INJURED↔ACTIVE coordination.
- [x] 6.5 Control-type tests: immutable; identical Players differing only in control type are treated identically; no gameplay branch on control type.
- [x] 6.6 Population tests: reproducible from seed; measurable diversity (variance in overall skill and in each golfer's strongest attribute); replenishment keeps size sufficient; no scripted rivals.
- [x] 6.7 Integration check: a generated Player feeds the shot engine via `toGolferState()` and resolves a shot (no change to `sim.shot`).
- [x] 6.8 Run `openspec validate add-golfer-entities --type change --strict` and resolve findings.
