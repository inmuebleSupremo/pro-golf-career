# Player Experience Definition — the complete-game target

**Status:** draft target for review. This document defines *what the finished game offers the player*, so every build slice can be measured against it and we never discover a hollow "make two decisions then watch a sim" experience. It **synthesises** the authoritative vision in `explore.md` (§1) into the player-facing decision + play surface; where the two ever disagree, `explore.md` wins. It is not itself an implementation.

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

*Engine reality:* the shot engine already exposes `resolveShot` (the human, decision-by-decision entry point) alongside `resolveRound` (AI). This mode is engine-ready and **not yet exposed to the player** — it is the priority next build.

### B. Career management — the decisions *between* rounds
The strategic spine that makes an age-18 choice matter at 40. All of these are **player decisions** (today taken automatically by AI policies for every golfer):
- **Development** — invest development points to specialise/round-out your golfer (exposed: *focus*).
- **Schedule & workload** — which tournaments to enter, when to rest, balancing prize/ranking/prestige against fatigue and injury risk (exposed: a *rest* toggle; needs true event-by-event scheduling).
- **Health** — manage fatigue and recovery; rehab decisions after injury (partial via rest).
- **Staff** — hire/replace coach, caddie, fitness coach, physiotherapist, sports psychologist (AI-driven today).
- **Equipment** — choose the bag and buy/upgrade clubs and ball (AI-driven today).
- **Sponsorship** — accept/decline offers and manage objective-bearing agreements (exposed: *accept/decline*).
- **Finances** — spend within your means; every purchase (staff, equipment) is a budget decision (economy exists; spending is the staff/equipment decisions).

## 4. The complete decision & interaction surface

The end-to-end experience, with honest current status. **Built** = engine exists; **Exposed** = a human can do it today; **Missing** = not yet a player action.

| Stage | Player does | Engine | Exposed to player |
|---|---|---|---|
| **Onboarding** | Create/choose your golfer (identity, nationality, archetype, starting build) | Built (population/identity/attributes) | **Missing** — you currently *designate an existing* generated golfer |
| **Season planning** | Choose events to enter; plan rest/training around the calendar | Built (calendar, schedule, tours, health) | **Missing** (only a rest toggle) |
| **Preparation** | Set loadout; brief staff; set tactics for the event/conditions | Built (equipment loadout, staff, weather) | **Missing** |
| **Play the round** | **Strategic shot selection** (club/target/risk) shot-by-shot, or sim | Built (`resolveShot`) | **Missing** ← priority next |
| **Development** | Invest development points | Built (progression) | **Exposed** (focus) |
| **Staff** | Hire/replace support team | Built (staff) | **Missing** |
| **Equipment** | Buy/choose clubs & ball | Built (equipment) | **Missing** |
| **Sponsorship** | Accept/decline & manage agreements | Built (economy) | **Exposed** (accept/decline) |
| **Finances** | Budget & spend | Built (economy) | Partial (via the above spends) |
| **Career arc** | Pursue self-chosen goals; navigate promotion/relegation, aging, decline, retirement | Built (tours, career, ranking, aging) | **Missing** (goals); progression is automatic |
| **Experience the world** | Read the news, track your stats/records/ranking, feel rivalries & narrative | Built (media, statistics, ranking) | Read models exist; **no UI** |

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

## 6. Guardrails

- **Decisions, not dexterity** (§1.1/§1.7) — every interaction is a choice the sim resolves; no timing/meters.
- **Decades over rounds** (Pillar 2) — shot play is available but skippable; the strategic weight is the career.
- **Same engine for human & AI** (REQ-104) — the human occupies the same seams the AI does; no separate rules, no hidden advantage.
- **Pure engine, app wraps it** — gameplay logic stays in framework-free `sim.*`; the app/API/UI expose it (see `app-layer-roadmap`).
- **Configure-then-advance for management; play-when-it-happens (skippable) for rounds.**

## 7. Roadmap toward this target (re-sequenced)

Proving the *game* early takes priority over more infrastructure:
1. ✅ App shell · ✅ Player-control loop (development / sponsorship / rest — the first management decisions).
2. **Playable round** — strategic shot selection (club/target/risk) shot-by-shot for the player's golfer, with sim/skip. *The proof the game is real; next build.*
3. **Management breadth** — expose the remaining levers to the player (staff, equipment, true event-by-event scheduling, finances) via the same seam pattern.
4. **Onboarding & goals** — create-your-golfer; self-defined career goals/ambitions.
5. **Persistence** — snapshot + save/load (now against a settled, fuller player-state).
6. **GraphQL API → Auth → React/Vite frontend → Docker** — deliver it as an actual playable product.

## 8. Open product calls to confirm

These shape the target and are the player's/owner's to decide (proposals in italics):
- **Shot-play centrality** — *available every round, always skippable, never required to win; the game is winnable purely on decisions.* Confirm this balance.
- **Create-your-golfer** — *yes, create a custom golfer (identity + starting build/archetype) as the career you own,* rather than adopting an existing one. Confirm for V1 vs later.
- **Career goals** — *lightweight, self-chosen ambitions (e.g., reach the top tour, win a major, world #1) that frame progress but never gate it (§1.6),* surfaced by the narrative layer. Confirm scope.
- **Scheduling depth** — *choose events to enter from the calendar (not just rest/play), with entry requirements and fatigue/travel trade-offs.* Confirm depth for V1.
- **Majors / prestige tiers** — whether V1 distinguishes "major-equivalent" events (§1.6 references them) or treats all tour events uniformly for now.
