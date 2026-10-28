# Design

Use the canonical world-to-screen model and existing `Hole2d` interaction. Fit four nested, smoothly
capped Heathland bands to generated corridor samples; draw canonical regions above them. Adapt the
approved source's contour, material, vegetation, sand, pond and lighting construction to scene-space
coordinates. Batch decorative micro paths and reject decoration against polygons and routes before
creating SVG elements.

Compose partial neighbouring holes from generated Heathland geometry. Evaluate placement and access
together with bounded deterministic candidate searches. Use the existing property route building
utilities, retain only clear connected routes, and omit infeasible neighbours. Decorative property
never handles pointer input. The renderer's own inverse projection and shot trace remain unchanged.
