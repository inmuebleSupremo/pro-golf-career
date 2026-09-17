# Player Experience Definition — the complete-game target

**Status:** target realised — engine + app-service layer + GraphQL API + auth + a full player-facing UI (last updated 2026-08-20). This document defines *what the finished game offers the player*, so every build slice can be measured against it and we never discover a hollow "make two decisions then watch a sim" experience. It **synthesises** the authoritative vision in `explore.md` (§1) into the player-facing decision + play surface; where the two ever disagree, `explore.md` wins. It is not itself an implementation.

> **Where we are (2026-08-20):** every decision surface below is **built in the framework-free engine, driven through `WorldService`, exposed over GraphQL, and now playable through a real Next.js UI** — create-your-golfer, shot-by-shot play (always skippable, with a 2D/2.5D hole layer), event-by-event scheduling, development, staff, equipment, sponsorship, self-chosen achievements, career records, and an end-of-season/off-season review. The **depth pass** put real substance behind the pillars: putting that holes out (realistic absolute scores ~par, not +90/round), shot-level stats, a scaled 640-golfer world, per-golfer **AI strategy variety**, and a two-phase **Hall-of-Fame election** — so Statistical realism (3), Emergent storytelling (4), and Meaningful risk (5) are mechanically real. The delivery pipeline that was "next" in the earlier draft is done: **persistence (filesystem save/load), the GraphQL API, JWT auth, and the React/Next UI all exist.** (Original intent named React/Vite; the UI was built with Next.js — see [`tech_stack.md`](tech_stack.md).) Remaining work is polish/breadth on the backlog ([`../info.txt`](../info.txt)), not a missing pillar.

---

## 1. The game in one line

You **create and guide one professional golfer across a multi-decade career** — making the strategic decisions (on the course and off it) while the simulation resolves the outcomes — and you **rise, struggle, and build a legacy** in a living professional-golf world.

> Player fantasy (`explore.md` §1.2): *"I am managing and living the career of a professional golfer over multiple decades."*

Two responsibilities, always separated (§1.1):
- **The player is responsible for decisions.**
- **The simulation is responsible for outcomes.**
- Success comes from the interaction of choices, attributes, conditions, and controlled variance — **never** from reflexes, timing, swing meters, or dexterity.

## 2. The five pillars (the filter for every mechanic)

Every player-facing feature must serve at least one (§1.3):
1. **Strategic decision making** — you win primarily because you decide better.
2. **Long-term career investment** — decades, not rounds; an age-18 choice felt at 40.
3. **Statistical realism** — credible pro-golf outcomes and patterns.
4. **Emergent storytelling** — stories arise from the sim, not scripts.
5. **Meaningful risk** — every meaningful reward carries meaningful risk.

## 3. The two intertwined modes of play

The game is **not** "management only" and **not** "an arcade golf game." It is a **decision game** with two decision surfaces:

### A. Strategic shot selection — *playing* the rounds (in scope, §1.7)
When your golfer plays a tournament round, you may play it **shot by shot**, making a **decision** each shot:
- **Club selection**, **target selection**, **risk selection** (safe vs aggressive line, lay up vs go).
- The sim resolves the outcome from your golfer's attributes, the lie, weather, fatigue, pressure, and equipment. **You choose the play; the dice are the engine's.**
- **Explicitly excluded (§1.7):** swing timing, shot-shaping controls, reflex mechanics. No swing meter, ever.
- **Always skippable:** sim a shot, "sim to the end of the round," or auto-play a whole round. Because the game is *about decades* (Pillar 2), you never have to grind every shot — but you *can* play the ones that matter (a major Sunday, in contention, a career-defining putt).

*Engine reality:* **done.** The shot engine exposes `resolveShot` (the human, decision-by-decision entry point) alongside `resolveRound` (AI), and `PlayableRound`/`PlayableEvent` now let the player play a real tournament round shot-by-shot (situation → club/target/risk) or sim any part of it, driven through `WorldService`. A fully-simmed player round is provably identical to the automatic resolution (fidelity by construction). Putting now holes out via a make-% model, so those played rounds post believable scores.

### B. Career management — the decisions *between* rounds
The strategic spine that makes an age-18 choice matter at 40. All of these are **player decisions**, now exposed through `WorldService` (for the player's golfer; the AI still auto-manages the rest of the field):
- **Development** — invest development points to specialise/round-out your golfer (exposed: *focus*).
- **Schedule & workload** — which tournaments to enter, when to rest, balancing prize/ranking/prestige against fatigue and injury risk (exposed: **event-by-event entry** across the calendar, plus a rest toggle).
- **Health** — manage fatigue and recovery; rehab decisions after injury (managed via scheduling/rest; the sim resolves fatigue + injuries).
- **Staff** — hire/replace coach, caddie, fitness coach, physiotherapist, sports psychologist (exposed: *pending offers → hire/release*; caddie & psychologist now affect shots).
- **Equipment** — choose the bag and buy/upgrade clubs and ball (exposed: *pending upgrades → buy*, *select loadout*; workability/feel now affect shots).
- **Sponsorship** — accept/decline offers and manage objective-bearing agreements (exposed: *accept/decline*).
- **Finances** — spend within your means; every purchase (staff, equipment) is a budget decision (economy exists; staff/equipment spends are budget-gated, and competing can run at a loss).

## 4. The complete decision & interaction surface

The end-to-end experience, with honest current status. **Built** = engine exists; **Exposed** = a human can do it today through `WorldService` (no UI yet); **Missing** = not yet a player action.

| Stage | Player does | Engine | Exposed to player |
|---|---|---|---|
| **Onboarding** | Create/choose your golfer (identity, nationality, archetype, starting build) | Built (population/identity/attributes) | **Exposed** (`createPlayer` — custom golfer at the Development tour) |
| **Season planning** | Choose events to enter; plan rest/training around the calendar | Built (calendar, schedule, tours, health) | **Exposed** (event-by-event skip/enter + rest) |
| **Preparation** | Set loadout; brief staff; set tactics for the event/conditions | Built (equipment loadout, staff, weather) | **Exposed** (loadout select, staff hire/release); per-event tactics still Missing |
| **Play the round** | **Strategic shot selection** (club/target/risk) shot-by-shot, or sim | Built (`resolveShot`, `PlayableRound`/`PlayableEvent`) | **Exposed** (play/sim via `WorldService`) |
| **Development** | Invest development points | Built (progression) | **Exposed** (focus) |
| **Staff** | Hire/replace support team | Built (staff) | **Exposed** (pending offers → hire/release) |
| **Equipment** | Buy/choose clubs & ball | Built (equipment) | **Exposed** (pending upgrades → buy, select loadout) |
| **Sponsorship** | Accept/decline & manage agreements | Built (economy) | **Exposed** (accept/decline) |
| **Finances** | Budget & spend | Built (economy) | **Exposed** (staff/equipment spends are budget-gated) |
| **Career arc** | Pursue self-chosen goals; navigate promotion/relegation, aging, decline, retirement, Hall-of-Fame | Built (tours, career, ranking, aging, HoF election) | **Exposed** (goals + live progress; HoF induction); attribute progression is automatic |
| **Experience the world** | Read the news, track your stats/records/ranking, feel rivalries & narrative | Built (media, statistics inc. shot-level, ranking) | Read models exposed; **no UI** |

**Definition of winning (§1.6):** no single win condition — you pursue your own mix of victories, majors, earnings, world ranking, longevity, records, Hall-of-Fame. The game must give *meaningful play in every career phase* (§1.5: Entry → Development → Growth → Prime → Veteran → Decline → Retirement); no phase mechanically abandoned.

## 5. "There is a game here" — the acceptance test

We can say the game is real when a player can, end to end:
1. **Create their golfer** and start a career.
2. **Play a tournament round via strategic shot selection** (club/target/risk), and choose to sim any round.
3. **Make the full set of career decisions** between events — development, schedule/rest, staff, equipment, sponsorship, finances — each with weight and trade-offs (Pillars 1 & 5).
4. **Feel long-term consequences** — an early choice still mattering seasons later (Pillar 2).
5. **Progress across phases** to prime, decline, and retirement, pursuing self-defined goals (§1.5/§1.6).
6. **Experience the living world** — news, rivals, stats, records, a ranking climb — so the career has a story (Pillars 3 & 4).

If a slice does not move at least one of these forward, it is infrastructure, not game.

**Status (2026-08-20): all six pass end-to-end through the real UI.** A human can create a golfer, play or sim tournament rounds via club/target/risk (with a 2D hole layer), make the full set of between-event decisions (development, scheduling, staff, equipment, sponsorship, finances), feel choices compound over a multi-decade career, progress through the phases pursuing self-chosen goals/achievements up to Hall-of-Fame induction, and read the living world's news/stats/records/ranking — all on screen, with saves persisted to disk. The player-facing UI (item 8) now exists.

## 6. Guardrails

- **Decisions, not dexterity** (§1.1/§1.7) — every interaction is a choice the sim resolves; no timing/meters.
- **Decades over rounds** (Pillar 2) — shot play is available but skippable; the strategic weight is the career.
- **Same engine for human & AI** (REQ-104) — the human occupies the same seams the AI does; no separate rules, no hidden advantage.
- **Pure engine, app wraps it** — gameplay logic stays in framework-free `sim.*`; the app/API/UI expose it (see `app-layer-roadmap`).
- **Configure-then-advance for management; play-when-it-happens (skippable) for rounds.**

## 7. Roadmap toward this target (re-sequenced)

Proving the *game* early took priority over more infrastructure. Items 1–5 are complete, plus a depth pass that made the mechanics real:
1. ✅ App shell · ✅ Player-control loop (development / sponsorship / rest — the first management decisions).
2. ✅ **Playable round** — strategic shot selection (club/target/risk) shot-by-shot for the player's golfer, with sim/skip (`PlayableRound`/`PlayableEvent`). *The proof the game is real.*
3. ✅ **Tournament structure & majors** *(engine)* — events differentiated by prestige (regular / signature / **major**), majors as cross-tour marquee events weighted into ranking points, prize, and legacy.
4. ✅ **Management breadth** — staff, equipment, and **event-by-event scheduling** across the differentiated calendar, all exposed via the same seam pattern; spends are budget-gated.
5. ✅ **Onboarding & goals** — **create-your-golfer** (custom identity + starting build); self-defined career goals with live progress, surfaced by the narrative layer.
6. ✅ **Depth pass** *(engine realism)* — closed the gaps that made decisions hollow or scores unreal: staff/equipment shot effects, economy stakes, a scaled 640-golfer world, shot-level stats, a **putting make-% model** (believable absolute scores), per-golfer **AI strategy variety**, and a two-phase **Hall-of-Fame election**. (Remaining Tier-3 calibration items are tracked separately.)
7. ✅ **Persistence** — snapshot + filesystem save/load against the settled player-state (`SimSnapshotModule`).
8. ✅ **GraphQL API → Auth → frontend → Docker** — delivered as an actual playable product (Spring GraphQL +
   JWT auth + a Next.js App-Router UI + `docker compose`). Ongoing work is polish/breadth on the backlog
   ([`../info.txt`](../info.txt)), not a missing pillar.

## 8. Confirmed decisions (locked 2026-07-10)

All five confirmed for V1:
- **Shot-play centrality** — ✅ Strategic shot selection is **available every round, always skippable, and never required to win.** The game is winnable purely on decisions; playing shots is immersion and marginal edge, not a gate.
- **Create-your-golfer** — ✅ The player **creates a custom golfer** (identity — name/nationality — plus a starting build/archetype) as the career they own, rather than adopting an existing generated one. (Onboarding slice; the engine's population/identity/attributes support it.)
- **Career goals** — ✅ **Lightweight, self-chosen ambitions** (e.g., reach the top tour, win a major, world #1) that frame progress and legacy but **never gate** play (§1.6); surfaced by the narrative/stats layer.
- **Scheduling depth** — ✅ The player **chooses which events to enter** from the calendar (not merely rest/play), weighing **entry requirements** and **fatigue/travel trade-offs** — a real season-planning decision (Pillars 1, 2, 5).
- **Tours & majors** — ✅ The game **distinguishes tours and the events within them as real-life golf does.** The tour *ladder* is already modelled (Elite / Primary / Secondary / Development ≈ the tour tiers). This adds **event prestige *within* a tour, including MAJORS** — the pinnacle, cross-tour events that carry the most **ranking points, prize money, prestige, and career legacy**, and are the marquee accomplishments of §1.6. **This is an engine addition** (an event prestige/type — regular vs signature vs major — weighted into ranking/prize/legacy, with majors drawing the strongest fields across tours), not just player-facing exposure. It lands as its own slice within the tournament/scheduling breadth (see §7).
