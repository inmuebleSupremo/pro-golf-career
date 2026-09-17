# BOUNDARIES.md

## Purpose

These are non-negotiable implementation rules.

When making design or engineering decisions, Claude must respect these constraints.

---

# Components

* Reuse existing components before creating new ones.
* Extend existing components before replacing them.
* Do not duplicate functionality.
* Do not invent new component patterns.

---

# Styling

* Never ship default shadcn styling.
* Use design tokens for all visual decisions.
* Maintain consistent spacing and typography.
* Prefer whitespace over containers.
* Prefer typography over decoration.

---

# Layout

* Maintain a consistent page structure throughout the application.
* Do not create unique layouts for individual pages without justification.
* Prioritise readability over visual novelty.

---

# Motion

* Motion must communicate.
* Never animate for decoration.
* Respect reduced-motion preferences.
* Keep transitions subtle and fast.

---

# Performance

* Prefer Server Components.
* Minimise client-side JavaScript.
* Lazy-load where appropriate.
* Optimise before adding complexity.

---

# Accessibility

* Keyboard navigation is mandatory.
* Maintain visible focus states.
* Use semantic HTML.
* Never sacrifice accessibility for aesthetics.

---

# Visual Language

Do not use:

* Heavy gradients
* Glassmorphism
* Excessive shadows
* Neon colours
* Decorative animations
* Floating UI for its own sake
* Inconsistent border radii
* Inconsistent spacing
* Multiple visual styles within the same application

---

# Decision Making

When uncertain:

1. Reuse rather than create.
2. Simplify rather than decorate.
3. Clarify rather than impress.
4. Be consistent rather than novel.

The safest implementation is usually the correct implementation.

---

# Final Rule

The objective is not to build an impressive frontend.

The objective is to build a frontend that users stop noticing because it feels coherent, effortless and exceptionally well crafted.
