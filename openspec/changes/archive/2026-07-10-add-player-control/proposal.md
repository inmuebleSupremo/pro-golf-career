## Why

The product is *"the player assumes control of a single golfer and guides their career."* The engine has always been built for this — every decision runs through a policy seam, and the rules are control-agnostic (identical for human and AI, REQ-104) — but there is **no human decision surface anywhere**. Every choice that defines the game (accept a sponsor, invest in development, play or rest) is taken by an AI policy. This change introduces the **player-control loop**: a designated golfer becomes human-controlled, and the player's standing decisions are applied at the engine's existing seams during a normal advance. It is the change that turns the simulation into a game — the first slice where *you* are actually playing.

## What Changes

- Add **`PlayerControl`** (a designated golfer id + the player's standing decisions) that the World holds optionally; a world with no assigned player runs **fully autonomously, exactly as today**.
- Let the World **designate a golfer as player-controlled** (`assignPlayer`), and expose the player's actions: set a **development focus**, toggle **resting**, review **pending sponsorship offers**, and **accept** them.
- Apply the player's decisions at the three existing seams, for their golfer only (AI unchanged for everyone else), under the locked **configure-then-advance** model — no mid-advance pausing:
  - **Development** — the player's chosen attribute focus drives Development-Point allocation instead of the AI policy (modifies `player-development`).
  - **Schedule** — a resting player is excluded from event fields (they sit out and recover), reusing the availability seam.
  - **Sponsorship** — the player's golfer's generated offers go to a **pending queue** awaiting the player's accept/decline, instead of being auto-signed by the AI acceptance policy.
- Surface the player operations on **`WorldService`** (the app's engine boundary) so the later GraphQL layer can wire them to a UI. The loop is exercised end-to-end by a test that "plays a turn."

Explicitly out of scope for this first slice (the "focused core" chosen in scoping): player control over **staff** and **equipment** (they remain AI-driven and follow the same pattern in a later slice); custom golfer creation (the player designates an existing generated golfer for now); mid-advance pause/resume interaction; and the GraphQL/UI surface (later). This change adds player agency to the engine and service layer; it makes the game *playable through the service*, with the real UI to come.

## Capabilities

### New Capabilities
- `player-control`: A single golfer per world may be designated human-controlled; the player configures standing decisions (development focus, resting, sponsorship acceptance) that the engine applies at its decision seams during a normal advance (configure-then-advance), while every other golfer and every unassigned world continues to run fully autonomously (REQ-104-style control-agnostic rules; the vision's single-golfer control).

### Modified Capabilities
- `player-development`: Seasonal development MAY follow a player-chosen attribute focus (a priority order), allocating Development Points to those attributes instead of the AI allocation; with no focus, development is unchanged.
- `world-progression`: When a world has a designated player, the World applies that player's decisions for their golfer during advance — development focus, resting exclusion from fields, and deferring sponsorship acceptance to the player's pending queue — while using the existing AI paths for all other golfers; an unassigned world is byte-for-byte unchanged.

## Impact

- **Codebase**: New `com.progolf.sim.control` package (`PlayerControl`). `ProgressionEngine` gains a focus-aware `develop` overload. `World` gains an optional `PlayerControl` + player pending-offers, an `assignPlayer` op, the player-action methods, and player-aware branches at `evolveGolfer` / `resolveEvent` field selection / `runFinancialSeason`. `WorldService` (app) exposes the player operations. No other domain changes.
- **Determinism / compatibility**: every player branch is `if (this is the player's golfer) … else <existing AI path>`; with no player assigned the engine is identical, so the 290 engine tests and the world reproducibility test are unaffected. The player's choices become new inputs the world depends on (as a game should); when captured by persistence later, save/load stays observably identical.
- **DAG**: `sim.control` depends only on `core` (attributes); the World composes it and owns the economy coupling (pending offers). Nothing lower imports `control`; only `world` (and, above, the app) use it.
- **Downstream (future changes)**: persistence snapshots the now-settled world state including `PlayerControl`; GraphQL resolvers expose the player operations to the frontend; staff/equipment player control follows the same seam pattern.
- **Boundary/risk**: this is the first human input into a previously autonomous engine — contained to one designated golfer, applied only at existing seams, and fully backward-compatible when unassigned; guarded by the unchanged engine suite plus new tests that play a turn and assert the player's choices take effect while others stay AI-driven.
