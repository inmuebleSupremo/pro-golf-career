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
