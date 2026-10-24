## Why

Every created golfer currently begins from the same ability baseline, while manual Development Point spending can lift one ordinary attribute to elite level in a single season. Career beginnings offer too little choice and development is compressed into too few decisions.

## What Changes

- Add a Starting Background choice to the existing golfer creator: Club Champion (challenging), Elite Amateur (the current standard), or Highly Rated Prospect (competitive sooner). Background sets initial ability; age and Playing Style continue to shape the build, and potential is unchanged.
- Pace human development with a lower seasonal Development Point award and a cumulative limit on purchased gains per attribute per calendar season. Manual allocation and banking remain intact. The initial V1 defaults are a 240-point base award and +6 purchased gains per attribute per season, subject to focused gameplay tuning.
- Explain the point balance, next cost, remaining potential, remaining seasonal allowance, and purchase blockers in the existing training interface. The Career Inbox shows Development only when a purchase is currently valid.
- Preserve existing careers' attributes, potential, banked points, and development history. Older creation callers default to Elite Amateur; no existing golfer is rebuilt from a background.

Out of scope: AI progression changes, economy rebalance, new tours or qualification, new training activities or screens, Playing Style or potential redesign, background-specific events, and unrelated cleanup of obsolete `developmentFocus` specifications.

## Capabilities

### Modified Capabilities

- `golfer-creation`: background and age combine with Playing Style without changing potential or existing careers.
- `player-development`: manual purchases share a seasonal per-attribute limit and preserve unspent points.
- `web-onboarding`: the creator presents the three backgrounds and their relative challenge.
- `web-career`: the existing training interface explains authoritative purchase availability.
- `graphql-api`: creation accepts an optional background and development reads expose purchase availability.
- `save-persistence`: older saves remain valid and current-season purchase history survives reload.

## Impact

- **Engine:** deterministic creation and human purchase rules only; simulation-controlled golfer progression remains unchanged.
- **Application/API:** one source of purchase eligibility serves mutation validation, training reads, and the existing Career Inbox projection.
- **Frontend:** one creator choice and explanation within the current training screen; no new management route.
- **Persistence:** use existing season-stamped development history where reliable, avoiding a new save field. Prior ratings and balances are never recalculated or discarded.
- **Validation:** focused default-world gameplay checks must establish that the standard start can progress, the challenging start remains viable, and focused and balanced spending both remain rewarding.
