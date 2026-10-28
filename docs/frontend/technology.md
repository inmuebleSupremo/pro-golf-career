# TECHNOLOGY.md

## Purpose

This document defines the approved frontend technology stack.

Claude must use these technologies unless explicitly instructed otherwise.

Do not introduce alternative frameworks or libraries without justification.

---

# Framework

* Next.js (App Router)
* React
* TypeScript
* pnpm

---

# Styling

* Tailwind CSS v4

Use utility-first styling.

Do not introduce CSS frameworks or component-specific CSS unless absolutely necessary.

---

# Components

Foundation:

* shadcn/ui

Accessibility:

* Radix UI

Always customise shadcn components to match the project's design language.

Never ship default shadcn styling.

---

# Motion

Use:

* Motion

Purpose:

* page transitions
* layout transitions
* hover interactions
* loading states
* micro-interactions

Motion should communicate hierarchy and continuity.

Never animate purely for decoration.

---

# Data

Server state:

* TanStack Query

Tables:

* TanStack Table

Forms:

* React Hook Form

Validation:

* Zod

Dates:

* date-fns

---

# Icons

Use:

* Lucide React

Do not mix icon libraries.

---

# Graphics

Prefer:

* SVG

Use D3 only for:

* mathematical layouts
* charts
* custom SVG generation

Do not use D3 for general UI.

Use Canvas only when SVG performance becomes a measurable bottleneck.

Normal play uses the canonical SVG renderer. For a live Generator 8 hole with Parkland family,
Parkland-resolved scene type, `GENERATOR_8_PRINCIPLED_ARCHITECTURE` provenance, and a playable boundary,
the stage selects Premium Parkland V2. Generator 8 Links family holes with the same provenance and a
playable boundary select Premium Links. Generator 8 Heathland family holes additionally require the
`HEATHLAND` canonical biome and a Heathland-resolved course type to select Premium Heathland. Earlier
saves and holes outside those eligibility rules retain Current. Historical `WOODLAND` identity is
preserved. All premium presentations use the same backend geometry, aim coordinates, and shot trace;
their wider property is decorative. The Parkland Current/V1/V2 selector and fixed-seed previews remain
development-only. See [Premium Links](premium-links.md) and [Premium Heathland](premium-heathland.md)
for their art, geometry, and evidence.

---

# Images

Use Next.js Image.

Always optimise images.

Never use oversized assets.

---

# Design Tokens

All visual decisions must come from design tokens.

Never hardcode:

* colours
* spacing
* typography
* shadows
* border radius

---

# Default Principle

Before adding any dependency ask:

1. Can React solve this?
2. Can Next.js solve this?
3. Can an approved library solve this?

Only introduce new dependencies if the answer to all three questions is **No**.
