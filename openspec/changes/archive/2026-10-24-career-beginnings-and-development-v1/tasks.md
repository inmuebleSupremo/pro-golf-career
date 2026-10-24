## 1. Starting Background creation

- [x] 1.1 Add the three creation-only background choices and deterministic initial-ability adjustments, preserving Playing Style potential and age effects; default omitted API input to Elite Amateur.
- [x] 1.2 Extend the existing creator and creation API with challenge descriptions and the selected background; cover all backgrounds, ages, Playing Styles, omitted input, and deterministic output.

## 2. Manual development pacing

- [x] 2.1 Set the provisional human base seasonal award to 240 without changing AI awards or existing stage, coach, and performance factors.
- [x] 2.2 Enforce the provisional +6 per-attribute calendar-season purchased-gain limit cumulatively across transactions, with atomic rejection, bank preservation, aging exclusion, and reset on calendar advance.
- [x] 2.3 Derive current-season purchased gains from recorded development history; verify old-save preservation and same-season save/reload equivalence before considering any additional persisted state.

## 3. Authoritative player-facing availability

- [x] 3.1 Provide one backend purchase-availability rule and expose per-attribute allowance, next cost, potential headroom, and blocking reason in the existing development read model.
- [x] 3.2 Explain availability in the existing training interface and prevent controls from proposing invalid raises; keep backend mutation validation authoritative.
- [x] 3.3 Use the same availability rule for the Development Inbox qualifier, coordinating with `add-career-inbox` without changing its other sources or acknowledgements; refresh Inbox after relevant purchases and season changes.

## 4. Focused verification

- [x] 4.1 Test cumulative transactions, atomic invalid requests, season reset, aging exclusion, saved balances, old-save load, and deterministic save/reload continuation.
- [x] 4.2 Check the three backgrounds in a small default-world gameplay sample; compare focused and balanced spending over multiple seasons, including standard-career progression and Club Champion viability. Tune only the provisional values if needed.
- [x] 4.3 Run backend tests (including architecture purity), frontend codegen/typecheck/lint and relevant UI checks; validate the OpenSpec change after any specification adjustments.
