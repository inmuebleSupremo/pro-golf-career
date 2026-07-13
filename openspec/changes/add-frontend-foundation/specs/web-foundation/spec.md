## ADDED Requirements

### Requirement: Frontend application scaffold

The system SHALL provide a frontend application built with Next.js (App Router), React, and TypeScript, managed with pnpm, located in a top-level `frontend/` directory, in accordance with `docs/frontend/TECHNOLOGY.md`.

#### Scenario: Development server starts

- **WHEN** a developer installs dependencies with pnpm and starts the Next.js development server
- **THEN** the application compiles without type errors and serves the root route in a browser

#### Scenario: Approved stack only

- **WHEN** a dependency is added to the frontend
- **THEN** it is one of the technologies approved in `docs/frontend/TECHNOLOGY.md`, or a build-time-only tool justified against the dependency gate (React → Next.js → approved library → new dependency)

### Requirement: Token-driven design system

The system SHALL define a concrete design-token layer covering every category in `docs/frontend/DESIGN_TOKENS.md` (colour roles, typography, spacing, sizing, border radius, borders, shadows, motion, opacity, z-index, breakpoints), and all component styling SHALL resolve to these tokens through Tailwind CSS v4.

#### Scenario: No hardcoded visual values

- **WHEN** a component applies colour, spacing, typography, radius, shadow, or motion
- **THEN** the value resolves to a design token and no raw/ad-hoc visual value is hardcoded in the component

#### Scenario: Semantic colour roles

- **WHEN** a component needs a colour
- **THEN** it references a semantic role token (e.g. background, surface, primary, text-primary) rather than a raw colour value

#### Scenario: Editorial direction

- **WHEN** the token values are authored
- **THEN** they express the editorial, premium, calm, and spacious direction of `docs/frontend/REFERENCES.md` and avoid the prohibited visual language in `docs/frontend/BOUNDARIES.md` (heavy gradients, glassmorphism, excessive shadows, neon colours, decorative animation)

### Requirement: Re-skinned component baseline

The system SHALL provide a component baseline built on shadcn/ui and Radix UI, customised to the token system, and SHALL NOT ship default shadcn styling.

#### Scenario: Component consumes tokens

- **WHEN** a shadcn-derived primitive (e.g. button, input, form field) is rendered
- **THEN** its appearance is driven by the project's design tokens, not shadcn's default theme

#### Scenario: Reuse before creation

- **WHEN** a screen needs a UI primitive that already exists in the baseline
- **THEN** the existing component is reused or extended rather than a new duplicate being created

### Requirement: Consistent page structure and accessibility defaults

The system SHALL provide a shared page/layout structure that every screen inherits, and SHALL meet the accessibility and performance defaults in `docs/frontend/BOUNDARIES.md`: Server Components by default, semantic HTML, full keyboard navigation, visible focus states, and respect for reduced-motion preferences.

#### Scenario: Server Components by default

- **WHEN** a new route or component is added
- **THEN** it is a Server Component unless it requires client-side interactivity, in which case the client boundary is minimised

#### Scenario: Keyboard and focus

- **WHEN** a user navigates the interface with a keyboard
- **THEN** every interactive element is reachable and shows a visible focus state

#### Scenario: Reduced motion

- **WHEN** a user has a reduced-motion preference set
- **THEN** non-essential motion is disabled or reduced accordingly
